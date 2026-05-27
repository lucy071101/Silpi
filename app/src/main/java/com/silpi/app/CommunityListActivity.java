package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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

        setupSearchFilter();
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
            filterCommunities(searchQuery);
        });
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCommunities(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCommunities(String query) {
        String q = query.toLowerCase();

        toggleVisibility(btnExercise, "운동", q);
        toggleVisibility(btnFree, "자유", q);
        toggleVisibility(btnBaduk, "바둑", q);
        toggleVisibility(btnJanggi, "장기", q);
        toggleVisibility(btnHike, "등산", q);
        toggleVisibility(btnTravel, "여행", q);
        toggleVisibility(btnFish, "낚시", q);
        toggleVisibility(btnRead, "독서", q);
    }

    private void toggleVisibility(CardView cardView, String categoryName, String query) {
        if (query.isEmpty() || categoryName.contains(query)) {
            cardView.setVisibility(View.VISIBLE);
        } else {
            cardView.setVisibility(View.GONE);
        }
    }

    private void moveToCommunityDetail(String categoryName) {
        Intent intent = new Intent(CommunityListActivity.this, PostListActivity.class);
        intent.putExtra("communityName", categoryName);
        startActivity(intent);
    }
}