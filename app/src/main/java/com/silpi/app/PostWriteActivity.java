package com.silpi.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
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

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PostWriteActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // 🌟 Storage 관련 코드 삭제 완료!

    private ImageView btnCancel, btnPhoto, ivPreview;
    private TextView btnComplete;
    private EditText etTitle, etContent;
    private CheckBox cbAnonymous;

    private Uri selectedImageUri;
    private String category = "일반";

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

        if (getIntent().hasExtra("category")) {
            category = getIntent().getStringExtra("category");
        }

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

    // 🌟 Storage 대신 사진을 글자로 암호화해서 올리도록 변경!
    private void uploadPostWithImage() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        boolean isAnonymous = cbAnonymous.isChecked();

        btnComplete.setEnabled(false);
        Toast.makeText(this, "동네 소식을 올리는 중...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            // 사진을 Base64 글자로 변환
            String base64Image = encodeImageToBase64(selectedImageUri);

            if (base64Image != null) {
                // 변환된 글자 데이터를 데이터베이스에 바로 저장
                saveToFirestore(title, content, base64Image, isAnonymous);
            } else {
                btnComplete.setEnabled(true);
                Toast.makeText(this, "사진을 처리하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            saveToFirestore(title, content, null, isAnonymous);
        }
    }

    // 🌟 사진(Uri)을 엄청나게 긴 글자(Base64)로 바꿔주는 마법의 해독기(변환기)
    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 사진 용량을 살짝 압축해서 데이터베이스가 버거워하지 않게 합니다 (품질 70%)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFirestore(String title, String content, String imageUrl, boolean isAnonymous) {
        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("content", content);
        post.put("imageUrl", imageUrl); // 여기에 Base64 암호화 글자가 들어갑니다!
        post.put("isAnonymous", isAnonymous);
        post.put("category", category);
        post.put("recommendCount", 0);
        post.put("commentCount", 0);
        post.put("timestamp", FieldValue.serverTimestamp());

        String myUserId = CurrentUserProvider.INSTANCE.userId(this);
        post.put("authorId", myUserId);

        if (isAnonymous) {
            post.put("authorName", "익명");
            post.put("authorProfile", "");
        } else {
            String myName = CurrentUserProvider.INSTANCE.userName(this);
            String myProfile = CurrentUserProvider.INSTANCE.profileImageData(this);

            if (myName == null || myName.trim().isEmpty()) {
                myName = "동네주민";
            }

            post.put("authorName", myName);
            post.put("authorProfile", (myProfile != null) ? myProfile : "");
        }

        uploadToDb(post);
    }

    private void uploadToDb(Map<String, Object> post) {
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