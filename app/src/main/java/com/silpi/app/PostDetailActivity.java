package com.silpi.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private String postId; // 게시물 고유 ID
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView tvTitle, tvContent, tvAuthor, btnRecommend;
    private EditText etComment;
    private ImageView btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        android.widget.ImageView btnDetailBack = findViewById(R.id.btn_detail_back);
        if (btnDetailBack != null) {
            btnDetailBack.setOnClickListener(v -> finish());
        }

        postId = getIntent().getStringExtra("postId");

        initViews();

        commentAdapter = new CommentAdapter();
        rvComments.setAdapter(commentAdapter);

        if (postId != null) {
            loadPostDetail();
            loadComments();
        }

        btnRecommend.setOnClickListener(v -> {
            if (postId != null) {
                db.collection("posts").document(postId)
                        .update("recommendCount", FieldValue.increment(1));
            }
        });

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) {
                saveComment(text);
            }
        });
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvAuthor = findViewById(R.id.tv_detail_author);
        btnRecommend = findViewById(R.id.btn_recommend);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_comment_send);

        rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadPostDetail() {
        db.collection("posts").document(postId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        tvTitle.setText(snapshot.getString("title"));
                        tvContent.setText(snapshot.getString("content"));
                        long recommends = snapshot.getLong("recommendCount") != null ? snapshot.getLong("recommendCount") : 0;
                        btnRecommend.setText("👍 공감 " + recommends);
                    }
                });
    }

    // 🌟 이 메서드 내부가 업데이트되었습니다!
    private void saveComment(String text) {
        if (postId == null) return;

        Map<String, Object> comment = new HashMap<>();
        comment.put("content", text);
        comment.put("timestamp", FieldValue.serverTimestamp());
        comment.put("isAnonymous", true);

        // 'posts' 문서 안의 'comments'라는 하위 컬렉션에 저장합니다.
        db.collection("posts").document(postId).collection("comments").add(comment)
                .addOnSuccessListener(doc -> {

                    // 🌟 [핵심 추가] 댓글 저장 성공 시, 부모 게시글의 commentCount를 안전하게 1 증가시킵니다.
                    db.collection("posts").document(postId)
                            .update("commentCount", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> {
                                // 카운트까지 성공적으로 올라가면 입력창을 비우고 토스트를 띄웁니다.
                                etComment.setText("");
                                Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                // 혹시나 카운트 올리기가 실패하더라도 댓글은 써졌으므로 입력창은 비워줍니다.
                                etComment.setText("");
                            });

                });
    }

    private void loadComments() {
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        commentAdapter.setComments(value.getDocuments());
                    }
                });
    }
}