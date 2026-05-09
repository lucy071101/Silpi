package com.silpi.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

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
            Toast.makeText(this, "Chat room ID is missing.", Toast.LENGTH_SHORT).show()
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

        if (imageBytes.size > 400_000) {
            return ""
        }

        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = if (width > height) {
            maxSize.toFloat() / width.toFloat()
        } else {
            maxSize.toFloat() / height.toFloat()
        }

        val resizedWidth = (width * ratio).toInt()
        val resizedHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
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

        sendChatMessage(chatMessage, "Photo")
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
                    "lastMessageTime" to chatMessage.timestamp
            )

            for (userId in chatRoom.participants) {
                if (userId != myUserId) {
                    roomUpdates["unreadCount.$userId"] = FieldValue.increment(1)
                }
            }

            transaction.set(messageRef, chatMessage)
            transaction.update(roomRef, roomUpdates)
        }
                .addOnSuccessListener {
                    Log.d("ChatActivity", "Message sent")
                }
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Message send failed", e)
                    Toast.makeText(this, "Message send failed", Toast.LENGTH_SHORT).show()
                }
    }

    private fun markAsRead() {
        val roomRef = db.collection("chats").document(chatRoomId)

        roomRef.update("unreadCount.$myUserId", 0)
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Read update failed", e)
                }
    }

    private fun listenMessages() {
        messageListener = db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e("ChatActivity", "Message fetch failed", error)
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
