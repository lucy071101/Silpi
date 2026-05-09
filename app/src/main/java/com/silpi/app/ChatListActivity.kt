package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val chatRoomList = mutableListOf<ChatRoom>()

    private lateinit var db: FirebaseFirestore
    private var chatRoomsListener: ListenerRegistration? = null

    private lateinit var myUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        db = FirebaseFirestore.getInstance()
        myUserId = CurrentUserProvider.userId(this)

        registerTestUser()

        initViews()
        setupRecyclerView()
        setupClickListeners()
        listenChatRooms()
    }

    private fun initViews() {
        recyclerViewChatRoom = findViewById(R.id.recyclerViewChatRoom)
        buttonAddRoom = findViewById(R.id.buttonAddRoom)
    }

    private fun setupRecyclerView() {
        chatRoomAdapter = ChatRoomAdapter(
                chatRoomList = chatRoomList,
                myUserId = myUserId,
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
    }

    private fun listenChatRooms() {
        chatRoomsListener = db.collection("chats")
                .whereArrayContains("participants", myUserId)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e("ChatListActivity", "채팅방 목록 불러오기 실패", error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    chatRoomList.clear()

                    for (document in snapshot.documents) {
                        val chatRoom = document.toObject(ChatRoom::class.java)
                        if (chatRoom != null) {
                            chatRoomList.add(chatRoom.copy(roomId = document.id))
                        }
                    }

                    chatRoomList.sortByDescending { it.lastMessageTime }
                    chatRoomAdapter.notifyDataSetChanged()
                }
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
                .setTitle("채팅방 나가기")
                .setMessage("'$roomName' 채팅방에서 나가시겠습니까?")
                .setPositiveButton("나가기") { _, _ ->
                    exitChatRoom(chatRoom)
                }
                .setNegativeButton("취소", null)
                .show()
    }

    private fun exitChatRoom(chatRoom: ChatRoom) {
        val roomRef = db.collection("chats").document(chatRoom.roomId)

        roomRef.get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        Toast.makeText(this, "채팅방이 이미 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val currentRoom = document.toObject(ChatRoom::class.java)
                    if (currentRoom == null) {
                        Toast.makeText(this, "채팅방 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val updatedParticipants = currentRoom.participants.filter { it != myUserId }
                    val updatedParticipantNames = currentRoom.participantNames.toMutableMap()
                    val updatedUnreadCount = currentRoom.unreadCount.toMutableMap()

                    updatedParticipantNames.remove(myUserId)
                    updatedUnreadCount.remove(myUserId)

                    if (updatedParticipants.isEmpty()) {
                        roomRef.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "마지막 참여자가 나가 채팅방이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChatListActivity", "빈 채팅방 삭제 실패", e)
                                    Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show()
                                }
                    } else {
                        val updates = mapOf(
                                "participants" to updatedParticipants,
                                "participantNames" to updatedParticipantNames,
                                "unreadCount" to updatedUnreadCount
                        )

                        roomRef.update(updates)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "채팅방에서 나갔습니다.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChatListActivity", "채팅방 나가기 실패", e)
                                    Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show()
                                }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatListActivity", "채팅방 정보 조회 실패", e)
                    Toast.makeText(this, "나가기 실패", Toast.LENGTH_SHORT).show()
                }
    }
    //임시로 테스트용 앱실행시 user ID 입력
    private fun registerTestUser() {
        val user = CurrentUserProvider.user(this)

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.userId)
                .set(user)
    }
}
