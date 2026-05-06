package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class CommunityListActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageView btnSearch;
    private Button btnBaduk, btnHike, btnWalk, btnFish;

    private RecyclerView rvPopular;
    private CommunityAdapter communityAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_list);

        initViews();
        setupRecyclerView();

        // 🌟 여기가 핵심입니다! 이 줄이 있어야 버튼이 작동합니다.
        setClickListeners();

        loadPopularCommunities();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search_community);
        btnSearch = findViewById(R.id.btn_search);
        btnBaduk = findViewById(R.id.btn_category_baduk);
        btnHike = findViewById(R.id.btn_category_hike);
        btnWalk = findViewById(R.id.btn_category_walk);
        btnFish = findViewById(R.id.btn_category_fish);
        rvPopular = findViewById(R.id.rv_popular_communities);
    }

    private void setupRecyclerView() {
        communityAdapter = new CommunityAdapter();
        rvPopular.setLayoutManager(new LinearLayoutManager(this));
        rvPopular.setAdapter(communityAdapter);
    }

    // 🌟 버튼을 눌렀을 때 어디로 갈지 정해주는 곳!
    // 🌟 이 부분을 찾아서 아래처럼 토스트(Toast) 메시지를 한 줄 추가해 보세요!
    private void setClickListeners() {
        btnBaduk.setOnClickListener(v -> moveToCommunityDetail("바둑"));
        btnHike.setOnClickListener(v -> moveToCommunityDetail("등산"));
        btnWalk.setOnClickListener(v -> moveToCommunityDetail("산책"));
        btnFish.setOnClickListener(v -> moveToCommunityDetail("낚시"));
    }

    // 🌟 화면을 게시판으로 실제로 넘겨주는 마법의 문구!
    private void moveToCommunityDetail(String categoryName) {
        Intent intent = new Intent(CommunityListActivity.this, PostListActivity.class);
        intent.putExtra("communityName", categoryName);
        startActivity(intent);
    }

    private void loadPopularCommunities() {
        db.collection("communities")
                .orderBy("memberCount", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        communityAdapter.setCommunities(value.getDocuments());
                    }
                });
    }
}