package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CreateChatRoomActivity : AppCompatActivity() {

    private lateinit var etRoomName: EditText
    private lateinit var btnCreateRoom: Button
    private lateinit var buttonBack: ImageButton
    private lateinit var recyclerViewUser: RecyclerView

    private lateinit var db: FirebaseFirestore
    private lateinit var selectUserAdapter: SelectUserAdapter

    private val userList = mutableListOf<SelectableUser>()

    private lateinit var myUserId: String
    private lateinit var myUserName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat_room)

        db = FirebaseFirestore.getInstance()
        myUserId = CurrentUserProvider.userId(this)
        myUserName = CurrentUserProvider.userName(this)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadUsers()
    }

    private fun initViews() {
        etRoomName = findViewById(R.id.etRoomName)
        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        buttonBack = findViewById(R.id.buttonBack)
        recyclerViewUser = findViewById(R.id.recyclerViewUser)
    }

    private fun setupRecyclerView() {
        selectUserAdapter = SelectUserAdapter(userList)
        recyclerViewUser.layoutManager = LinearLayoutManager(this)
        recyclerViewUser.adapter = selectUserAdapter
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            finish()
        }

        btnCreateRoom.setOnClickListener {
            createGroupRoom()
        }
    }

    private fun loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener { snapshot ->
                    userList.clear()

                    for (document in snapshot.documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null && user.userId != myUserId) {
                            userList.add(
                                    SelectableUser(
                                            userId = user.userId,
                                            userName = user.userName,
                                            email = user.email
                                    )
                            )
                        }
                    }

                    selectUserAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "유저 목록 불러오기 실패", Toast.LENGTH_SHORT).show()
                }
    }

    private fun createGroupRoom() {
        val roomName = etRoomName.text.toString().trim()
        val selectedUsers = selectUserAdapter.getSelectedUsers()

        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "최소 1명 이상 선택하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val isGroup = selectedUsers.size >= 2

        if (isGroup && roomName.isEmpty()) {
            Toast.makeText(this, "그룹 채팅방 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val roomRef = db.collection("chats").document()
        val roomId = roomRef.id

        val participants = mutableListOf<String>()
        participants.add(myUserId)
        participants.addAll(selectedUsers.map { it.userId })

        val participantNames = mutableMapOf<String, String>()
        participantNames[myUserId] = myUserName
        selectedUsers.forEach { user ->
            participantNames[user.userId] = user.userName
        }

        val unreadCount = mutableMapOf<String, Int>()
        participants.forEach { userId ->
            unreadCount[userId] = 0
        }

        val chatRoom = ChatRoom(
                roomId = roomId,
                roomName = if (isGroup) roomName else "",
                participants = participants,
                participantNames = participantNames,
                lastMessage = "",
                lastMessageTime = 0L,
                unreadCount = unreadCount,
                group = isGroup,
                createdBy = myUserId,
                createdAt = System.currentTimeMillis()
        )

        roomRef.set(chatRoom)
                .addOnSuccessListener {
                    Toast.makeText(this, "채팅방 생성 완료", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("chatRoomId", roomId)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "생성 실패", Toast.LENGTH_SHORT).show()
                }
    }
}
