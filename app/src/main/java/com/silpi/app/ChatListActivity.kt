package com.silpi.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatListActivity : AppCompatActivity() {

    private lateinit var recyclerViewChatRoom: RecyclerView
    private lateinit var buttonAddRoom: ImageButton
    private lateinit var buttonSearch: ImageButton
    private lateinit var buttonCloseSearch: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var textViewTitle: TextView
    private lateinit var textViewSearchLabel: TextView
    private lateinit var textViewAddLabel: TextView
    private lateinit var editTextChatSearch: EditText
    private lateinit var chatRoomAdapter: ChatRoomAdapter

    private val allChatRoomList = mutableListOf<ChatRoom>()
    private val chatRoomList = mutableListOf<ChatRoom>()
    private val profileImagesByUserId = mutableMapOf<String, String>()

    private lateinit var db: FirebaseFirestore
    private var chatRoomsListener: ListenerRegistration? = null

    private lateinit var myUserId: String
    private lateinit var myUserName: String
    private var isSearchMode = false
    private var searchKeyword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        db = FirebaseFirestore.getInstance()
        myUserId = CurrentUserProvider.userId(this)
        myUserName = CurrentUserProvider.userName(this)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        listenChatRooms()
    }

    private fun initViews() {
        recyclerViewChatRoom = findViewById(R.id.recyclerViewChatRoom)
        buttonAddRoom = findViewById(R.id.buttonAddRoom)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonCloseSearch = findViewById(R.id.buttonCloseSearch)
        buttonSettings = findViewById(R.id.buttonSettings)
        textViewTitle = findViewById(R.id.textViewTitle)
        textViewSearchLabel = findViewById(R.id.textViewSearchLabel)
        textViewAddLabel = findViewById(R.id.textViewAddLabel)
        editTextChatSearch = findViewById(R.id.editTextChatSearch)
    }

    private fun setupRecyclerView() {
        chatRoomAdapter = ChatRoomAdapter(
                chatRoomList = chatRoomList,
                myUserId = myUserId,
                profileImagesByUserId = profileImagesByUserId,
                onRoomLongClick = { chatRoom ->
                    showExitDialog(chatRoom)
                }
        )

        recyclerViewChatRoom.layoutManager = LinearLayoutManager(this)
        recyclerViewChatRoom.adapter = chatRoomAdapter
    }

    private fun setupClickListeners() {
        buttonAddRoom.setOnClickListener {
            startActivity(Intent(this, CreateChatRoomActivity::class.java))
        }

        buttonSearch.setOnClickListener {
            openSearchMode()
        }

        buttonCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        editTextChatSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newKeyword = normalizeSearchText(s.toString())
                if (newKeyword != searchKeyword) {
                    searchKeyword = newKeyword
                    applyChatRoomFilter()
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun listenChatRooms() {
        chatRoomsListener?.remove()

        chatRoomsListener = db.collection("chats")
                .whereArrayContains("participants", myUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ChatListActivity", "Chat room list load failed", error)
                        Toast.makeText(this, "대화 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    allChatRoomList.clear()

                    for (document in snapshot.documents) {
                        val chatRoom = document.toObject(ChatRoom::class.java)
                        if (chatRoom != null) {
                            val displayLastMessageTime =
                                    ChatTimeHelper.readMillis(document, "lastMessageSentAt")
                                            ?: chatRoom.lastMessageTime
                            val displayRoom = chatRoom.copy(
                                    roomId = document.id,
                                    lastMessageTime = displayLastMessageTime
                            )

                            allChatRoomList.add(displayRoom)
                            ensureSearchNames(document.id, displayRoom)
                        }
                    }

                    applyChatRoomFilter()
                    loadParticipantProfiles()
                }
    }

    private fun applyChatRoomFilter() {
        chatRoomList.clear()

        val filteredRooms = if (searchKeyword.isBlank()) {
            allChatRoomList
        } else {
            allChatRoomList.filter { chatRoom ->
                getSearchName(chatRoom).startsWith(searchKeyword)
            }
        }

        chatRoomList.addAll(filteredRooms.sortedByDescending { it.lastMessageTime })
        chatRoomAdapter.notifyDataSetChanged()
    }

    private fun getSearchName(chatRoom: ChatRoom): String {
        val savedSearchName = chatRoom.searchNames[myUserId].orEmpty()
        if (savedSearchName.isNotBlank()) return savedSearchName

        return if (chatRoom.group) {
            normalizeSearchText(chatRoom.roomName)
        } else {
            val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
            normalizeSearchText(chatRoom.participantNames[otherUserId].orEmpty())
        }
    }

    private fun openSearchMode() {
        if (isSearchMode) return

        isSearchMode = true
        textViewTitle.visibility = View.GONE
        editTextChatSearch.visibility = View.VISIBLE
        buttonSettings.visibility = View.GONE
        buttonAddRoom.visibility = View.GONE
        buttonSearch.visibility = View.GONE
        textViewSearchLabel.visibility = View.GONE
        textViewAddLabel.visibility = View.GONE
        buttonCloseSearch.visibility = View.VISIBLE
        editTextChatSearch.requestFocus()
        showKeyboard(editTextChatSearch)
    }

    private fun closeSearchMode() {
        isSearchMode = false
        searchKeyword = ""
        editTextChatSearch.setText("")
        editTextChatSearch.visibility = View.GONE
        textViewTitle.visibility = View.VISIBLE
        buttonSettings.visibility = View.VISIBLE
        buttonAddRoom.visibility = View.VISIBLE
        buttonSearch.visibility = View.VISIBLE
        textViewSearchLabel.visibility = View.VISIBLE
        textViewAddLabel.visibility = View.VISIBLE
        buttonCloseSearch.visibility = View.GONE
        hideKeyboard(editTextChatSearch)
        applyChatRoomFilter()
    }

    private fun showKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        loadParticipantProfiles()
    }

    override fun onDestroy() {
        chatRoomsListener?.remove()
        chatRoomsListener = null
        super.onDestroy()
    }

    private fun showExitDialog(chatRoom: ChatRoom) {
        val roomName = if (chatRoom.group) {
            chatRoom.roomName
        } else {
            val otherUserId = chatRoom.participants.firstOrNull { it != myUserId }
            chatRoom.participantNames[otherUserId] ?: chatRoom.roomName
        }

        AlertDialog.Builder(this)
                .setTitle("대화방 나가기")
                .setMessage("'$roomName' 대화방에서 나가시겠습니까?")
                .setPositiveButton("나가기") { _, _ ->
                    exitChatRoom(chatRoom)
                }
                .setNegativeButton("취소", null)
                .show()
    }

    private fun exitChatRoom(chatRoom: ChatRoom) {
        val roomRef = db.collection("chats").document(chatRoom.roomId)
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
                    Toast.makeText(this, "대화방에서 나갔습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ChatListActivity", "Chat room exit failed", e)
                    Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show()
                }
    }

    private fun ensureSearchNames(roomId: String, chatRoom: ChatRoom) {
        if (chatRoom.searchNames[myUserId].orEmpty().isNotBlank()) return

        val searchNames = buildSearchNames(chatRoom)
        if (searchNames.isEmpty()) return

        db.collection("chats")
                .document(roomId)
                .update("searchNames", searchNames)
                .addOnFailureListener { e ->
                    Log.e("ChatListActivity", "Search name update failed", e)
                }
    }

    private fun buildSearchNames(chatRoom: ChatRoom): Map<String, String> {
        if (chatRoom.group) {
            val roomName = chatRoom.roomName
            if (roomName.isBlank()) return emptyMap()

            return chatRoom.participants.associateWith {
                normalizeSearchText(roomName)
            }
        }

        if (chatRoom.participants.size < 2) return emptyMap()

        return chatRoom.participants
                .associateWith { userId ->
                    val otherUserId = chatRoom.participants.firstOrNull { it != userId }
                    normalizeSearchText(chatRoom.participantNames[otherUserId].orEmpty())
                }
                .filterValues { it.isNotBlank() }
    }

    private fun normalizeSearchText(text: String): String {
        return text.trim().lowercase()
    }

    private fun loadParticipantProfiles() {
        val participantIds = chatRoomList
                .flatMap { it.participants }
                .distinct()

        if (participantIds.isEmpty()) return

        for (userId in participantIds) {
            if (profileImagesByUserId.containsKey(userId)) continue

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            profileImagesByUserId[userId] = user.profileImageData
                            chatRoomAdapter.notifyDataSetChanged()
                        }
                    }
        }
    }
}
