package com.silpi.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {

    Button btnCurrentLocation, btnMeetingLocation, btnMeetingInfo;
    TextView txtMapResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnMeetingLocation = findViewById(R.id.btnMeetingLocation);
        btnMeetingInfo = findViewById(R.id.btnMeetingInfo);
        txtMapResult = findViewById(R.id.txtMapResult);

        btnCurrentLocation.setOnClickListener(v -> {
            txtMapResult.setText(
                    "현재 위치 확인\n\n" +
                            "현재 위치를 확인했습니다.\n" +
                            "예시 위치: 서울특별시 노원구 삼육대학교 근처"
            );

            Toast.makeText(MapActivity.this, "현재 위치 확인", Toast.LENGTH_SHORT).show();
        });

        btnMeetingLocation.setOnClickListener(v -> {
            txtMapResult.setText(
                    "모임 위치 표시\n\n" +
                            "생성된 모임 위치를 지도에 표시합니다.\n\n" +
                            "예시 모임 위치:\n" +
                            "등산 모임 - 불암산 입구\n" +
                            "식사 모임 - 삼육대학교 정문 근처"
            );

            Toast.makeText(MapActivity.this, "모임 위치 표시", Toast.LENGTH_SHORT).show();
        });

        btnMeetingInfo.setOnClickListener(v -> {
            txtMapResult.setText(
                    "모임 정보 조회\n\n" +
                            "선택한 모임의 정보를 확인합니다.\n\n" +
                            "제목: 등산 모임 - 불암산\n" +
                            "장소: 불암산 입구\n" +
                            "시간: 2026-05-06 14:00\n" +
                            "인원: 1 / 5명"
            );

            Toast.makeText(MapActivity.this, "모임 정보 조회", Toast.LENGTH_SHORT).show();
        });
    }
}
