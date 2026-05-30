package com.silpi.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.HashMap;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private String postId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration postListener;
    private ListenerRegistration commentsListener;

    private TextView tvTitle, tvContent, tvAuthor, btnRecommend;
    private EditText etComment;
    private ImageView btnSend, ivProfile, btnMoreOptions, ivPostImage;
    private CheckBox cbCommentAnonymous;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        ImageView btnDetailBack = findViewById(R.id.btn_detail_back);
        if (btnDetailBack != null) btnDetailBack.setOnClickListener(v -> finish());

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
                db.collection("posts").document(postId).update("recommendCount", FieldValue.increment(1));
            }
        });

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) saveComment(text);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postListener != null) postListener.remove();
        if (commentsListener != null) commentsListener.remove();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        tvAuthor = findViewById(R.id.tv_detail_author);
        btnRecommend = findViewById(R.id.btn_recommend);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_comment_send);
        ivProfile = findViewById(R.id.iv_detail_profile);
        cbCommentAnonymous = findViewById(R.id.cb_comment_anonymous);
        btnMoreOptions = findViewById(R.id.btn_more_options);

        ivPostImage = findViewById(R.id.iv_detail_image);

        rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadPostDetail() {
        postListener = db.collection("posts").document(postId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        tvTitle.setText(snapshot.getString("title"));
                        tvContent.setText(snapshot.getString("content"));

                        String authorName = snapshot.getString("authorName");
                        tvAuthor.setText(authorName != null ? authorName : "익명");

                        String imageUrl = snapshot.getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            ivPostImage.setVisibility(View.VISIBLE);
                            try {
                                byte[] decodedBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT);
                                Glide.with(PostDetailActivity.this)
                                        .asBitmap()
                                        .load(decodedBytes)
                                        .into(ivPostImage);
                            } catch (IllegalArgumentException ex) {
                                ivPostImage.setVisibility(View.GONE);
                            }
                        } else {
                            ivPostImage.setVisibility(View.GONE);
                        }

                        Boolean isAnonymous = snapshot.getBoolean("isAnonymous");
                        String authorProfile = snapshot.getString("authorProfile");

                        if (Boolean.TRUE.equals(isAnonymous)) {
                            ivProfile.setImageResource(R.drawable.ic_default_profile);
                        } else if (authorProfile != null && !authorProfile.isEmpty()) {
                            if (authorProfile.startsWith("http")) {
                                Glide.with(PostDetailActivity.this)
                                        .load(authorProfile)
                                        .placeholder(R.drawable.ic_default_profile)
                                        .error(R.drawable.ic_default_profile)
                                        .circleCrop()
                                        .into(ivProfile);
                            } else {
                                try {
                                    byte[] decodedBytes = android.util.Base64.decode(authorProfile, android.util.Base64.DEFAULT);
                                    Glide.with(PostDetailActivity.this)
                                            .asBitmap()
                                            .load(decodedBytes)
                                            .placeholder(R.drawable.ic_default_profile)
                                            .error(R.drawable.ic_default_profile)
                                            .circleCrop()
                                            .into(ivProfile);
                                } catch (IllegalArgumentException ex) {
                                    ivProfile.setImageResource(R.drawable.ic_default_profile);
                                }
                            }
                        } else {
                            ivProfile.setImageResource(R.drawable.ic_default_profile);
                        }

                        long recommends = snapshot.getLong("recommendCount") != null ? snapshot.getLong("recommendCount") : 0;
                        btnRecommend.setText("👍 공감 " + recommends);

                        String authorId = snapshot.getString("authorId");
                        String myUserId = CurrentUserProvider.INSTANCE.userId(this);
                        String myName = CurrentUserProvider.INSTANCE.userName(this);

                        boolean isMyPost = false;
                        if (authorId != null && authorId.equals(myUserId)) {
                            isMyPost = true;
                        } else if (authorId == null && authorName != null && authorName.equals(myName) && !authorName.equals("익명")) {
                            isMyPost = true;
                        }

                        if (isMyPost) {
                            btnMoreOptions.setVisibility(View.VISIBLE);
                            btnMoreOptions.setOnClickListener(v -> {
                                PopupMenu popup = new PopupMenu(PostDetailActivity.this, btnMoreOptions);
                                popup.getMenu().add("수정");
                                popup.getMenu().add("삭제");
                                popup.setOnMenuItemClickListener(item -> {
                                    if (item.getTitle().equals("수정")) {
                                        Intent intent = new Intent(PostDetailActivity.this, PostWriteActivity.class);
                                        intent.putExtra("postId", postId);
                                        intent.putExtra("title", tvTitle.getText().toString());
                                        intent.putExtra("content", tvContent.getText().toString());
                                        startActivity(intent);
                                    } else if (item.getTitle().equals("삭제")) {
                                        new AlertDialog.Builder(PostDetailActivity.this)
                                                .setTitle("게시글 삭제")
                                                .setMessage("정말로 이 게시글을 삭제하시겠습니까?")
                                                .setPositiveButton("삭제", (dialog, which) -> deletePost())
                                                .setNegativeButton("취소", null).show();
                                    }
                                    return true;
                                });
                                popup.show();
                            });
                        } else {
                            btnMoreOptions.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void deletePost() {
        if (postId == null) return;
        db.collection("posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveComment(String text) {
        if (postId == null) return;
        boolean isAnonymous = cbCommentAnonymous.isChecked();
        Map<String, Object> comment = new HashMap<>();
        comment.put("content", text);
        comment.put("timestamp", FieldValue.serverTimestamp());
        comment.put("isAnonymous", isAnonymous);

        String myUserId = CurrentUserProvider.INSTANCE.userId(this);
        comment.put("authorId", myUserId);

        if (isAnonymous) {
            comment.put("authorName", "익명");
            comment.put("authorProfile", "");
        } else {
            String myName = CurrentUserProvider.INSTANCE.userName(this);
            String myProfile = CurrentUserProvider.INSTANCE.profileImageData(this);
            if (myName == null || myName.trim().isEmpty()) myName = "동네주민";
            comment.put("authorName", myName);
            comment.put("authorProfile", (myProfile != null) ? myProfile : "");
        }

        db.collection("posts").document(postId).collection("comments").add(comment)
                .addOnSuccessListener(doc -> {
                    db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1));
                    etComment.setText("");
                    Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComments() {
        commentsListener = db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) commentAdapter.setComments(value.getDocuments());
                });
    }
}