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
        // 🌟 뒤로 가기 버튼 누르면 상세 화면 닫기!
        android.widget.ImageView btnDetailBack = findViewById(R.id.btn_detail_back);
        if (btnDetailBack != null) {
            btnDetailBack.setOnClickListener(v -> finish());
        }
        // 이전 화면(목록)에서 클릭한 게시물의 ID를 받아옵니다.
        postId = getIntent().getStringExtra("postId");

        initViews();

        // 🌟 추가된 부분: 어댑터를 연결하고 댓글을 보여줄 준비를 합니다.
        commentAdapter = new CommentAdapter();
        rvComments.setAdapter(commentAdapter);

        // postId가 잘 넘어왔을 때만 데이터를 불러옵니다 (에러 방지)
        if (postId != null) {
            loadPostDetail(); // 게시글 본문 불러오기
            loadComments();   // 🌟 추가된 부분: 댓글 리스트 불러오기
        }

        // 공감(추천) 버튼 클릭 시
        btnRecommend.setOnClickListener(v -> {
            if (postId != null) {
                db.collection("posts").document(postId)
                        .update("recommendCount", FieldValue.increment(1));
            }
        });

        // 댓글 전송 버튼 클릭 시
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

        // 🌟 추가된 부분: RecyclerView(댓글 리스트)를 화면과 연결합니다.
        rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadPostDetail() {
        // 실시간 업데이트(SnapshotListener)를 사용하여 추천수가 바로 바뀌게 합니다.
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

    private void saveComment(String text) {
        if (postId == null) return;

        Map<String, Object> comment = new HashMap<>();
        comment.put("content", text);
        comment.put("timestamp", FieldValue.serverTimestamp());
        comment.put("isAnonymous", true);

        // 'posts' 문서 안의 'comments'라는 하위 컬렉션에 저장합니다.
        db.collection("posts").document(postId).collection("comments").add(comment)
                .addOnSuccessListener(doc -> {
                    etComment.setText("");
                    Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // 🌟 추가된 부분: 파이어베이스에서 댓글을 시간순으로 불러오는 메서드입니다.
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