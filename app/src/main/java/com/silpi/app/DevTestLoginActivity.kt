package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DevTestLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val testUsers = listOf(
            TestLoginUser("주댕이", "test999@example.com", "123456"),
            TestLoginUser("테스트123", "test123@example.com", "123456"),
            TestLoginUser("테스트777", "test777@example.com", "123456")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev_test_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<TextView>(R.id.textViewCurrentUser).text =
                "현재 사용자: ${CurrentUserProvider.userName(this)}"

        findViewById<Button>(R.id.buttonLoginUser999).setOnClickListener {
            signInTestUser(testUsers[0])
        }
        findViewById<Button>(R.id.buttonLoginUser123).setOnClickListener {
            signInTestUser(testUsers[1])
        }
        findViewById<Button>(R.id.buttonLoginUser777).setOnClickListener {
            signInTestUser(testUsers[2])
        }
        findViewById<Button>(R.id.buttonContinueChat).setOnClickListener {
            openChatList()
        }
    }

    private fun signInTestUser(testUser: TestLoginUser) {
        auth.signInWithEmailAndPassword(testUser.email, testUser.password)
                .addOnSuccessListener {
                    registerSignedInUser(testUser)
                }
                .addOnFailureListener {
                    createAndRegisterTestUser(testUser)
                }
    }

    private fun createAndRegisterTestUser(testUser: TestLoginUser) {
        auth.createUserWithEmailAndPassword(testUser.email, testUser.password)
                .addOnSuccessListener {
                    registerSignedInUser(testUser)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                            this,
                            "테스트 로그인 실패: ${e.message}",
                            Toast.LENGTH_LONG
                    ).show()
                }
    }

    private fun registerSignedInUser(testUser: TestLoginUser) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "로그인 사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = User(
                userId = firebaseUser.uid,
                userName = testUser.userName
        )

        CurrentUserProvider.saveSignedInUser(
                context = this,
                userId = user.userId,
                userName = user.userName,
                email = testUser.email
        )

        db.collection("users")
                .document(user.userId)
                .set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "${testUser.userName} 로그인 완료", Toast.LENGTH_SHORT).show()
                    openChatList()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "유저 등록 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
    }

    private fun openChatList() {
        startActivity(Intent(this, ChatListActivity::class.java))
        finish()
    }

    private data class TestLoginUser(
            val userName: String,
            val email: String,
            val password: String
    )
}
