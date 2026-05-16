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
        val meetingCreateButton = findViewById<Button>(R.id.meetingCreateButton)
        val meetingJoinButton = findViewById<Button>(R.id.meetingJoinButton)
        val mapButton = findViewById<Button>(R.id.mapButton)
        val mypageButton = findViewById<Button>(R.id.mypageButton)


        val postButton = findViewById<Button>(R.id.postButton)

        chatButton.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        meetingCreateButton.setOnClickListener {
            startActivity(Intent(this, MeetingCreateActivity::class.java))
        }

        meetingJoinButton.setOnClickListener {
            startActivity(Intent(this, MeetingJoinActivity::class.java))
        }

        mapButton.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        mypageButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        postButton.setOnClickListener {
            startActivity(Intent(this, CommunityListActivity::class.java))
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            CurrentUserProvider.clear(this)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}