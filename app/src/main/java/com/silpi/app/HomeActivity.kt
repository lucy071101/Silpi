package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()

        val chatButton = findViewById<Button>(R.id.chatButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        chatButton.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            CurrentUserProvider.clear(this)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
