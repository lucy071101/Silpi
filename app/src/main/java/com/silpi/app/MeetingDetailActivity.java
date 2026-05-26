package com.silpi.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MeetingDetailActivity extends AppCompatActivity {

    int index;
    TextView title, people, status;
    Button join, cancel, delete;

    String[] info;

    private static final double CURRENT_LAT = 37.6425;
    private static final double CURRENT_LNG = 127.1066;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meeting_detail);

        title = findViewById(R.id.title);
        people = findViewById(R.id.people);
        status = findViewById(R.id.status);
        join = findViewById(R.id.join);
        cancel = findViewById(R.id.cancel);
        delete = findViewById(R.id.delete);

        index = getIntent().getIntExtra("index", -1);

        loadCurrentMeeting();
        showInfo();

        join.setOnClickListener(v -> joinMeeting());
        cancel.setOnClickListener(v -> cancelMeeting());
        delete.setOnClickListener(v -> deleteMeeting());
    }

    void loadCurrentMeeting() {
        ArrayList<String[]> meetings = getMeetings();

        if (index >= 0 && index < meetings.size()) {
            info = meetings.get(index);
        }
    }

    void showInfo() {
        if (info == null) return;

        String meetingTitle = info[0];
        String category = info[1];
        int maxPeople = parseIntSafe(info[2]);
        String place = info[3];
        String distance = info[4];
        String date = info[5];
        String time = info[6];
        String guardian = info[7];
        String desc = info[8];

        int currentPeople = parseIntSafe(getValue(info, 9, "0"));
        boolean joined = Boolean.parseBoolean(getValue(info, 10, "false"));
        boolean owner = Boolean.parseBoolean(getValue(info, 11, "true"));

        double limitKm = parseDoubleSafe(distance);
        double lat = parseDoubleSafe(getValue(info, 12, "0"));
        double lng = parseDoubleSafe(getValue(info, 13, "0"));
        double distanceKm = calculateDistanceKm(CURRENT_LAT, CURRENT_LNG, lat, lng);

        title.setText(meetingTitle);
        people.setText("인원: " + currentPeople + "/" + maxPeople + "명");

        status.setText(
                "카테고리: " + category + "\n"
                        + "장소: " + place + "\n"
                        + "거리 제한: " + distance + "km\n"
                        + "현재 위치와 거리: " + String.format("%.2f", distanceKm) + "km\n"
                        + "날짜: " + date + "\n"
                        + "시간: " + time + "\n"
                        + "보호자 동반: " + guardian + "\n"
                        + "설명: " + desc
        );

        if (distanceKm > limitKm) {
            join.setEnabled(false);
            join.setText("거리 초과");
        } else if (currentPeople >= maxPeople && !joined) {
            join.setEnabled(false);
            join.setText("정원 마감");
        } else if (joined) {
            join.setEnabled(false);
            join.setText("참여 완료");
        } else {
            join.setEnabled(true);
            join.setText("모임 참여");
        }

        cancel.setEnabled(joined);
        delete.setEnabled(owner);
    }

    void joinMeeting() {
        if (info == null) return;

        double limitKm = parseDoubleSafe(info[4]);
        double lat = parseDoubleSafe(getValue(info, 12, "0"));
        double lng = parseDoubleSafe(getValue(info, 13, "0"));
        double distanceKm = calculateDistanceKm(CURRENT_LAT, CURRENT_LNG, lat, lng);

        if (distanceKm > limitKm) {
            Toast.makeText(this, "현재 위치에서 참여 가능 거리를 초과했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxPeople = parseIntSafe(info[2]);
        int currentPeople = parseIntSafe(getValue(info, 9, "0"));

        if (currentPeople >= maxPeople) {
            Toast.makeText(this, "정원이 마감되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        setValue(9, String.valueOf(currentPeople + 1));
        setValue(10, "true");

        saveMeeting();
        Toast.makeText(this, "모임에 참여했습니다.", Toast.LENGTH_SHORT).show();

        loadCurrentMeeting();
        showInfo();
    }

    void cancelMeeting() {
        if (info == null) return;

        int currentPeople = parseIntSafe(getValue(info, 9, "0"));

        if (currentPeople > 0) {
            setValue(9, String.valueOf(currentPeople - 1));
        }

        setValue(10, "false");

        saveMeeting();
        Toast.makeText(this, "참여를 취소했습니다.", Toast.LENGTH_SHORT).show();

        loadCurrentMeeting();
        showInfo();
    }

    void deleteMeeting() {
        ArrayList<String[]> meetings = getMeetings();

        if (index >= 0 && index < meetings.size()) {
            meetings.remove(index);
            saveAllMeetings(meetings);
            Toast.makeText(this, "모임을 삭제했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    ArrayList<String[]> getMeetings() {
        ArrayList<String[]> meetings = new ArrayList<>();

        String savedMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                .getString("meetings", "");

        if (savedMeetings != null && savedMeetings.length() > 0) {
            String[] meetingArr = savedMeetings.split("¶");

            for (String meeting : meetingArr) {
                String[] arr = meeting.split("§", -1);

                if (arr.length >= 9) {
                    meetings.add(normalize(arr));
                }
            }
        }

        return meetings;
    }

    void saveMeeting() {
        ArrayList<String[]> meetings = getMeetings();

        if (index >= 0 && index < meetings.size()) {
            meetings.set(index, info);
            saveAllMeetings(meetings);
        }
    }

    void saveAllMeetings(ArrayList<String[]> meetings) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < meetings.size(); i++) {
            if (i > 0) sb.append("¶");
            sb.append(String.join("§", meetings.get(i)));
        }

        getSharedPreferences("meeting_data", MODE_PRIVATE)
                .edit()
                .putString("meetings", sb.toString())
                .apply();
    }

    String[] normalize(String[] arr) {
        String[] result = new String[14];

        for (int i = 0; i < result.length; i++) {
            if (i < arr.length) result[i] = arr[i];
            else if (i == 9) result[i] = "0";
            else if (i == 10) result[i] = "false";
            else if (i == 11) result[i] = "true";
            else result[i] = "";
        }

        return result;
    }

    String getValue(String[] arr, int index, String defaultValue) {
        if (arr.length > index && arr[index] != null) return arr[index];
        return defaultValue;
    }

    void setValue(int index, String value) {
        info[index] = value;
    }

    int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2)
                        * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}