package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class CommunityListActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageView btnSearch;
    private CardView btnExercise, btnFree, btnBaduk, btnJanggi, btnHike, btnTravel, btnFish, btnRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_list);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search_community);
        btnSearch = findViewById(R.id.btn_search);
        btnExercise = findViewById(R.id.btn_category_exercise);
        btnFree = findViewById(R.id.btn_category_free);
        btnBaduk = findViewById(R.id.btn_category_baduk);
        btnJanggi = findViewById(R.id.btn_category_janggi);
        btnHike = findViewById(R.id.btn_category_hike);
        btnTravel = findViewById(R.id.btn_category_travel);
        btnFish = findViewById(R.id.btn_category_fish);
        btnRead = findViewById(R.id.btn_category_read);
    }

    private void setClickListeners() {
        btnExercise.setOnClickListener(v -> moveToCommunityDetail("운동"));
        btnFree.setOnClickListener(v -> moveToCommunityDetail("자유"));
        btnBaduk.setOnClickListener(v -> moveToCommunityDetail("바둑"));
        btnJanggi.setOnClickListener(v -> moveToCommunityDetail("장기"));
        btnHike.setOnClickListener(v -> moveToCommunityDetail("등산"));
        btnTravel.setOnClickListener(v -> moveToCommunityDetail("여행"));
        btnFish.setOnClickListener(v -> moveToCommunityDetail("낚시"));
        btnRead.setOnClickListener(v -> moveToCommunityDetail("독서"));

        btnSearch.setOnClickListener(v -> {
            String searchQuery = etSearch.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                Toast.makeText(CommunityListActivity.this, searchQuery + " 검색", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveToCommunityDetail(String categoryName) {
        Intent intent = new Intent(CommunityListActivity.this, PostListActivity.class);
        intent.putExtra("communityName", categoryName);
        startActivity(intent);
    }
}