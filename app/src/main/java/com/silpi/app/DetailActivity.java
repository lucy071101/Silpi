package com.silpi.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    int current;
    int max;
    boolean joined = false;

    TextView title, people, status;
    Button join, cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        title = findViewById(R.id.title);
        people = findViewById(R.id.people);
        status = findViewById(R.id.status);
        join = findViewById(R.id.join);
        cancel = findViewById(R.id.cancel);

        // 데이터 받기
        title.setText(getIntent().getStringExtra("title"));
        current = getIntent().getIntExtra("current", 0);
        max = getIntent().getIntExtra("max", 0);

        updateUI();

        // 참여
        join.setOnClickListener(v -> {
            if (current >= max) {
                Toast.makeText(this, "정원 초과", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!joined) {
                current++;
                joined = true;
                Toast.makeText(this, "참여 완료", Toast.LENGTH_SHORT).show();
            }

            updateUI();
        });

        // 취소
        cancel.setOnClickListener(v -> {
            if (joined) {
                current--;
                joined = false;
                Toast.makeText(this, "참여 취소", Toast.LENGTH_SHORT).show();
            }

            updateUI();
        });
    }

    private void updateUI() {
        people.setText("인원: " + current + " / " + max);

        if (current >= max) {
            status.setText("상태: 모집 마감");
        } else {
            status.setText("상태: 모집중");
        }
    }
}