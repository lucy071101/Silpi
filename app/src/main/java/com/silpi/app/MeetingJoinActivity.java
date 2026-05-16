package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MeetingJoinActivity extends AppCompatActivity {

    ArrayList<String> displayList = new ArrayList<>();
    ArrayList<String[]> meetingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meeting_join);

        ListView listView = findViewById(R.id.listView);

        String savedMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                .getString("meetings", "");

        if (!savedMeetings.isEmpty()) {
            String[] meetings = savedMeetings.split("¶");

            for (String meeting : meetings) {
                String[] info = meeting.split("§", -1);

                if (info.length >= 9) {
                    meetingList.add(info);

                    String title = info[0];
                    String date = info[5];
                    String time = info[6];

                    displayList.add(title + "\n" + date + " " + time);
                }
            }
        }

        if (displayList.isEmpty()) {
            displayList.add("생성된 모임이 없습니다.");
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        displayList
                );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (meetingList.isEmpty()) return;

            String[] info = meetingList.get(position);

            Intent intent = new Intent(MeetingJoinActivity.this, MeetingDetailActivity.class);
            intent.putExtra("title", info[0]);
            intent.putExtra("category", info[1]);
            intent.putExtra("people", info[2]);
            intent.putExtra("place", info[3]);
            intent.putExtra("distance", info[4]);
            intent.putExtra("date", info[5]);
            intent.putExtra("time", info[6]);
            intent.putExtra("guardian", info[7]);
            intent.putExtra("desc", info[8]);

            startActivity(intent);
        });
    }
}
