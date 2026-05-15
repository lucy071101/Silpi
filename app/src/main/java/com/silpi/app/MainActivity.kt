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

        // 1. 자동 로그인 체크 (임시: 인증 여부 상관없이 로그인되어 있으면 통과)
        val currentUser = auth.currentUser
        // if (currentUser != null && currentUser.isEmailVerified) { // <-- 기존 로직 주석
        if (currentUser != null) { // <-- 임시 로직
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val switchButton = findViewById<Button>(R.id.switchButton)
        val recoveryButton = findViewById<Button>(R.id.recoveryButton)

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
                        /* [임시 주석: 테스트를 위해 이메일 인증 확인 절차를 끕니다]
                        val user = auth.currentUser
                        user?.reload()?.addOnCompleteListener { _ ->
                            if (user != null && user.isEmailVerified) {
                        */
                        Toast.makeText(this, "로그인 성공 (테스트 모드)", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                        /*
                            } else {
                                auth.signOut()
                                Toast.makeText(this, "이메일 인증이 필요합니다.", Toast.LENGTH_LONG).show()
                            }

                            user.reload().addOnCompleteListener {
                                if (user.isEmailVerified) {
                                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                                    loadProfileAndMove()
                                } else {
                                    auth.signOut()
                                    Toast.makeText(this, "이메일 인증이 필요합니다.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "이메일 또는 비밀번호 오류", Toast.LENGTH_SHORT).show()
                        }
                        */
                    } else {
                        Toast.makeText(this, "이메일 또는 비밀번호 오류", Toast.LENGTH_SHORT).show()
                    }
        }

        switchButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        recoveryButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "비밀번호 재설정 메일을 보냈습니다", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "메일 전송 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
