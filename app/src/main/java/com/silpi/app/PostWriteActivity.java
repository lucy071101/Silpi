package com.silpi.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PostWriteActivity extends AppCompatActivity {

    // 파이어베이스 도구 초기화
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private ImageView btnCancel, btnPhoto, ivPreview;
    private TextView btnComplete;
    private EditText etTitle, etContent;
    private CheckBox cbAnonymous;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivPreview.setImageURI(selectedImageUri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_write);

        initViews();
        setClickListeners();
        addInputWatcher();
    }

    private void initViews() {
        btnCancel = findViewById(R.id.btn_cancel);
        btnPhoto = findViewById(R.id.btn_photo);
        ivPreview = findViewById(R.id.iv_preview);
        btnComplete = findViewById(R.id.btn_complete);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        cbAnonymous = findViewById(R.id.cb_anonymous);
    }

    private void setClickListeners() {
        btnCancel.setOnClickListener(v -> finish());

        btnPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnComplete.setOnClickListener(v -> uploadPostWithImage());
    }

    // 사진 유무에 따른 업로드 로직 분리
    private void uploadPostWithImage() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        boolean isAnonymous = cbAnonymous.isChecked();

        // 중복 클릭 방지
        btnComplete.setEnabled(false);
        Toast.makeText(this, "동네 소식을 올리는 중...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            // 사진이 있는 경우: 스토리지에 먼저 업로드
            String fileName = "posts/" + System.currentTimeMillis() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            ref.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveToFirestore(title, content, uri.toString(), isAnonymous);
                    }))
                    .addOnFailureListener(e -> {
                        btnComplete.setEnabled(true);
                        Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 사진이 없는 경우: 텍스트만 저장
            saveToFirestore(title, content, null, isAnonymous);
        }
    }

    private void saveToFirestore(String title, String content, String imageUrl, boolean isAnonymous) {
        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("content", content);
        post.put("imageUrl", imageUrl); // 이미지 주소 추가
        post.put("isAnonymous", isAnonymous);
        post.put("recommendCount", 0);
        post.put("commentCount", 0);
        post.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "글이 동네 소식에 올라갔어요!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnComplete.setEnabled(true);
                    Toast.makeText(this, "저장 실패: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addInputWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasTitle = !etTitle.getText().toString().trim().isEmpty();
                boolean hasContent = !etContent.getText().toString().trim().isEmpty();

                if (hasTitle && hasContent) {
                    btnComplete.setEnabled(true);
                    btnComplete.setTextColor(Color.WHITE);
                } else {
                    btnComplete.setEnabled(false);
                    // ContextCompat을 사용하여 안전하게 색상을 가져옵니다.
                    btnComplete.setTextColor(ContextCompat.getColor(PostWriteActivity.this, R.color.silphy_divider));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etTitle.addTextChangedListener(watcher);
        etContent.addTextChangedListener(watcher);
    }
}