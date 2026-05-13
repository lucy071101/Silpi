package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FIRST_SETUP = "extra_first_setup"
        const val EXTRA_PROFILE_LOAD_FAILED = "extra_profile_load_failed"
    }

    private lateinit var buttonBack: ImageButton
    private lateinit var buttonSave: ImageButton
    private lateinit var imageProfile: ImageView
    private lateinit var editTextName: EditText
    private lateinit var editTextCity: EditText
    private lateinit var editTextBio: EditText
    private lateinit var textViewInterests: TextView
    private lateinit var layoutProfileLoadError: View
    private lateinit var textViewProfileLoadError: TextView
    private lateinit var buttonRetryProfileLoad: Button

    private lateinit var db: FirebaseFirestore
    private var profileImageData: String = ""
    private var isFirstSetup: Boolean = false
    private var profileLoadFailed: Boolean = false
    private val interests = listOf("게임", "독서", "음악 감상", "카페 탐방", "러닝", "영화")

    private val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            profileImageData = ProfileImageHelper.encodeProfileImage(this, imageUri)
            ProfileImageHelper.setProfileImage(imageProfile, profileImageData)
            saveProfile(showToast = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()
        isFirstSetup = intent.getBooleanExtra(EXTRA_FIRST_SETUP, false)
        profileLoadFailed = intent.getBooleanExtra(EXTRA_PROFILE_LOAD_FAILED, false)

        initViews()
        bindProfile()
        setupClickListeners()
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.buttonBack)
        buttonSave = findViewById(R.id.buttonSave)
        imageProfile = findViewById(R.id.imageProfile)
        editTextName = findViewById(R.id.editTextName)
        editTextCity = findViewById(R.id.editTextCity)
        editTextBio = findViewById(R.id.editTextBio)
        textViewInterests = findViewById(R.id.textViewInterests)
        layoutProfileLoadError = findViewById(R.id.layoutProfileLoadError)
        textViewProfileLoadError = findViewById(R.id.textViewProfileLoadError)
        buttonRetryProfileLoad = findViewById(R.id.buttonRetryProfileLoad)
    }

    private fun bindProfile() {
        if (isFirstSetup) {
            editTextName.setText("")
        } else {
            editTextName.setText(CurrentUserProvider.userName(this))
        }
        editTextCity.setText(CurrentUserProvider.city(this))
        editTextBio.setText(CurrentUserProvider.bio(this))
        textViewInterests.text = interests.joinToString("   ") { interest -> "#$interest" }

        profileImageData = CurrentUserProvider.profileImageData(this)
        ProfileImageHelper.setProfileImage(imageProfile, profileImageData)
    }

    private fun setupClickListeners() {
        if (isFirstSetup || profileLoadFailed) {
            buttonBack.visibility = View.INVISIBLE
        }

        if (profileLoadFailed) {
            showProfileLoadFailedState()
        }

        buttonBack.setOnClickListener {
            finish()
        }

        imageProfile.setOnClickListener {
            if (!profileLoadFailed) {
                imagePickerLauncher.launch("image/*")
            }
        }

        buttonSave.setOnClickListener {
            saveProfile(showToast = true, goHomeAfterSave = isFirstSetup)
        }

        buttonRetryProfileLoad.setOnClickListener {
            retryProfileLoad()
        }
    }

    private fun saveProfile(showToast: Boolean, goHomeAfterSave: Boolean = false) {
        if (profileLoadFailed) {
            Toast.makeText(this, "프로필 정보를 먼저 다시 불러와주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val enteredUserName = editTextName.text.toString().trim()
        if (isFirstSetup && enteredUserName.isBlank()) {
            Toast.makeText(this, "사용자명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val userName = enteredUserName.ifBlank { TestUser.USER_NAME }
        val city = editTextCity.text.toString().trim().ifBlank { "서울시 강남구" }
        val bio = editTextBio.text.toString().trim()

        CurrentUserProvider.saveProfile(
                context = this,
                userName = userName,
                city = city,
                bio = bio,
                interests = interests,
                profileImageData = profileImageData
        )

        val user = CurrentUserProvider.user(this)
        db.collection("users")
                .document(user.userId)
                .set(user, SetOptions.merge())
                .addOnSuccessListener {
                    if (goHomeAfterSave) {
                        CurrentUserProvider.markProfileCompleted(this)
                        moveToHome()
                        return@addOnSuccessListener
                    }
                    if (showToast) {
                        Toast.makeText(this, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "프로필 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
    }

    private fun moveToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showProfileLoadFailedState() {
        layoutProfileLoadError.visibility = View.VISIBLE
        textViewProfileLoadError.visibility = View.VISIBLE
        buttonRetryProfileLoad.visibility = View.VISIBLE

        buttonSave.isEnabled = false
        buttonSave.alpha = 0.35f
        imageProfile.isEnabled = false
        editTextName.isEnabled = false
        editTextCity.isEnabled = false
        editTextBio.isEnabled = false
    }

    private fun retryProfileLoad() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
