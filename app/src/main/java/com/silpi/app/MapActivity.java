package com.silpi.app;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    WebView webMap;
    Button btnCurrentLocation, btnMeetingLocation, btnMeetingInfo;
    TextView txtMapResult;

    private static final String KAKAO_JS_KEY = "7cb014f8b57539042d4166b4548557ef";

    private static final double CURRENT_LAT = 37.6425;
    private static final double CURRENT_LNG = 127.1066;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        webMap = findViewById(R.id.webMap);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnMeetingLocation = findViewById(R.id.btnMeetingLocation);
        btnMeetingInfo = findViewById(R.id.btnMeetingInfo);
        txtMapResult = findViewById(R.id.txtMapResult);

        WebSettings settings = webMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webMap.setWebViewClient(new WebViewClient());
        webMap.setWebChromeClient(new WebChromeClient());

        webMap.loadDataWithBaseURL(
                "https://localhost/",
                makeMapHtml(),
                "text/html",
                "UTF-8",
                null
        );

        webMap.postDelayed(this::showDefaultMap, 2500);

        btnCurrentLocation.setOnClickListener(v -> {
            webMap.evaluateJavascript("focusCurrentLocation();", null);
            txtMapResult.setText("현재 위치를 중심으로 이동했습니다.\n기준 위치: 삼육대학교 근처");
            Toast.makeText(this, "현재 위치 확인", Toast.LENGTH_SHORT).show();
        });

        btnMeetingLocation.setOnClickListener(v -> {
            showDefaultMap();
            Toast.makeText(this, "모임 위치 표시", Toast.LENGTH_SHORT).show();
        });

        btnMeetingInfo.setOnClickListener(v -> {
            txtMapResult.setText(getMeetingInfoText());
            Toast.makeText(this, "모임 정보 조회", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDefaultMap() {
        String markersJson = getMeetingMarkersJson();
        webMap.evaluateJavascript("showDefaultMap(" + markersJson + ");", null);
        txtMapResult.setText("현재 위치와 생성된 모든 모임 위치를 표시했습니다.");
    }

    private String getMeetingMarkersJson() {
        String savedMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                .getString("meetings", "");

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        if (savedMeetings != null && savedMeetings.length() > 0) {
            String[] meetings = savedMeetings.split("¶");

            for (String meeting : meetings) {
                String[] info = meeting.split("§", -1);

                if (info.length >= 14) {
                    String title = info[0];
                    String place = info[3];
                    String date = info[5];
                    String time = info[6];

                    double limitKm = parseDoubleSafe(info[4]);
                    double lat = parseDoubleSafe(info[12]);
                    double lng = parseDoubleSafe(info[13]);

                    if (lat == 0 || lng == 0) continue;

                    double distanceKm = calculateDistanceKm(CURRENT_LAT, CURRENT_LNG, lat, lng);
                    String state = distanceKm <= limitKm ? "참여 가능" : "거리 초과";

                    if (!first) json.append(",");

                    json.append("{")
                            .append("\"title\":\"").append(escapeJson(title)).append("\",")
                            .append("\"place\":\"").append(escapeJson(place)).append("\",")
                            .append("\"date\":\"").append(escapeJson(date)).append("\",")
                            .append("\"time\":\"").append(escapeJson(time)).append("\",")
                            .append("\"lat\":").append(lat).append(",")
                            .append("\"lng\":").append(lng).append(",")
                            .append("\"distance\":\"").append(String.format(Locale.US, "%.2f", distanceKm)).append("\",")
                            .append("\"limit\":\"").append(String.format(Locale.US, "%.1f", limitKm)).append("\",")
                            .append("\"state\":\"").append(state).append("\"")
                            .append("}");

                    first = false;
                }
            }
        }

        json.append("]");
        return json.toString();
    }

    private String getMeetingInfoText() {
        String savedMeetings = getSharedPreferences("meeting_data", MODE_PRIVATE)
                .getString("meetings", "");

        if (savedMeetings == null || savedMeetings.length() == 0) {
            return "생성된 모임이 없습니다.";
        }

        StringBuilder result = new StringBuilder("생성된 모임 정보\n\n");
        String[] meetings = savedMeetings.split("¶");
        int count = 0;

        for (String meeting : meetings) {
            String[] info = meeting.split("§", -1);

            if (info.length >= 14) {
                double limitKm = parseDoubleSafe(info[4]);
                double lat = parseDoubleSafe(info[12]);
                double lng = parseDoubleSafe(info[13]);

                if (lat == 0 || lng == 0) continue;

                double distanceKm = calculateDistanceKm(CURRENT_LAT, CURRENT_LNG, lat, lng);
                String state = distanceKm <= limitKm ? "참여 가능" : "거리 초과";

                count++;

                result.append(count).append(". ").append(info[0]).append("\n")
                        .append("장소: ").append(info[3]).append("\n")
                        .append("날짜: ").append(info[5]).append("\n")
                        .append("시간: ").append(info[6]).append("\n")
                        .append("현재 위치와 거리: ").append(String.format(Locale.US, "%.2f", distanceKm)).append("km\n")
                        .append("제한 거리: ").append(limitKm).append("km\n")
                        .append("상태: ").append(state).append("\n\n");
            }
        }

        if (count == 0) {
            return "좌표가 저장된 모임이 없습니다.\n장소 확인 후 새 모임을 생성해주세요.";
        }

        return result.toString();
    }

    private String makeMapHtml() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<script src='https://dapi.kakao.com/v2/maps/sdk.js?appkey=" + KAKAO_JS_KEY + "&autoload=false'></script>" +
                "<style>" +
                "html, body { width:100%; height:100%; margin:0; padding:0; overflow:hidden; }" +
                "#map { width:100%; height:100vh; background:#dff3f0; }" +
                ".redMarker { width:22px; height:22px; background:red; border-radius:50%; border:3px solid white; box-shadow:0 0 6px rgba(0,0,0,0.5); }" +
                ".blueMarker { width:20px; height:20px; background:#1E88E5; border-radius:50%; border:3px solid white; box-shadow:0 0 6px rgba(0,0,0,0.5); }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = null;" +
                "var currentOverlay = null;" +
                "var meetingOverlays = [];" +

                "kakao.maps.load(function() {" +
                "var container = document.getElementById('map');" +
                "var options = { center: new kakao.maps.LatLng(" + CURRENT_LAT + ", " + CURRENT_LNG + "), level: 4 };" +
                "map = new kakao.maps.Map(container, options);" +
                "drawCurrentMarker();" +
                "});" +

                "function drawCurrentMarker() {" +
                "if (!map) return null;" +
                "var pos = new kakao.maps.LatLng(" + CURRENT_LAT + ", " + CURRENT_LNG + ");" +
                "if (currentOverlay != null) currentOverlay.setMap(null);" +
                "currentOverlay = new kakao.maps.CustomOverlay({ position: pos, content: '<div class=\"redMarker\"></div>', xAnchor: 0.5, yAnchor: 0.5 });" +
                "currentOverlay.setMap(map);" +
                "return pos;" +
                "}" +

                "function clearMeetingMarkers() {" +
                "for (var i = 0; i < meetingOverlays.length; i++) meetingOverlays[i].setMap(null);" +
                "meetingOverlays = [];" +
                "}" +

                "function showDefaultMap(meetings) {" +
                "if (!map) return;" +
                "var bounds = new kakao.maps.LatLngBounds();" +
                "var currentPos = drawCurrentMarker();" +
                "bounds.extend(currentPos);" +
                "clearMeetingMarkers();" +

                "if (meetings && meetings.length > 0) {" +
                "for (var i = 0; i < meetings.length; i++) {" +
                "var m = meetings[i];" +
                "var pos = new kakao.maps.LatLng(m.lat, m.lng);" +
                "var overlay = new kakao.maps.CustomOverlay({ position: pos, content: '<div class=\"blueMarker\"></div>', xAnchor: 0.5, yAnchor: 0.5 });" +
                "overlay.setMap(map);" +
                "meetingOverlays.push(overlay);" +
                "bounds.extend(pos);" +
                "}" +
                "map.setBounds(bounds);" +
                "} else {" +
                "map.setCenter(currentPos);" +
                "map.setLevel(4);" +
                "}" +
                "}" +

                "function focusCurrentLocation() {" +
                "if (!map) return;" +
                "var currentPos = drawCurrentMarker();" +
                "map.setCenter(currentPos);" +
                "map.setLevel(4);" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    private double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    private double parseDoubleSafe(String value) {
        try { return Double.parseDouble(value); }
        catch (Exception e) { return 0; }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}