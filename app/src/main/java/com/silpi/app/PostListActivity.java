package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class PostListActivity extends AppCompatActivity {

    private TextView tvBoardTitle;

    private LinearLayout btnWrite;

    private RecyclerView rvPostList;
    private PostAdapter postAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration postListListener;

    private String currentCategory = "바둑";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        initViews();
        setupRecyclerView();

        String communityName = getIntent().getStringExtra("communityName");
        if (communityName != null) {
            currentCategory = communityName;
            tvBoardTitle.setText(currentCategory + " 게시판");
        }

        setupWriteButton();

        loadPosts(currentCategory);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postListListener != null) {
            postListListener.remove();
        }
    }

    private void initViews() {
        tvBoardTitle = findViewById(R.id.tv_board_title);
        rvPostList = findViewById(R.id.rv_post_list);

        btnWrite = findViewById(R.id.btn_write);

        android.widget.ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter();
        rvPostList.setLayoutManager(new LinearLayoutManager(this));
        rvPostList.setAdapter(postAdapter);
    }

    private void loadPosts(String categoryName) {
        postListListener = db.collection("posts")
                .whereEqualTo("category", categoryName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirebaseError", "에러 발생: " + error.getMessage());
                        Toast.makeText(this, "🚨DB 에러: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value != null) {
                        postAdapter.setPosts(value.getDocuments());
                    }
                });
    }

    private void setupWriteButton() {
        btnWrite.setOnClickListener(v -> {
            Intent intent = new Intent(PostListActivity.this, PostWriteActivity.class);
            intent.putExtra("category", currentCategory);
            startActivity(intent);
        });
    }
}