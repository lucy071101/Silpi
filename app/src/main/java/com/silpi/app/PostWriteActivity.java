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

    private ImageView btnCancel, btnPhoto, ivPreview;
    private TextView btnComplete;
    private EditText etTitle, etContent;
    private CheckBox cbAnonymous;

    private Uri selectedImageUri;
    private String category = "일반";

    private String editingPostId = null;

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

        if (getIntent().hasExtra("postId")) {
            editingPostId = getIntent().getStringExtra("postId");
            etTitle.setText(getIntent().getStringExtra("title"));
            etContent.setText(getIntent().getStringExtra("content"));
            btnComplete.setText("수정 완료");
        }
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

    private void uploadPostWithImage() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        boolean isAnonymous = cbAnonymous.isChecked();

        btnComplete.setEnabled(false);
        Toast.makeText(this, "동네 소식을 올리는 중...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            String base64Image = encodeImageToBase64(selectedImageUri);

            if (base64Image != null) {
                saveToFirestore(title, content, base64Image, isAnonymous);
            } else {
                btnComplete.setEnabled(true);
                Toast.makeText(this, "사진을 처리하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {

            saveToFirestore(title, content, null, isAnonymous);
        }
    }

    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFirestore(String title, String content, String imageUrl, boolean isAnonymous) {
        if (editingPostId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("title", title);
            updateData.put("content", content);
            updateData.put("isAnonymous", isAnonymous);

            if (imageUrl != null) {
                updateData.put("imageUrl", imageUrl);
            }

            db.collection("posts").document(editingPostId).update(updateData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "글이 수정되었습니다!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnComplete.setEnabled(true);
                        Toast.makeText(this, "수정 실패: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> post = new HashMap<>();
            post.put("title", title);
            post.put("content", content);

            if (imageUrl != null) {
                post.put("imageUrl", imageUrl);
            }

            post.put("isAnonymous", isAnonymous);
            post.put("category", category);
            post.put("timestamp", FieldValue.serverTimestamp());
            post.put("recommendCount", 0);
            post.put("commentCount", 0);

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