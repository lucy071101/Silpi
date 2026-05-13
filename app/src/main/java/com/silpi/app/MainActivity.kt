package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 이미 로그인된 상태면 홈으로 바로 이동
        if (auth.currentUser != null) {
            loadProfileAndMove()
            return
        }

        setContentView(R.layout.activity_main)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val switchButton = findViewById<Button>(R.id.switchButton)
        val recoveryButton = findViewById<Button>(R.id.recoveryButton)

        // 로그인
        confirmButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                        loadProfileAndMove()
                    } else {
                        Toast.makeText(
                            this,
                            "이메일 또는 비밀번호 오류",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // 회원가입 화면 이동
        switchButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // 비밀번호 재설정 메일 전송
        recoveryButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "비밀번호 재설정 메일을 보냈습니다",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "메일 전송 실패: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun loadProfileAndMove() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    if (document.exists() && user != null) {
                        CurrentUserProvider.saveUserProfile(this, user)
                        moveToNextScreen(CurrentUserProvider.isProfileCompleted(user))
                    } else {
                        CurrentUserProvider.saveSignedInUser(
                                context = this,
                                userId = currentUser.uid,
                                userName = "",
                                email = currentUser.email.orEmpty()
                        )
                        moveToNextScreen(profileCompleted = false)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "프로필 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    moveToProfileLoadFailed()
                }
    }

    private fun moveToProfileLoadFailed() {
        val intent = Intent(this, ProfileActivity::class.java)
                .putExtra(ProfileActivity.EXTRA_PROFILE_LOAD_FAILED, true)
        startActivity(intent)
        finish()
    }

    private fun moveToNextScreen(profileCompleted: Boolean) {
        val intent = if (profileCompleted) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, ProfileActivity::class.java)
                    .putExtra(ProfileActivity.EXTRA_FIRST_SETUP, true)
        }

        startActivity(intent)
        finish()
    }
}
