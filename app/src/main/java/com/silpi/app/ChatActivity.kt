package com.silpi.app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var addButton: ImageButton

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var messageListener: ListenerRegistration? = null

    private lateinit var chatRoomId: String
    private lateinit var myUserId: String
    private lateinit var myUserName: String

    private val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            uploadAndSendImage(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        myUserId = CurrentUserProvider.userId(this)
        myUserName = CurrentUserProvider.userName(this)

        chatRoomId = intent.getStringExtra("chatRoomId") ?: ""

        if (chatRoomId.isEmpty()) {
            Toast.makeText(this, "채팅방 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        markAsRead()
        listenMessages()
    }

    private fun initViews() {
        chatRecyclerView = findViewById(R.id.recyclerViewChat)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        backButton = findViewById(R.id.buttonBack)
        addButton = findViewById(R.id.buttonAdd)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messageList, myUserId)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter
    }

    private fun setupClickListeners() {
        addButton.setOnClickListener {
            openImagePicker()
        }

        backButton.setOnClickListener {
            finish()
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

    private fun uploadAndSendImage(imageUri: Uri) {
        val imageRef = storage.reference
                .child("chat_images")
                .child(chatRoomId)
                .child("${UUID.randomUUID()}.jpg")

        Toast.makeText(this, "이미지 업로드 중...", Toast.LENGTH_SHORT).show()

        imageRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    sendImageMessage(downloadUri.toString())
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "이미지 업로드 실패", e)
                    Toast.makeText(this, "이미지 전송 실패", Toast.LENGTH_SHORT).show()
                }
    }

    private fun sendImageMessage(imageUrl: String) {
        val chatMessage = ChatMessage(
                message = "",
                senderId = myUserId,
                senderName = myUserName,
                imageUrl = imageUrl,
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
                    ?: throw IllegalStateException("채팅방 정보 불러오기 실패")

            val updatedUnreadCount = chatRoom.unreadCount.toMutableMap()

            for (userId in chatRoom.participants) {
                if (userId != myUserId) {
                    val currentCount = updatedUnreadCount[userId] ?: 0
                    updatedUnreadCount[userId] = currentCount + 1
                }
            }

            transaction.set(messageRef, chatMessage)
            transaction.update(
                    roomRef,
                    mapOf(
                            "lastMessage" to lastMessage,
                            "lastMessageTime" to chatMessage.timestamp,
                            "unreadCount" to updatedUnreadCount
                    )
            )
        }
                .addOnSuccessListener {
                    Log.d("ChatActivity", "메시지 전송 성공")
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "메시지 전송 실패", e)
                    Toast.makeText(this, "메시지 전송 실패", Toast.LENGTH_SHORT).show()
                }
    }

    private fun markAsRead() {
        val roomRef = db.collection("chats").document(chatRoomId)

        roomRef.get()
                .addOnSuccessListener { document ->
                    val chatRoom = document.toObject(ChatRoom::class.java)

                    if (chatRoom == null) return@addOnSuccessListener

                    val updatedUnreadCount = chatRoom.unreadCount.toMutableMap()
                    updatedUnreadCount[myUserId] = 0

                    roomRef.update("unreadCount", updatedUnreadCount)
                            .addOnFailureListener { e ->
                                Log.e("ChatActivity", "읽음 처리 실패", e)
                            }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "채팅방 정보 조회 실패", e)
                }
    }

    private fun listenMessages() {
        messageListener = db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e("ChatActivity", "메시지 불러오기 실패", error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    messageList.clear()

                    for (document in snapshot.documents) {
                        val chatMessage = document.toObject(ChatMessage::class.java)
                        if (chatMessage != null) {
                            messageList.add(chatMessage)
                        }
                    }

                    chatAdapter.notifyDataSetChanged()

                    if (messageList.isNotEmpty()) {
                        chatRecyclerView.scrollToPosition(messageList.size - 1)
                    }

                    markAsRead()
                }
    }

    override fun onDestroy() {
        messageListener?.remove()
        messageListener = null
        super.onDestroy()
    }
}
