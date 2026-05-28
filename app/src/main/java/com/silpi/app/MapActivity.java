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
    Button btnCurrentLocation, btnMeetingLocation, btnJoinedMeetingLocation, btnMeetingInfo;
    TextView txtMapResult;

    private static final String KAKAO_JS_KEY = BuildConfig.KAKAO_JS_KEY;
    private static final double CURRENT_LAT = 37.6425;
    private static final double CURRENT_LNG = 127.1066;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        webMap = findViewById(R.id.webMap);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnMeetingLocation = findViewById(R.id.btnMeetingLocation);
        btnJoinedMeetingLocation = findViewById(R.id.btnJoinedMeetingLocation);
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

        webMap.postDelayed(this::showAllMeetings, 2500);

        btnCurrentLocation.setOnClickListener(v -> {
            webMap.evaluateJavascript("focusCurrentLocation();", null);
            txtMapResult.setText("현재 위치를 중심으로 이동했습니다.\n기준 위치: 삼육대학교 근처");
            Toast.makeText(this, "현재 위치 확인", Toast.LENGTH_SHORT).show();
        });

        btnMeetingLocation.setOnClickListener(v -> {
            showAllMeetings();
            Toast.makeText(this, "전체 모임 표시", Toast.LENGTH_SHORT).show();
        });

        btnJoinedMeetingLocation.setOnClickListener(v -> {
            showJoinedMeetings();
            Toast.makeText(this, "참여중 모임 표시", Toast.LENGTH_SHORT).show();
        });

        btnMeetingInfo.setOnClickListener(v -> {
            txtMapResult.setText(getMeetingInfoText());
            Toast.makeText(this, "모임 정보 조회", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAllMeetings() {
        String markersJson = getMeetingMarkersJson(false);
        webMap.evaluateJavascript("showMeetings(" + markersJson + ");", null);
        txtMapResult.setText("전체 모임 위치를 표시했습니다.\n파란색: 전체 모임 / 초록색: 참여중 모임 / 빨간색: 현재 위치");
    }

    private void showJoinedMeetings() {
        String markersJson = getMeetingMarkersJson(true);
        webMap.evaluateJavascript("showMeetings(" + markersJson + ");", null);
        txtMapResult.setText("참여중인 모임만 표시했습니다.\n초록색: 참여중 모임 / 빨간색: 현재 위치");
    }

    private String getMeetingMarkersJson(boolean onlyJoined) {
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
                    boolean joined = Boolean.parseBoolean(getValue(info, 10, "false"));
                    double lat = parseDoubleSafe(info[12]);
                    double lng = parseDoubleSafe(info[13]);

                    if (lat == 0 || lng == 0) continue;
                    if (onlyJoined && !joined) continue;

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
                            .append("\"state\":\"").append(state).append("\",")
                            .append("\"joined\":").append(joined)
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

        StringBuilder joinedResult = new StringBuilder();
        StringBuilder normalResult = new StringBuilder();

        String[] meetings = savedMeetings.split("¶");
        int count = 0;
        int joinedCount = 0;

        for (String meeting : meetings) {
            String[] info = meeting.split("§", -1);

            if (info.length >= 14) {
                double limitKm = parseDoubleSafe(info[4]);
                boolean joined = Boolean.parseBoolean(getValue(info, 10, "false"));
                double lat = parseDoubleSafe(info[12]);
                double lng = parseDoubleSafe(info[13]);

                if (lat == 0 || lng == 0) continue;

                double distanceKm = calculateDistanceKm(CURRENT_LAT, CURRENT_LNG, lat, lng);
                String state = distanceKm <= limitKm ? "참여 가능" : "거리 초과";

                StringBuilder item = new StringBuilder();

                if (joined) {
                    joinedCount++;
                    item.append("★ [참여중] ");
                } else {
                    count++;
                    item.append(count).append(". ");
                }

                item.append(info[0]).append("\n")
                        .append("장소: ").append(info[3]).append("\n")
                        .append("시간: ").append(info[5]).append(" ").append(info[6]).append("\n")
                        .append("거리: ").append(String.format(Locale.US, "%.2f", distanceKm)).append("km")
                        .append(" / 제한: ").append(String.format(Locale.US, "%.1f", limitKm)).append("km\n")
                        .append("상태: ").append(state).append("\n\n");

                if (joined) joinedResult.append(item);
                else normalResult.append(item);
            }
        }

        if (joinedResult.length() == 0 && normalResult.length() == 0) {
            return "좌표가 저장된 모임이 없습니다.\n장소 확인 후 새 모임을 생성해주세요.";
        }

        return "모임 정보\n\n" + joinedResult + normalResult;
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
                ".redMarker { width:24px; height:24px; background:#E53935; border-radius:50%; border:4px solid white; box-shadow:0 0 7px rgba(0,0,0,0.45); }" +
                ".blueMarker { width:22px; height:22px; background:#1E88E5; border-radius:50%; border:3px solid white; box-shadow:0 0 7px rgba(0,0,0,0.45); cursor:pointer; }" +
                ".greenMarker { width:22px; height:22px; background:#2E7D32; border-radius:50%; border:3px solid white; box-shadow:0 0 7px rgba(0,0,0,0.45); cursor:pointer; }" +
                ".bubble { min-width:150px; max-width:210px; background:white; border-radius:12px; padding:10px 12px; box-shadow:0 2px 8px rgba(0,0,0,0.25); font-family:sans-serif; }" +
                ".bubbleTitle { font-size:15px; font-weight:bold; color:#222; margin-bottom:5px; line-height:1.25; }" +
                ".bubbleJoined { font-size:13px; font-weight:bold; color:#2E7D32; margin-bottom:4px; }" +
                ".bubbleText { font-size:13px; color:#444; line-height:1.35; }" +
                ".bubbleState { font-size:13px; font-weight:bold; margin-top:5px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = null;" +
                "var currentOverlay = null;" +
                "var meetingOverlays = [];" +
                "var infoOverlay = null;" +
                "var meetingData = [];" +

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
                "closeBubble();" +
                "}" +

                "function closeBubble() {" +
                "if (infoOverlay != null) {" +
                "infoOverlay.setMap(null);" +
                "infoOverlay = null;" +
                "}" +
                "}" +

                "function showMeetings(meetings) {" +
                "if (!map) return;" +
                "meetingData = meetings || [];" +
                "var bounds = new kakao.maps.LatLngBounds();" +
                "var currentPos = drawCurrentMarker();" +
                "bounds.extend(currentPos);" +
                "clearMeetingMarkers();" +

                "if (meetingData.length > 0) {" +
                "for (var i = 0; i < meetingData.length; i++) {" +
                "var m = meetingData[i];" +
                "var pos = new kakao.maps.LatLng(m.lat, m.lng);" +
                "var markerClass = m.joined ? 'greenMarker' : 'blueMarker';" +
                "var content = '<div class=\"' + markerClass + '\" onclick=\"showBubble(' + i + ')\"></div>';" +
                "var overlay = new kakao.maps.CustomOverlay({ position: pos, content: content, xAnchor: 0.5, yAnchor: 0.5 });" +
                "overlay.setMap(map);" +
                "meetingOverlays.push(overlay);" +
                "bounds.extend(pos);" +
                "}" +
                "map.setBounds(bounds);" +
                "kakao.maps.event.addListener(map, 'click', function() {" +
                "closeBubble();" +
                "});" +
                "} else {" +
                "map.setCenter(currentPos);" +
                "map.setLevel(4);" +
                "}" +
                "}" +

                "function showBubble(index) {" +
                "var m = meetingData[index];" +
                "if (!m) return;" +
                "closeBubble();" +
                "var pos = new kakao.maps.LatLng(m.lat, m.lng);" +
                "var joinedHtml = m.joined ? '<div class=\"bubbleJoined\">참여중</div>' : '';" +
                "var stateColor = m.state === '참여 가능' ? '#2E7D32' : '#D32F2F';" +
                "var html = '<div class=\"bubble\">' +" +
                "joinedHtml +" +
                "'<div class=\"bubbleTitle\">' + escapeHtml(m.title) + '</div>' +" +
                "'<div class=\"bubbleText\">' + escapeHtml(m.place) + '</div>' +" +
                "'<div class=\"bubbleText\">' + escapeHtml(m.date) + ' ' + escapeHtml(m.time) + '</div>' +" +
                "'<div class=\"bubbleState\" style=\"color:' + stateColor + '\">' + m.distance + 'km · ' + escapeHtml(m.state) + '</div>' +" +
                "'</div>';" +
                "infoOverlay = new kakao.maps.CustomOverlay({ position: pos, content: html, xAnchor: 0.5, yAnchor: 1.25 });" +
                "infoOverlay.setMap(map);" +
                "map.panTo(pos);" +
                "}" +

                "function escapeHtml(text) {" +
                "if (!text) return '';" +
                "return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\\\"/g, '&quot;').replace(/'/g, '&#039;');" +
                "}" +

                "function focusCurrentLocation() {" +
                "if (!map) return;" +
                "closeBubble();" +
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
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getValue(String[] arr, int index, String defaultValue) {
        if (arr.length > index && arr[index] != null) return arr[index];
        return defaultValue;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}