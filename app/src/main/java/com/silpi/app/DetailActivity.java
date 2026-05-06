package com.silpi.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setTextSize(22);
        textView.setPadding(40, 70, 40, 40);

        String title = getIntent().getStringExtra("title");
        String category = getIntent().getStringExtra("category");
        String people = getIntent().getStringExtra("people");
        String place = getIntent().getStringExtra("place");
        String distance = getIntent().getStringExtra("distance");
        String date = getIntent().getStringExtra("date");
        String time = getIntent().getStringExtra("time");
        String guardian = getIntent().getStringExtra("guardian");
        String desc = getIntent().getStringExtra("desc");

        textView.setText(
                "모임 상세 정보\n\n"
                        + "제목: " + title + "\n"
                        + "카테고리: " + category + "\n"
                        + "모집 인원: " + people + "명\n"
                        + "장소: " + place + "\n"
                        + "거리: " + distance + "km\n"
                        + "날짜: " + date + "\n"
                        + "시간: " + time + "\n"
                        + "보호자 동반: " + guardian + "\n"
                        + "설명: " + desc
        );

        setContentView(textView);
    }
}