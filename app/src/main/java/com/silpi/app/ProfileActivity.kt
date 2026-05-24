package com.silpi.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FIRST_SETUP = "extra_first_setup"
        const val EXTRA_PROFILE_LOAD_FAILED = "extra_profile_load_failed"
    }

    private lateinit var buttonBack: ImageButton
    private lateinit var buttonSave: TextView
    private lateinit var imageProfile: ImageView
    private lateinit var editTextName: EditText
    private lateinit var editTextCity: EditText
    private lateinit var editTextBio: EditText
    private lateinit var textViewEditHint: TextView
    private lateinit var textViewInterests: TextView
    private lateinit var buttonSelectInterests: TextView
    private lateinit var layoutInterestsEdit: View
    private lateinit var layoutProfileLoadError: View
    private lateinit var textViewProfileLoadError: TextView
    private lateinit var buttonRetryProfileLoad: Button

    private lateinit var db: FirebaseFirestore
    private var profileImageData: String = ""
    private var isFirstSetup: Boolean = false
    private var isEditing: Boolean = false
    private var profileLoadFailed: Boolean = false

    private val interestOptions = listOf("게임", "독서", "음악 감상", "카페 탐방", "러닝", "영화", "맛집", "여행", "운동", "공부")
    private val selectedInterests = mutableListOf<String>()

    private val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null && isEditing && !profileLoadFailed) {
            profileImageData = ProfileImageHelper.encodeProfileImage(this, imageUri)
            ProfileImageHelper.setProfileImage(imageProfile, profileImageData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()
        isFirstSetup = intent.getBooleanExtra(EXTRA_FIRST_SETUP, false)
        profileLoadFailed = intent.getBooleanExtra(EXTRA_PROFILE_LOAD_FAILED, false)
        isEditing = isFirstSetup && !profileLoadFailed

        initViews()
        bindProfile()
        setupClickListeners()
        applyEditMode()
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.buttonBack)
        buttonSave = findViewById(R.id.buttonSave)
        imageProfile = findViewById(R.id.imageProfile)
        editTextName = findViewById(R.id.editTextName)
        editTextCity = findViewById(R.id.editTextCity)
        editTextBio = findViewById(R.id.editTextBio)
        textViewEditHint = findViewById(R.id.textViewEditHint)
        textViewInterests = findViewById(R.id.textViewInterests)
        buttonSelectInterests = findViewById(R.id.buttonSelectInterests)
        layoutInterestsEdit = findViewById(R.id.layoutInterestsEdit)
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

        selectedInterests.clear()
        if (!isFirstSetup) {
            selectedInterests.addAll(CurrentUserProvider.interests(this))
        }
        updateInterestText()

        profileImageData = CurrentUserProvider.profileImageData(this)
        ProfileImageHelper.setProfileImage(imageProfile, profileImageData)
    }

    private fun setupClickListeners() {
        if (profileLoadFailed) {
            buttonBack.visibility = View.INVISIBLE
        }

        if (profileLoadFailed) {
            showProfileLoadFailedState()
        }

        buttonBack.setOnClickListener {
            if (isFirstSetup) {
                signOutAndMoveToLogin()
                return@setOnClickListener
            }

            finish()
        }

        imageProfile.setOnClickListener {
            if (isEditing && !profileLoadFailed) {
                imagePickerLauncher.launch("image/*")
            }
        }

        textViewInterests.setOnClickListener {
            if (isEditing && !profileLoadFailed) {
                showInterestDialog()
            }
        }

        buttonSelectInterests.setOnClickListener {
            if (isEditing && !profileLoadFailed) {
                showInterestDialog()
            }
        }

        layoutInterestsEdit.setOnClickListener {
            if (isEditing && !profileLoadFailed) {
                showInterestDialog()
            }
        }

        buttonSave.setOnClickListener {
            if (profileLoadFailed) return@setOnClickListener

            if (!isEditing) {
                isEditing = true
                applyEditMode()
                return@setOnClickListener
            }

            saveProfile(showToast = true, goHomeAfterSave = isFirstSetup)
        }

        buttonRetryProfileLoad.setOnClickListener {
            retryProfileLoad()
        }
    }

    private fun applyEditMode() {
        if (profileLoadFailed) return

        buttonSave.text = if (isEditing) "저장" else "수정"
        buttonSave.setTextColor(if (isEditing) 0xFFFFFFFF.toInt() else 0xFF162033.toInt())
        buttonSave.setBackgroundResource(if (isEditing) R.drawable.bg_profile_save_button else R.drawable.bg_edit_text_clear)

        imageProfile.isEnabled = isEditing
        textViewEditHint.visibility = if (isEditing) View.VISIBLE else View.GONE
        textViewInterests.isEnabled = isEditing
        textViewInterests.alpha = if (isEditing) 1.0f else 0.92f
        buttonSelectInterests.visibility = if (isEditing) View.VISIBLE else View.GONE
        layoutInterestsEdit.setBackgroundResource(
                if (isEditing) R.drawable.bg_profile_soft_edit_field else R.drawable.bg_interest_chip
        )

        setEditTextEditable(editTextName, isEditing)
        setEditTextEditable(editTextCity, isEditing)
        setEditTextEditable(editTextBio, isEditing)
        updateEditFieldStyle(editTextName, isEditing)
        updateSoftEditFieldStyle(editTextCity, isEditing)
        updateSoftEditFieldStyle(editTextBio, isEditing)
        updateInterestText()
    }

    private fun setEditTextEditable(editText: EditText, editable: Boolean) {
        editText.isFocusable = editable
        editText.isFocusableInTouchMode = editable
        editText.isCursorVisible = editable
        editText.isLongClickable = editable
    }

    private fun updateEditFieldStyle(editText: EditText, editable: Boolean) {
        editText.setBackgroundResource(
                if (editable) R.drawable.bg_profile_edit_field else R.drawable.bg_edit_text_clear
        )
    }

    private fun updateSoftEditFieldStyle(editText: EditText, editable: Boolean) {
        editText.setBackgroundResource(
                if (editable) R.drawable.bg_profile_soft_edit_field else R.drawable.bg_interest_chip
        )
    }

    private fun showInterestDialog() {
        val tempSelectedInterests = selectedInterests.toMutableList()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_interest_picker, null)
        val searchEditText = dialogView.findViewById<EditText>(R.id.editTextInterestSearch)
        val optionGrid = dialogView.findViewById<GridLayout>(R.id.gridInterestOptions)
        val confirmButton = dialogView.findViewById<TextView>(R.id.buttonConfirmInterest)

        fun bindInterestOptions(keyword: String = "") {
            optionGrid.removeAllViews()
            val filteredOptions = interestOptions.filter {
                it.contains(keyword.trim(), ignoreCase = true)
            }

            for (interest in filteredOptions) {
                val optionView = TextView(this)
                val isSelected = tempSelectedInterests.contains(interest)
                optionView.text = interest
                optionView.gravity = android.view.Gravity.CENTER
                optionView.textSize = 22f
                optionView.setTypeface(null, android.graphics.Typeface.BOLD)
                optionView.setBackgroundResource(R.drawable.bg_interest_option)
                optionView.isSelected = isSelected
                optionView.setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF7D68A8.toInt())
                optionView.setOnClickListener {
                    if (tempSelectedInterests.contains(interest)) {
                        tempSelectedInterests.remove(interest)
                        optionView.isSelected = false
                        optionView.setTextColor(0xFF7D68A8.toInt())
                    } else {
                        tempSelectedInterests.add(interest)
                        optionView.isSelected = true
                        optionView.setTextColor(0xFFFFFFFF.toInt())
                    }
                }

                val layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dpToPx(92)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(0, 0, dpToPx(16), dpToPx(18))
                }
                optionGrid.addView(optionView, layoutParams)
            }
        }

        val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bindInterestOptions(s.toString())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        confirmButton.setOnClickListener {
            selectedInterests.clear()
            selectedInterests.addAll(tempSelectedInterests)
            updateInterestText()
            dialog.dismiss()
        }

        bindInterestOptions()
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updateInterestText() {
        textViewInterests.text = if (selectedInterests.isEmpty()) {
            if (isEditing) "취미를 선택해주세요" else "관심사를 선택하세요"
        } else {
            selectedInterests.joinToString("   ") { interest -> "#$interest" }
        }
    }

    private fun saveProfile(showToast: Boolean, goHomeAfterSave: Boolean = false) {
        if (profileLoadFailed) {
            Toast.makeText(this, "프로필 정보를 먼저 다시 불러와주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val enteredUserName = editTextName.text.toString().trim()
        if (enteredUserName.isBlank()) {
            Toast.makeText(this, "사용자명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val userName = enteredUserName
        val city = editTextCity.text.toString().trim().ifBlank { "서울시 강남구" }
        val bio = editTextBio.text.toString().trim()

        CurrentUserProvider.saveProfile(
                context = this,
                userName = userName,
                city = city,
                bio = bio,
                interests = selectedInterests,
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

                    isEditing = false
                    applyEditMode()

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

    override fun onBackPressed() {
        if (isFirstSetup) {
            signOutAndMoveToLogin()
            return
        }

        super.onBackPressed()
    }

    private fun signOutAndMoveToLogin() {
        FirebaseAuth.getInstance().signOut()
        CurrentUserProvider.clear(this)

        val intent = Intent(this, MainActivity::class.java)
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
        textViewInterests.isEnabled = false
        setEditTextEditable(editTextName, false)
        setEditTextEditable(editTextCity, false)
        setEditTextEditable(editTextBio, false)
    }

    private fun retryProfileLoad() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
