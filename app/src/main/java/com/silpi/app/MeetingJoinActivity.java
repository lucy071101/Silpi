package com.silpi.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MeetingJoinActivity extends AppCompatActivity {

    EditText edtSearch;
    Spinner spinnerFilter;
    ListView listView;
    TextView textMeetingTitle;
    ImageButton buttonMeetingSearch, buttonCloseSearch;

    ArrayList<String[]> allMeetings = new ArrayList<>();
    ArrayList<String[]> filteredMeetings = new ArrayList<>();

    String[] filters = {"전체", "참여중", "등산", "식사", "산책", "카페", "바둑", "낚시"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meeting_join);

        edtSearch = findViewById(R.id.edtSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        listView = findViewById(R.id.listView);
        textMeetingTitle = findViewById(R.id.textMeetingTitle);
        buttonMeetingSearch = findViewById(R.id.buttonMeetingSearch);
        buttonCloseSearch = findViewById(R.id.buttonCloseSearch);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        buttonMeetingSearch.setOnClickListener(v -> {
            textMeetingTitle.setVisibility(View.GONE);
            buttonMeetingSearch.setVisibility(View.GONE);
            edtSearch.setVisibility(View.VISIBLE);
            buttonCloseSearch.setVisibility(View.VISIBLE);
            edtSearch.requestFocus();
        });

        buttonCloseSearch.setOnClickListener(v -> {
            edtSearch.setText("");
            edtSearch.setVisibility(View.GONE);
            buttonCloseSearch.setVisibility(View.GONE);
            textMeetingTitle.setVisibility(View.VISIBLE);
            buttonMeetingSearch.setVisibility(View.VISIBLE);
            applyFilter();
        });

        loadMeetings();
        applyFilter();

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (filteredMeetings.isEmpty()) return;

            String[] info = filteredMeetings.get(position);

            Intent intent = new Intent(MeetingJoinActivity.this, MeetingDetailActivity.class);
            intent.putExtra("index", getOriginalIndex(info));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeetings();
        applyFilter();
    }

    void loadMeetings() {
        allMeetings.clear();

        String savedMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                .getString("meetings", "");

        if (savedMeetings != null && savedMeetings.length() > 0) {
            String[] meetings = savedMeetings.split("¶");

            for (String meeting : meetings) {
                String[] info = meeting.split("§", -1);
                if (info.length >= 9) {
                    allMeetings.add(normalize(info));
                }
            }
        }
    }

    void applyFilter() {
        filteredMeetings.clear();

        String keyword = edtSearch.getText().toString().trim();
        String selectedCategory = spinnerFilter.getSelectedItem().toString();
        String currentUid = getCurrentUid();

        for (String[] info : allMeetings) {
            String title = info[0];
            String category = info[1];
            boolean joined = containsUid(getValue(info, 10, ""), currentUid);

            if (selectedCategory.equals("참여중") && !joined) continue;

            if (!selectedCategory.equals("전체")
                    && !selectedCategory.equals("참여중")
                    && !category.equals(selectedCategory)) continue;

            if (keyword.length() > 0 && !title.contains(keyword) && !category.contains(keyword)) continue;

            if (joined) filteredMeetings.add(0, info);
            else filteredMeetings.add(info);
        }

        listView.setAdapter(new MeetingCardAdapter(filteredMeetings));
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

    int getOriginalIndex(String[] target) {
        for (int i = 0; i < allMeetings.size(); i++) {
            if (allMeetings.get(i) == target) return i;
        }
        return -1;
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

    int parseIntSafe(String value) {
        try { return Integer.parseInt(value); }
        catch (Exception e) { return 0; }
    }

    class MeetingCardAdapter extends BaseAdapter {
        ArrayList<String[]> meetings;

        MeetingCardAdapter(ArrayList<String[]> meetings) {
            this.meetings = meetings;
        }

        @Override public int getCount() { return meetings.size(); }
        @Override public Object getItem(int position) { return meetings.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MeetingJoinActivity.this)
                        .inflate(R.layout.meeting_card, parent, false);
            }

            TextView txtMonth = convertView.findViewById(R.id.txtMonth);
            TextView txtDay = convertView.findViewById(R.id.txtDay);
            TextView txtBadge = convertView.findViewById(R.id.txtBadge);
            TextView txtTitle = convertView.findViewById(R.id.txtTitle);
            TextView txtTime = convertView.findViewById(R.id.txtTime);
            TextView txtPeople = convertView.findViewById(R.id.txtPeople);

            String[] info = meetings.get(position);

            String title = info[0];
            String date = info[5];
            String time = info[6];

            int maxPeople = parseIntSafe(info[2]);
            String joinedUserIds = getValue(info, 10, "");
            int currentPeople = countJoinedUsers(joinedUserIds);
            boolean joined = containsUid(joinedUserIds, getCurrentUid());
            boolean closedByTime = isClosedByDateTime(date, time);

            String month = "";
            String day = "";

            String[] dateParts = date.split("-");
            if (dateParts.length >= 3) {
                month = parseIntSafe(dateParts[1]) + "월";
                day = dateParts[2];
            }

            txtMonth.setText(month);
            txtDay.setText(day);
            txtTitle.setText(title);
            txtTime.setText("시간: " + time);
            txtPeople.setText("인원: " + currentPeople + "/" + maxPeople + "명");

            if (closedByTime) {
                txtBadge.setText("마감");
                txtBadge.setBackgroundColor(Color.parseColor("#9E9E9E"));
            } else if (currentPeople >= maxPeople) {
                txtBadge.setText("마감");
                txtBadge.setBackgroundColor(Color.parseColor("#9E9E9E"));
            } else if (joined) {
                txtBadge.setText("참여중");
                txtBadge.setBackgroundColor(Color.parseColor("#7B68A8"));
            } else {
                txtBadge.setText("모집중");
                txtBadge.setBackgroundColor(Color.parseColor("#34A853"));
            }

            return convertView;
        }
    }
}