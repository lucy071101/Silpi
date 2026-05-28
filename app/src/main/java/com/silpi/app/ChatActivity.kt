package com.silpi.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.ByteArrayOutputStream

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var addButton: ImageButton
    private lateinit var phoneButton: ImageButton
    private lateinit var phoneButtonLayout: View
    private lateinit var moreButton: ImageButton
    private lateinit var menuPanel: FrameLayout
    private lateinit var imageProfile: ImageView
    private lateinit var textViewUserName: TextView
    private lateinit var imagePreviewOverlay: FrameLayout
    private lateinit var imagePreview: ImageView
    private lateinit var buttonCloseImagePreview: ImageButton

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private val profileImagesByUserId = mutableMapOf<String, String>()
    private var currentChatRoom: ChatRoom? = null

    private lateinit var db: FirebaseFirestore
    private var messageListener: ListenerRegistration? = null

    private lateinit var chatRoomId: String
    private lateinit var myUserId: String
    private lateinit var myUserName: String

    private val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            prepareAndSendImage(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        db = FirebaseFirestore.getInstance()
        myUserId = CurrentUserProvider.userId(this)
        myUserName = CurrentUserProvider.userName(this)
        chatRoomId = intent.getStringExtra("chatRoomId") ?: ""

        if (chatRoomId.isEmpty()) {
            Toast.makeText(this, "대화방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        markAsRead()
        loadRoomHeader()
        listenMessages()
    }

    private fun initViews() {
        chatRecyclerView = findViewById(R.id.recyclerViewChat)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        backButton = findViewById(R.id.buttonBack)
        addButton = findViewById(R.id.buttonAdd)
        phoneButton = findViewById(R.id.buttonPhone)
        phoneButtonLayout = findViewById(R.id.layoutPhoneButton)
        moreButton = findViewById(R.id.buttonMore)
        menuPanel = findViewById(R.id.layoutChatMenuPanel)
        imageProfile = findViewById(R.id.imageProfile)
        textViewUserName = findViewById(R.id.textViewUserName)
        imagePreviewOverlay = findViewById(R.id.layoutImagePreviewOverlay)
        imagePreview = findViewById(R.id.imagePreview)
        buttonCloseImagePreview = findViewById(R.id.buttonCloseImagePreview)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList, myUserId, profileImagesByUserId) { imageData ->
            showImagePreview(imageData)
        }
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupClickListeners() {
        addButton.setOnClickListener {
            openImagePicker()
        }

        backButton.setOnClickListener {
            if (menuPanel.visibility == View.VISIBLE) {
                closeMenuPanel()
            } else {
                finish()
            }
        }

        moreButton.setOnClickListener {
            showChatRoomMenu()
        }

        buttonCloseImagePreview.setOnClickListener {
            closeImagePreview()
        }

        imagePreviewOverlay.setOnClickListener {
            closeImagePreview()
        }

        imagePreview.setOnClickListener {
            // Keep image taps from closing the preview.
        }

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageEditText.setText("")
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun sendMessage(message: String) {
        val chatMessage = ChatMessage(
                message = message,
                senderId = myUserId,
                senderName = myUserName,
                messageType = "text",
                timestamp = System.currentTimeMillis()
        )

        sendChatMessage(chatMessage, message)
    }

    private fun prepareAndSendImage(imageUri: Uri) {
        try {
            val imageData = encodeImageForTest(imageUri)
            if (imageData.isBlank()) {
                Toast.makeText(this, "Image is too large. Please choose a smaller image.", Toast.LENGTH_SHORT).show()
                return
            }

            sendImageMessage(imageData)
        } catch (e: Exception) {
            Log.e("ChatActivity", "Image send failed", e)
            Toast.makeText(this, "Image send failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodeImageForTest(imageUri: Uri): String {
        val inputStream = contentResolver.openInputStream(imageUri) ?: return ""
        val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return ""
        val resizedBitmap = resizeBitmap(bitmap, 500)
        val outputStream = ByteArrayOutputStream()
        var quality = 70
        var imageBytes: ByteArray

        do {
            outputStream.reset()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            imageBytes = outputStream.toByteArray()
            quality -= 10
        } while (imageBytes.size > 400_000 && quality >= 30)

        if (imageBytes.size > 400_000) return ""
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = if (width > height) {
            maxSize.toFloat() / width.toFloat()
        } else {
            maxSize.toFloat() / height.toFloat()
        }

        return Bitmap.createScaledBitmap(
                bitmap,
                (width * ratio).toInt(),
                (height * ratio).toInt(),
                true
        )
    }

    private fun sendImageMessage(imageData: String) {
        val chatMessage = ChatMessage(
                message = "",
                senderId = myUserId,
                senderName = myUserName,
                imageData = imageData,
                messageType = "image",
                timestamp = System.currentTimeMillis()
        )

        sendChatMessage(chatMessage, "사진")
    }

    private fun sendChatMessage(chatMessage: ChatMessage, lastMessage: String) {
        val roomRef = db.collection("chats").document(chatRoomId)
        val messageRef = roomRef.collection("messages").document()

        db.runTransaction { transaction ->
            val document = transaction.get(roomRef)
            val chatRoom = document.toObject(ChatRoom::class.java)
                    ?: throw IllegalStateException("Failed to load chat room.")

            val roomUpdates = mutableMapOf<String, Any>(
                    "lastMessage" to lastMessage,
                    "lastMessageTime" to chatMessage.timestamp,
                    "lastMessageSentAt" to FieldValue.serverTimestamp()
            )

            for (userId in chatRoom.participants) {
                if (userId != myUserId) {
                    roomUpdates["unreadCount.$userId"] = FieldValue.increment(1)
                }
            }

            transaction.set(messageRef, createMessageData(chatMessage))
            transaction.update(roomRef, roomUpdates)
        }
                .addOnSuccessListener {
                    Log.d("ChatActivity", "Message sent")
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Message send failed", e)
                    Toast.makeText(this, "문자 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
    }

    private fun markAsRead() {
        db.collection("chats")
                .document(chatRoomId)
                .update("unreadCount.$myUserId", 0)
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Read update failed", e)
                }
    }

    private fun createMessageData(chatMessage: ChatMessage): Map<String, Any> {
        return mapOf(
                "message" to chatMessage.message,
                "senderId" to chatMessage.senderId,
                "senderName" to chatMessage.senderName,
                "imageUrl" to chatMessage.imageUrl,
                "imageData" to chatMessage.imageData,
                "messageType" to chatMessage.messageType,
                "timestamp" to chatMessage.timestamp,
                "sentAt" to FieldValue.serverTimestamp()
        )
    }

    private fun loadRoomHeader() {
        db.collection("chats")
                .document(chatRoomId)
                .get()
                .addOnSuccessListener { document ->
                    val chatRoom = document.toObject(ChatRoom::class.java) ?: return@addOnSuccessListener
                    currentChatRoom = chatRoom.copy(roomId = document.id)

                    if (chatRoom.group) {
                        phoneButtonLayout.visibility = View.GONE
                        bindGroupRoomHeader(chatRoom)
                        return@addOnSuccessListener
                    }

                    phoneButtonLayout.visibility = View.VISIBLE
                    val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
                    textViewUserName.text = chatRoom.participantNames[otherUserId] ?: "채팅"
                    imageProfile.visibility = View.GONE
                }
    }

    private fun bindGroupRoomHeader(chatRoom: ChatRoom) {
        val roomName = chatRoom.roomName.ifBlank { "그룹 대화방" }
        val memberCountText = chatRoom.participants.size.toString()
        val title = "$roomName $memberCountText"
        val memberCountStart = title.length - memberCountText.length
        val spannableTitle = SpannableString(title)

        spannableTitle.setSpan(
                ForegroundColorSpan(Color.parseColor("#888888")),
                memberCountStart,
                title.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        imageProfile.visibility = View.GONE
        textViewUserName.text = spannableTitle
    }

    private fun showChatRoomMenu() {
        val chatRoom = currentChatRoom
        if (chatRoom == null) {
            Toast.makeText(this, "대화방 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        loadParticipants(chatRoom) { participants ->
            bindMenuHeader(menuPanel, chatRoom, participants)
            bindMenuParticipants(menuPanel, participants)
            bindMenuActions(menuPanel, chatRoom)
            openMenuPanel()
        }
    }

    private fun openMenuPanel() {
        menuPanel.visibility = View.VISIBLE
        menuPanel.post {
            menuPanel.translationX = menuPanel.width.toFloat()
            menuPanel.animate()
                    .translationX(0f)
                    .setDuration(220L)
                    .start()
        }
    }

    private fun closeMenuPanel() {
        menuPanel.animate()
                .translationX(menuPanel.width.toFloat())
                .setDuration(200L)
                .withEndAction {
                    menuPanel.visibility = View.GONE
                    menuPanel.translationX = 0f
                }
                .start()
    }

    private fun bindMenuHeader(view: View, chatRoom: ChatRoom, participants: List<ChatParticipant>) {
        val layoutProfileCluster = view.findViewById<View>(R.id.layoutMenuProfileCluster)
        val imageSingleProfile = view.findViewById<ShapeableImageView>(R.id.imageMenuSingleProfile)
        val textViewRoomName = view.findViewById<TextView>(R.id.textViewMenuRoomName)

        if (chatRoom.group) {
            layoutProfileCluster.visibility = View.VISIBLE
            imageSingleProfile.visibility = View.GONE
            bindGroupProfileCluster(view, participants.take(4))
            textViewRoomName.text = chatRoom.roomName.ifBlank { "그룹 대화방" }
        } else {
            val otherParticipant = participants.firstOrNull { !it.isMe } ?: participants.firstOrNull()
            layoutProfileCluster.visibility = View.GONE
            imageSingleProfile.visibility = View.VISIBLE
            ProfileImageHelper.setProfileImage(imageSingleProfile, otherParticipant?.profileImageData.orEmpty())
            textViewRoomName.text = otherParticipant?.userName ?: "대화방"
        }
    }

    private fun bindGroupProfileCluster(view: View, participants: List<ChatParticipant>) {
        val imageViews = listOf<ShapeableImageView>(
                view.findViewById(R.id.imageMenuProfile1),
                view.findViewById(R.id.imageMenuProfile2),
                view.findViewById(R.id.imageMenuProfile3),
                view.findViewById(R.id.imageMenuProfile4)
        )

        for (index in imageViews.indices) {
            val imageView = imageViews[index]
            val participant = participants.getOrNull(index)
            if (participant == null) {
                imageView.visibility = View.INVISIBLE
            } else {
                imageView.visibility = View.VISIBLE
                ProfileImageHelper.setProfileImage(imageView, participant.profileImageData)
            }
        }
    }

    private fun bindMenuParticipants(view: View, participants: List<ChatParticipant>) {
        val participantTitle = view.findViewById<TextView>(R.id.textViewMenuParticipantTitle)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMenuParticipants)
        val showAllText = view.findViewById<TextView>(R.id.textViewShowAllParticipants)

        val visibleParticipants = participants.take(30)
        participantTitle.text = "대화상대 ${participants.size}"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isVerticalScrollBarEnabled = true
        recyclerView.isScrollbarFadingEnabled = false
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.layoutParams = recyclerView.layoutParams.apply {
            height = if (visibleParticipants.size > 6) {
                (360 * resources.displayMetrics.density).toInt()
            } else {
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        recyclerView.adapter = ChatParticipantAdapter(visibleParticipants)

        showAllText.visibility = if (participants.size > 30) View.VISIBLE else View.GONE
        showAllText.text = "전체보기 (${participants.size})"
    }

    private fun bindMenuActions(view: View, chatRoom: ChatRoom) {
        view.findViewById<ImageButton>(R.id.buttonCloseMenu).setOnClickListener {
            closeMenuPanel()
        }

        view.findViewById<TextView>(R.id.textViewExitChatRoom).setOnClickListener {
            confirmExitChatRoom(chatRoom)
        }

        val createdInfo = view.findViewById<TextView>(R.id.textViewMenuCreatedInfo)
        val creatorName = chatRoom.participantNames[chatRoom.createdBy].orEmpty()
        createdInfo.text = if (creatorName.isNotBlank()) {
            "${creatorName}님이 만든 대화방이에요."
        } else {
            "대화방 정보"
        }
    }

    private fun confirmExitChatRoom(chatRoom: ChatRoom) {
        AlertDialog.Builder(this)
                .setTitle("대화방 나가기")
                .setMessage("이 대화방에서 나가시겠습니까?")
                .setNegativeButton("취소", null)
                .setPositiveButton("나가기") { _, _ ->
                    exitChatRoom(chatRoom)
                }
                .show()
    }

    private fun exitChatRoom(chatRoom: ChatRoom) {
        val roomRef = db.collection("chats").document(chatRoomId)
        val exitMessageRef = roomRef.collection("messages").document()
        val exitMessage = "${myUserName}님이 나갔습니다."
        val exitMessageTime = System.currentTimeMillis()

        db.runTransaction { transaction ->
            val document = transaction.get(roomRef)
            val currentRoom = document.toObject(ChatRoom::class.java)
                    ?: throw IllegalStateException("Failed to load chat room.")

            val updatedParticipants = currentRoom.participants.filter { it != myUserId }
            val updatedParticipantNames = currentRoom.participantNames.toMutableMap()
            val updatedUnreadCount = currentRoom.unreadCount.toMutableMap()
            val updatedSearchNames = currentRoom.searchNames.toMutableMap()

            updatedParticipantNames.remove(myUserId)
            updatedUnreadCount.remove(myUserId)
            updatedSearchNames.remove(myUserId)

            if (updatedParticipants.isEmpty()) {
                transaction.delete(roomRef)
            } else {
                updatedParticipants.forEach { userId ->
                    updatedUnreadCount[userId] = (updatedUnreadCount[userId] ?: 0) + 1
                }

                transaction.set(
                        exitMessageRef,
                        mapOf(
                                "message" to exitMessage,
                                "senderId" to "",
                                "senderName" to "",
                                "imageUrl" to "",
                                "imageData" to "",
                                "messageType" to "system",
                                "timestamp" to exitMessageTime,
                                "sentAt" to FieldValue.serverTimestamp()
                        )
                )
                transaction.update(
                        roomRef,
                        mapOf(
                                "participants" to updatedParticipants,
                                "participantNames" to updatedParticipantNames,
                                "unreadCount" to updatedUnreadCount,
                                "searchNames" to updatedSearchNames,
                                "lastMessage" to exitMessage,
                                "lastMessageTime" to exitMessageTime,
                                "lastMessageSentAt" to FieldValue.serverTimestamp()
                        )
                )
            }
        }
                .addOnSuccessListener {
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Chat room exit failed", e)
                    Toast.makeText(this, "대화방 나가기에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
    }

    private fun showImagePreview(imageData: String) {
        val bitmap = decodeImageData(imageData)
        if (bitmap == null) {
            Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        imagePreview.setImageBitmap(bitmap)
        imagePreviewOverlay.visibility = View.VISIBLE
    }

    private fun decodeImageData(imageData: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(imageData, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun closeImagePreview() {
        imagePreview.setImageDrawable(null)
        imagePreviewOverlay.visibility = View.GONE
    }

    private fun loadParticipants(chatRoom: ChatRoom, onLoaded: (List<ChatParticipant>) -> Unit) {
        val participantIds = chatRoom.participants
        if (participantIds.isEmpty()) {
            onLoaded(emptyList())
            return
        }

        val loadedParticipants = mutableMapOf<String, ChatParticipant>()
        var remainingCount = participantIds.size

        for (userId in participantIds) {
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)
                        val participantName = user?.userName
                                ?: chatRoom.participantNames[userId]
                                ?: if (userId == myUserId) myUserName else "사용자"

                        loadedParticipants[userId] = ChatParticipant(
                                userId = userId,
                                userName = participantName,
                                profileImageData = user?.profileImageData.orEmpty(),
                                isMe = userId == myUserId
                        )
                    }
                    .addOnFailureListener {
                        loadedParticipants[userId] = ChatParticipant(
                                userId = userId,
                                userName = chatRoom.participantNames[userId]
                                        ?: if (userId == myUserId) myUserName else "사용자",
                                profileImageData = "",
                                isMe = userId == myUserId
                        )
                    }
                    .addOnCompleteListener {
                        remainingCount -= 1
                        if (remainingCount == 0) {
                            onLoaded(participantIds.mapNotNull { loadedParticipants[it] })
                        }
                    }
        }
    }

    private fun listenMessages() {
        messageListener = db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ChatActivity", "Message fetch failed", error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    messageList.clear()

                    val loadedMessages = snapshot.documents.mapNotNull { document ->
                        val chatMessage = document.toObject(ChatMessage::class.java)
                        if (chatMessage == null) {
                            null
                        } else {
                            val displayTimestamp =
                                    ChatTimeHelper.readMillis(document, "sentAt")
                                            ?: chatMessage.timestamp
                            chatMessage.copy(timestamp = displayTimestamp)
                        }
                    }

                    messageList.addAll(buildMessagesWithDateDividers(loadedMessages.sortedBy { it.timestamp }))

                    chatAdapter.notifyDataSetChanged()
                    loadMessageSenderProfiles()

                    if (messageList.isNotEmpty()) {
                        chatRecyclerView.scrollToPosition(messageList.size - 1)
                    }

                    markAsRead()
                }
    }

    private fun buildMessagesWithDateDividers(messages: List<ChatMessage>): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        var lastDateKey = ""

        for (message in messages) {
            val dateKey = ChatTimeHelper.dateKey(message.timestamp)
            if (dateKey.isNotBlank() && dateKey != lastDateKey) {
                result.add(
                        ChatMessage(
                                message = ChatTimeHelper.formatDateDivider(message.timestamp),
                                messageType = "date",
                                timestamp = message.timestamp
                        )
                )
                lastDateKey = dateKey
            }
            result.add(message)
        }

        return result
    }

    private fun loadMessageSenderProfiles() {
        profileImagesByUserId[myUserId] = CurrentUserProvider.profileImageData(this)

        val senderIds = messageList
                .map { it.senderId }
                .filter { it.isNotBlank() }
                .distinct()

        for (senderId in senderIds) {
            if (profileImagesByUserId.containsKey(senderId)) continue

            loadUserProfileImage(senderId) { profileImageData ->
                profileImagesByUserId[senderId] = profileImageData
                chatAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadUserProfileImage(userId: String, onLoaded: (String) -> Unit) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    onLoaded(user?.profileImageData.orEmpty())
                }
    }

    override fun onBackPressed() {
        if (menuPanel.visibility == View.VISIBLE) {
            closeMenuPanel()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        messageListener?.remove()
        messageListener = null
        super.onDestroy()
    }
}
