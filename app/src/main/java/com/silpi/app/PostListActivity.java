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

    // 🌟 1. 현재 내가 들어와 있는 카테고리 이름을 기억할 저장소 (기본값 "바둑")
    private String currentCategory = "바둑";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        initViews();
        setupRecyclerView();

        // 🌟 2. 이전 화면에서 어떤 게시판을 눌렀는지 확인하고 전역 변수에 저장합니다.
        String communityName = getIntent().getStringExtra("communityName");
        if (communityName != null) {
            currentCategory = communityName;
            tvBoardTitle.setText(currentCategory + " 게시판");
        }

        // 하단 바 네비게이션 세팅 (currentCategory를 사용하기 위해 아래로 이동시켰습니다)
        setupBottomNavigation();

        // 파이어베이스에서 이 카테고리의 글들만 쏙쏙 골라옵니다.
        loadPosts(currentCategory);
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

    private void loadPosts(String categoryName) {
        db.collection("posts")
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

    private void setupBottomNavigation() {
        btnNavPostList.setOnClickListener(v -> Toast.makeText(this, "현재 게시글 목록입니다.", Toast.LENGTH_SHORT).show());
        btnNavGathering.setOnClickListener(v -> Toast.makeText(this, "모임 상세 정보 페이지 (준비 중)", Toast.LENGTH_SHORT).show());
        btnNavChat.setOnClickListener(v -> Toast.makeText(this, "채팅방으로 이동 (다른 팀원 파트)", Toast.LENGTH_SHORT).show());

        btnNavWrite.setOnClickListener(v -> {
            // 🌟 3. 글쓰기 화면을 켤 때, "지금 무슨 게시판인지" 카테고리 정보를 짐 가방에 싸서 보냅니다!
            Intent intent = new Intent(PostListActivity.this, PostWriteActivity.class);
            intent.putExtra("category", currentCategory);
            startActivity(intent);
        });
    }
}