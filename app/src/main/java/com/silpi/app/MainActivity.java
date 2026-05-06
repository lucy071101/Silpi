package com.silpi.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Spinner spinnerCategory;
    EditText edtPeople, edtPlace, edtDistance, edtDate, edtTime, edtDesc;
    CheckBox chkGuardian;
    Button btnCreate, btnGoJoin, btnGoMap;
    TextView txtResult;

    String[] categories = {"등산", "식사", "산책", "카페", "바둑", "낚시"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGoMap = findViewById(R.id.btnGoMap);

        btnGoMap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });

        spinnerCategory = findViewById(R.id.spinnerCategory);
        edtPeople = findViewById(R.id.edtPeople);
        edtPlace = findViewById(R.id.edtPlace);
        edtDistance = findViewById(R.id.edtDistance);
        edtDate = findViewById(R.id.edtDate);
        edtTime = findViewById(R.id.edtTime);
        edtDesc = findViewById(R.id.edtDesc);
        chkGuardian = findViewById(R.id.chkGuardian);
        btnCreate = findViewById(R.id.btnCreate);
        txtResult = findViewById(R.id.txtResult);

        btnGoJoin = findViewById(R.id.btnGoJoin);

        btnGoJoin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, joinsilpi.class);
            startActivity(intent);
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String category = spinnerCategory.getSelectedItem().toString();
                String people = edtPeople.getText().toString().trim();
                String place = edtPlace.getText().toString().trim();
                String distance = edtDistance.getText().toString().trim();
                String date = edtDate.getText().toString().trim();
                String time = edtTime.getText().toString().trim();
                String desc = edtDesc.getText().toString().trim();
                String guardian = chkGuardian.isChecked() ? "예" : "아니오";

                if (people.isEmpty() || place.isEmpty() || distance.isEmpty()
                        || date.isEmpty() || time.isEmpty()) {
                    Toast.makeText(MainActivity.this, "필수 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 확인창 띄우기
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("모임 생성")
                        .setMessage("생성하시겠습니까?")

                        // 네 버튼
                        .setPositiveButton("네", (dialog, which) -> {

                            String result = "모임 생성 정보\n\n"
                                    + "카테고리: " + category + "\n"
                                    + "참여 인원: " + people + "명\n"
                                    + "모임 장소: " + place + "\n"
                                    + "거리: " + distance + "km\n"
                                    + "날짜: " + date + "\n"
                                    + "시간: " + time + "\n"
                                    + "보호자 동반: " + guardian + "\n"
                                    + "설명: " + desc;

                            txtResult.setText(result);

                            Toast.makeText(MainActivity.this, "모임 생성 완료", Toast.LENGTH_SHORT).show();

                            String title = category + " 모임 - " + place;

                            String newMeeting =
                                    title + "§" +
                                            category + "§" +
                                            people + "§" +
                                            place + "§" +
                                            distance + "§" +
                                            date + "§" +
                                            time + "§" +
                                            guardian + "§" +
                                            desc;

                            String oldMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                                    .getString("meetings", "");

                            String updatedMeetings;

                            if (oldMeetings.isEmpty()) {
                                updatedMeetings = newMeeting;
                            } else {
                                updatedMeetings = oldMeetings + "¶" + newMeeting;
                            }

                            getSharedPreferences("meeting_data", MODE_PRIVATE)
                                    .edit()
                                    .putString("meetings", updatedMeetings)
                                    .apply();
                        })

                        ///아니오 버튼
                        .setNegativeButton("아니오", null)

                        .show();
            }
        });
    }
}