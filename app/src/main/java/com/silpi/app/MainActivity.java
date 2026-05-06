package com.silpi.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 테스트 사용자 선택 화면으로 이동
        Intent intent = new Intent(this, DevTestLoginActivity.class);
        startActivity(intent);

        finish();
    }
}
