package com.silpi.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MeetingCreateActivity extends AppCompatActivity {

    Spinner spinnerCategory;
    TextView edtPeople;
    EditText edtPlace, edtDistance, edtDate, edtTime, edtDesc;
    CheckBox chkGuardian;
    Button btnCreate, btnCheckPlace, btnMinusPeople, btnPlusPeople;
    TextView txtResult, txtPlaceCheck;
    WebView webPlaceSearch;

    String[] categories = {"등산", "식사", "산책", "카페", "바둑", "낚시"};

    private static final String KAKAO_JS_KEY = BuildConfig.KAKAO_JS_KEY;
    boolean placeChecked = false;
    String checkedPlace = "";
    String placeLat = "";
    String placeLng = "";

    int peopleCount = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meeting_create);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        edtPeople = findViewById(R.id.edtPeople);
        edtPlace = findViewById(R.id.edtPlace);
        edtDistance = findViewById(R.id.edtDistance);
        edtDate = findViewById(R.id.edtDate);
        edtTime = findViewById(R.id.edtTime);
        edtDesc = findViewById(R.id.edtDesc);
        chkGuardian = findViewById(R.id.chkGuardian);
        btnCreate = findViewById(R.id.btnCreate);
        btnCheckPlace = findViewById(R.id.btnCheckPlace);
        btnMinusPeople = findViewById(R.id.btnMinusPeople);
        btnPlusPeople = findViewById(R.id.btnPlusPeople);
        txtResult = findViewById(R.id.txtResult);
        txtPlaceCheck = findViewById(R.id.txtPlaceCheck);
        webPlaceSearch = findViewById(R.id.webPlaceSearch);

        edtPeople.setText(String.valueOf(peopleCount));

        edtDistance.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);

                        if (!Character.isDigit(c) && c != '.') return "";
                        if (c == '.' && dest.toString().contains(".")) return "";
                    }
                    return null;
                }
        });

        btnMinusPeople.setOnClickListener(v -> {
            if (peopleCount <= 2) {
                Toast.makeText(this, "최소 2명부터 설정 가능합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            peopleCount--;
            edtPeople.setText(String.valueOf(peopleCount));
        });

        btnPlusPeople.setOnClickListener(v -> {
            peopleCount++;
            edtPeople.setText(String.valueOf(peopleCount));
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        setupPlaceSearchWebView();

        btnCheckPlace.setOnClickListener(v -> {
            String place = edtPlace.getText().toString().trim();

            if (place.length() == 0) {
                Toast.makeText(this, "모임 장소를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            placeChecked = false;
            checkedPlace = "";
            placeLat = "";
            placeLng = "";

            txtPlaceCheck.setText("장소 확인 중입니다...");

            webPlaceSearch.evaluateJavascript(
                    "searchPlace('" + escapeJs(place) + "');",
                    null
            );
        });

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

                if (people.length() == 0 || place.length() == 0 || distance.length() == 0
                        || date.length() == 0 || time.length() == 0) {
                    Toast.makeText(MeetingCreateActivity.this, "필수 항목을 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (parseIntSafe(people) < 2) {
                    Toast.makeText(MeetingCreateActivity.this, "최소 2명부터 설정 가능합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (parseDoubleSafe(distance) <= 0) {
                    Toast.makeText(MeetingCreateActivity.this, "참여 가능 거리를 올바르게 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isFutureDateTime(date, time)) {
                    Toast.makeText(MeetingCreateActivity.this, "오늘 이후 날짜만 선택 가능합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!placeChecked || !place.equals(checkedPlace)) {
                    Toast.makeText(MeetingCreateActivity.this, "장소 확인을 먼저 해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String result = "모임 생성 정보\n\n"
                        + "카테고리: " + category + "\n"
                        + "참여 인원: " + people + "명\n"
                        + "모임 장소: " + place + "\n"
                        + "거리 제한: " + distance + "km\n"
                        + "날짜: " + date + "\n"
                        + "시간: " + time + "\n"
                        + "보호자 동반: " + guardian + "\n"
                        + "설명: " + desc;

                new androidx.appcompat.app.AlertDialog.Builder(MeetingCreateActivity.this)
                        .setTitle("모임 생성 정보")
                        .setMessage(result)
                        .setPositiveButton("네", (dialog, which) -> {
                            String title = category + " 모임 - " + place;

                            String creatorUid = "";
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                creatorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            }

                            String newMeeting =
                                    title + "§" +
                                            category + "§" +
                                            people + "§" +
                                            place + "§" +
                                            distance + "§" +
                                            date + "§" +
                                            time + "§" +
                                            guardian + "§" +
                                            desc + "§" +
                                            "0" + "§" +
                                            "" + "§" +
                                            "true" + "§" +
                                            placeLat + "§" +
                                            placeLng + "§" +
                                            creatorUid;

                            String oldMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                                    .getString("meetings", "");

                            String updatedMeetings;

                            if (oldMeetings == null || oldMeetings.length() == 0) {
                                updatedMeetings = newMeeting;
                            } else {
                                updatedMeetings = oldMeetings + "¶" + newMeeting;
                            }

                            getSharedPreferences("meeting_data", MODE_PRIVATE)
                                    .edit()
                                    .putString("meetings", updatedMeetings)
                                    .apply();

                            Intent intent = new Intent(MeetingCreateActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            }
        });
    }

    private boolean isFutureDateTime(String date, String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
            format.setLenient(false);

            Date selectedDateTime = format.parse(date + " " + time);
            Date now = new Date();

            return selectedDateTime != null && selectedDateTime.after(now);
        } catch (Exception e) {
            Toast.makeText(this, "날짜와 시간을 예시 형식에 맞게 입력하세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void setupPlaceSearchWebView() {
        WebSettings settings = webPlaceSearch.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webPlaceSearch.setWebViewClient(new WebViewClient());
        webPlaceSearch.setWebChromeClient(new WebChromeClient());
        webPlaceSearch.addJavascriptInterface(new PlaceBridge(), "Android");

        webPlaceSearch.loadDataWithBaseURL(
                "https://localhost/",
                makePlaceSearchHtml(),
                "text/html",
                "UTF-8",
                null
        );
    }

    private String makePlaceSearchHtml() {
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<script src='https://dapi.kakao.com/v2/maps/sdk.js?appkey=" + KAKAO_JS_KEY + "&libraries=services&autoload=false'></script>" +
                "</head><body><script>" +
                "var ps = null;" +
                "var ready = false;" +
                "kakao.maps.load(function() {" +
                "ps = new kakao.maps.services.Places();" +
                "ready = true;" +
                "Android.onSearchReady();" +
                "});" +
                "function searchPlace(keyword) {" +
                "if (!ready || ps == null) {" +
                "setTimeout(function(){ searchPlace(keyword); }, 500);" +
                "return;" +
                "}" +
                "ps.keywordSearch(keyword, function(data, status) {" +
                "if (status === kakao.maps.services.Status.OK && data.length > 0) {" +
                "Android.onPlaceFound(keyword, data[0].y, data[0].x, data[0].place_name);" +
                "} else {" +
                "Android.onPlaceNotFound('장소를 찾을 수 없습니다. 더 정확한 장소명으로 입력해주세요.');" +
                "}" +
                "});" +
                "}" +
                "</script></body></html>";
    }

    private String escapeJs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("'", "\\'");
    }

    public class PlaceBridge {

        @JavascriptInterface
        public void onSearchReady() {
            runOnUiThread(() -> txtPlaceCheck.setText("장소 검색 준비 완료"));
        }

        @JavascriptInterface
        public void onPlaceFound(String inputPlace, String lat, String lng, String foundName) {
            runOnUiThread(() -> {
                placeChecked = true;
                checkedPlace = inputPlace;
                placeLat = lat;
                placeLng = lng;

                txtPlaceCheck.setText(
                        "장소 확인 완료\n"
                                + "검색 장소: " + foundName + "\n"
                                + "위도: " + lat + "\n"
                                + "경도: " + lng
                );
            });
        }

        @JavascriptInterface
        public void onPlaceNotFound(String message) {
            runOnUiThread(() -> {
                placeChecked = false;
                checkedPlace = "";
                placeLat = "";
                placeLng = "";

                txtPlaceCheck.setText(message);
            });
        }
    }
}