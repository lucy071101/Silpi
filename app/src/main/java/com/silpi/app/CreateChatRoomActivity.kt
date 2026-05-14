package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CreateChatRoomActivity : AppCompatActivity() {

    private lateinit var editTextUserSearch: EditText
    private lateinit var btnCreateRoom: Button
    private lateinit var buttonBack: ImageButton
    private lateinit var recyclerViewUser: RecyclerView
    private lateinit var recyclerViewSelectedUser: RecyclerView

    private lateinit var db: FirebaseFirestore
    private lateinit var selectUserAdapter: SelectUserAdapter
    private lateinit var selectedUserAdapter: SelectedUserAdapter

    private val allUserList = mutableListOf<SelectableUser>()
    private val userList = mutableListOf<SelectableUser>()
    private val selectedUserList = mutableListOf<SelectableUser>()

    private lateinit var myUserId: String
    private lateinit var myUserName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat_room)

        db = FirebaseFirestore.getInstance()
        myUserId = CurrentUserProvider.userId(this)
        myUserName = CurrentUserProvider.userName(this)

        initViews()
        setupRecyclerViews()
        setupClickListeners()
        loadUsers()
    }

    private fun initViews() {
        editTextUserSearch = findViewById(R.id.editTextUserSearch)
        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        buttonBack = findViewById(R.id.buttonBack)
        recyclerViewUser = findViewById(R.id.recyclerViewUser)
        recyclerViewSelectedUser = findViewById(R.id.recyclerViewSelectedUser)
    }

    private fun setupRecyclerViews() {
        selectUserAdapter = SelectUserAdapter(userList) {
            updateSelectedUsers()
        }

        selectedUserAdapter = SelectedUserAdapter(selectedUserList) { user ->
            user.isSelected = false
            updateSelectedUsers()
            selectUserAdapter.notifyDataSetChanged()
        }

        recyclerViewUser.layoutManager = LinearLayoutManager(this)
        recyclerViewUser.adapter = selectUserAdapter

        recyclerViewSelectedUser.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewSelectedUser.adapter = selectedUserAdapter
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            finish()
        }

        btnCreateRoom.setOnClickListener {
            val selectedUsers = getSelectedUsers()
            if (selectedUsers.isEmpty()) {
                Toast.makeText(this, "대화상대를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedUsers.size == 1) {
                createChatRoom(roomName = "")
            } else {
                showRoomNameDialog()
            }
        }

        editTextUserSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyUserFilter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener { snapshot ->
                    allUserList.clear()

                    for (document in snapshot.documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null && user.userId != myUserId) {
                            allUserList.add(
                                    SelectableUser(
                                            userId = user.userId,
                                            userName = user.userName,
                                            email = user.email,
                                            profileImageData = user.profileImageData
                                    )
                            )
                        }
                    }

                    applyUserFilter(editTextUserSearch.text.toString())
                    updateSelectedUsers()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "사용자 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
    }

    private fun applyUserFilter(keyword: String) {
        val normalizedKeyword = normalizeSearchText(keyword)
        userList.clear()

        if (normalizedKeyword.isBlank()) {
            userList.addAll(allUserList)
        } else {
            userList.addAll(
                    allUserList.filter { user ->
                        normalizeSearchText(user.userName).contains(normalizedKeyword) ||
                                user.email.toHandle().contains(normalizedKeyword)
                    }
            )
        }

        selectUserAdapter.notifyDataSetChanged()
    }

    private fun updateSelectedUsers() {
        selectedUserList.clear()
        selectedUserList.addAll(getSelectedUsers())
        selectedUserAdapter.notifyDataSetChanged()

        recyclerViewSelectedUser.visibility =
                if (selectedUserList.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

        btnCreateRoom.text = if (selectedUserList.isEmpty()) {
            "생성"
        } else {
            "${selectedUserList.size} 생성"
        }
    }

    private fun showRoomNameDialog() {
        val roomNameInput = EditText(this)
        roomNameInput.hint = "방 이름 입력"
        roomNameInput.setSingleLine(true)
        roomNameInput.setPadding(32, 24, 32, 24)

        val dialog = AlertDialog.Builder(this)
                .setTitle("방 이름 설정")
                .setView(roomNameInput)
                .setNegativeButton("취소", null)
                .setPositiveButton("생성", null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val roomName = roomNameInput.text.toString().trim()
                if (roomName.isBlank()) {
                    Toast.makeText(this, "방 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                hideKeyboard(roomNameInput)
                dialog.dismiss()
                createChatRoom(roomName)
            }

            roomNameInput.requestFocus()
            dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        dialog.show()
    }

    private fun createChatRoom(roomName: String) {
        val selectedUsers = getSelectedUsers()

        if (selectedUsers.isEmpty()) {
            Toast.makeText(this, "대화상대를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val isGroup = selectedUsers.size >= 2
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

        val searchNames = mutableMapOf<String, String>()
        if (isGroup) {
            val normalizedRoomName = normalizeSearchText(roomName)
            participants.forEach { userId ->
                searchNames[userId] = normalizedRoomName
            }
        } else {
            val otherUser = selectedUsers.first()
            searchNames[myUserId] = normalizeSearchText(otherUser.userName)
            searchNames[otherUser.userId] = normalizeSearchText(myUserName)
        }

        val chatRoom = ChatRoom(
                roomId = roomId,
                roomName = if (isGroup) roomName else "",
                participants = participants,
                participantNames = participantNames,
                searchName = normalizeSearchText(if (isGroup) roomName else myUserName),
                searchNames = searchNames,
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

    private fun getSelectedUsers(): List<SelectableUser> {
        return allUserList.filter { it.isSelected }
    }

    private fun normalizeSearchText(text: String): String {
        return text.trim().lowercase()
    }

    private fun String.toHandle(): String {
        val handle = substringBefore("@").trim()
        return if (handle.isNotEmpty()) "@$handle".lowercase() else ""
    }

    private fun hideKeyboard(editText: EditText) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
    }
}
