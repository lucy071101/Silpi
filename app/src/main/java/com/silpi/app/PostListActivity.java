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
import com.google.firebase.firestore.Query;

public class PostListActivity extends AppCompatActivity {

    private TextView tvBoardTitle;
    private LinearLayout btnNavPostList, btnNavGathering, btnNavChat, btnNavWrite;

    private RecyclerView rvPostList;
    private PostAdapter postAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        initViews();
        setupRecyclerView();
        setupBottomNavigation();

        // 🌟 1. 어떤 게시판(카테고리)인지 먼저 확인합니다.
        String communityName = getIntent().getStringExtra("communityName");
        if (communityName != null) {
            tvBoardTitle.setText(communityName + " 게시판");
        }

        // 🌟 2. 파이어베이스를 부를 때 "이 카테고리만 줘!" 라고 알려줍니다.
        loadPosts(communityName);
    }

    private void initViews() {
        tvBoardTitle = findViewById(R.id.tv_board_title);
        btnNavPostList = findViewById(R.id.btn_nav_post_list);
        btnNavGathering = findViewById(R.id.btn_nav_gathering);
        btnNavChat = findViewById(R.id.btn_nav_chat);
        btnNavWrite = findViewById(R.id.btn_nav_write);
        rvPostList = findViewById(R.id.rv_post_list);

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

    // 🌟 3. 넘어온 카테고리 이름(categoryName)을 받아서 필터링합니다.
    private void loadPosts(String categoryName) {
        if (categoryName == null) categoryName = "바둑"; // 안전장치

        db.collection("posts")
                .whereEqualTo("category", categoryName) // 🌟 핵심! 이름표가 똑같은 글만 골라와라!
                .orderBy("timestamp", Query.Direction.DESCENDING) // 다시 최신순 정렬 켭니다!
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FirebaseError", "에러 발생: " + error.getMessage());
                        Toast.makeText(this, "🚨DB 에러 (Logcat 확인 요망): " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value != null) {
                        postAdapter.setPosts(value.getDocuments());
                    }
                });
    }

    private void setupBottomNavigation() {
        btnNavPostList.setOnClickListener(v -> Toast.makeText(this, "현재 게시글 목록입니다.", Toast.LENGTH_SHORT).show());
        btnNavGathering.setOnClickListener(v -> Toast.makeText(this, "모임 상세 정보 페이지 (준비 중)", Toast.LENGTH_SHORT).show());
        btnNavChat.setOnClickListener(v -> Toast.makeText(this, "채팅방으로 이동 (다른 팀원 파트)", Toast.LENGTH_SHORT).show());

        btnNavWrite.setOnClickListener(v -> {
            Intent intent = new Intent(PostListActivity.this, PostWriteActivity.class);
            startActivity(intent);
        });
    }
}