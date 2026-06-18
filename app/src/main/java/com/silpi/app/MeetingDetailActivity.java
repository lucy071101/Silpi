package com.silpi.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MeetingDetailActivity extends AppCompatActivity {

    int index;
    TextView title, people, status;
    Button join, cancel, delete;

    String[] info;
    boolean joinProcessing = false;

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
        joinProcessing = false;

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

        String currentUid = getCurrentUid();
        String joinedUserIds = getValue(info, 10, "");
        boolean joined = containsUid(joinedUserIds, currentUid);
        int currentPeople = countJoinedUsers(joinedUserIds);
        setValue(9, String.valueOf(currentPeople));

        boolean closedByTime = isClosedByDateTime(date, time);

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

        if (closedByTime) {
            join.setEnabled(false);
            join.setText("마감");
        } else if (distanceKm > limitKm) {
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

        if (isCurrentUserCreator()) {
            delete.setVisibility(View.VISIBLE);
            delete.setEnabled(true);
        } else {
            delete.setVisibility(View.GONE);
            delete.setEnabled(false);
        }

        saveMeeting();
    }

    void joinMeeting() {
        if (info == null) return;

        if (joinProcessing) return;
        joinProcessing = true;
        join.setEnabled(false);

        String currentUid = getCurrentUid();

        if (currentUid.length() == 0) {
            Toast.makeText(this, "로그인 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            loadCurrentMeeting();
            showInfo();
            return;
        }

        if (isClosedByDateTime(info[5], info[6])) {
            Toast.makeText(this, "이미 마감된 모임입니다.", Toast.LENGTH_SHORT).show();
            loadCurrentMeeting();
            showInfo();
            return;
        }

        String joinedUserIds = getValue(info, 10, "");

        if (containsUid(joinedUserIds, currentUid)) {
            Toast.makeText(this, "이미 참여한 모임입니다.", Toast.LENGTH_SHORT).show();
            loadCurrentMeeting();
            showInfo();
            return;
        }

        double limitKm = parseDoubleSafe(info[4]);
        double lat = parseDoubleSafe(getValue(info, 12, "0"));
        double lng = parseDoubleSafe(getValue(info, 13, "0"));
        double distanceKm = calculateDistanceKm(CURRENT_LAT, CURRENT_LNG, lat, lng);

        if (distanceKm > limitKm) {
            Toast.makeText(this, "현재 위치에서 참여 가능 거리를 초과했습니다.", Toast.LENGTH_SHORT).show();
            loadCurrentMeeting();
            showInfo();
            return;
        }

        int maxPeople = parseIntSafe(info[2]);
        int currentPeople = countJoinedUsers(joinedUserIds);

        if (currentPeople >= maxPeople) {
            Toast.makeText(this, "정원이 마감되었습니다.", Toast.LENGTH_SHORT).show();
            loadCurrentMeeting();
            showInfo();
            return;
        }

        joinedUserIds = addUid(joinedUserIds, currentUid);

        setValue(10, joinedUserIds);
        setValue(9, String.valueOf(countJoinedUsers(joinedUserIds)));

        saveMeeting();
        Toast.makeText(this, "모임에 참여했습니다.", Toast.LENGTH_SHORT).show();

        loadCurrentMeeting();
        showInfo();
    }

    void cancelMeeting() {
        if (info == null) return;

        String currentUid = getCurrentUid();
        String joinedUserIds = getValue(info, 10, "");

        if (!containsUid(joinedUserIds, currentUid)) {
            Toast.makeText(this, "참여한 모임이 아닙니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        joinedUserIds = removeUid(joinedUserIds, currentUid);

        setValue(10, joinedUserIds);
        setValue(9, String.valueOf(countJoinedUsers(joinedUserIds)));

        saveMeeting();
        Toast.makeText(this, "참여를 취소했습니다.", Toast.LENGTH_SHORT).show();

        loadCurrentMeeting();
        showInfo();
    }

    void deleteMeeting() {
        if (!isCurrentUserCreator()) {
            Toast.makeText(this, "모임 생성자만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String[]> meetings = getMeetings();

        if (index >= 0 && index < meetings.size()) {
            meetings.remove(index);
            saveAllMeetings(meetings);
            Toast.makeText(this, "모임을 삭제했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    boolean isCurrentUserCreator() {
        if (info == null) return false;

        String creatorUid = getValue(info, 14, "");
        String currentUid = getCurrentUid();

        return currentUid.length() > 0 && currentUid.equals(creatorUid);
    }

    String getCurrentUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return "";
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    boolean containsUid(String joinedUserIds, String uid) {
        if (uid == null || uid.length() == 0) return false;
        if (joinedUserIds == null || joinedUserIds.length() == 0) return false;
        if (joinedUserIds.equals("true") || joinedUserIds.equals("false")) return false;

        String[] arr = joinedUserIds.split(",");

        for (String id : arr) {
            if (id.equals(uid)) return true;
        }

        return false;
    }

    String addUid(String joinedUserIds, String uid) {
        if (uid == null || uid.length() == 0) return "";

        if (joinedUserIds == null
                || joinedUserIds.length() == 0
                || joinedUserIds.equals("true")
                || joinedUserIds.equals("false")) {
            return uid;
        }

        if (containsUid(joinedUserIds, uid)) return joinedUserIds;

        return joinedUserIds + "," + uid;
    }

    String removeUid(String joinedUserIds, String uid) {
        if (joinedUserIds == null || joinedUserIds.length() == 0) return "";
        if (joinedUserIds.equals("true") || joinedUserIds.equals("false")) return "";

        StringBuilder sb = new StringBuilder();
        String[] arr = joinedUserIds.split(",");

        for (String id : arr) {
            if (!id.equals(uid)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(id);
            }
        }

        return sb.toString();
    }

    int countJoinedUsers(String joinedUserIds) {
        if (joinedUserIds == null || joinedUserIds.length() == 0) return 0;
        if (joinedUserIds.equals("true") || joinedUserIds.equals("false")) return 0;

        return joinedUserIds.split(",").length;
    }

    boolean isClosedByDateTime(String date, String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
            format.setLenient(false);

            Date meetingDateTime = format.parse(date + " " + time);
            Date now = new Date();

            return meetingDateTime != null && !meetingDateTime.after(now);
        } catch (Exception e) {
            return false;
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

        if (index >= 0 && index < meetings.size() && info != null) {
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
        String[] result = new String[15];

        for (int i = 0; i < result.length; i++) {
            if (i < arr.length) result[i] = arr[i];
            else if (i == 9) result[i] = "0";
            else if (i == 10) result[i] = "";
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