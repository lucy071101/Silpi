package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    String[] meetings = {
            "등산",
            "식사",
            "산책",
            "카페",
            "바둑",
            "낚시"
    };
    int[] currentList = {3, 5, 2};
    int[] maxList = {5, 5, 4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.listView);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1,
                        meetings);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("title", meetings[position]);
            intent.putExtra("current", currentList[position]);
            intent.putExtra("max", maxList[position]);
            startActivity(intent);
        });
    }
}