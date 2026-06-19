# 실피 (Silpi)

> 시니어 사용자가 관심사를 중심으로 소통하고, 모임에 참여할 수 있도록 돕는 Android 커뮤니티 애플리케이션

실피는 커뮤니티, 모임, 지도, 채팅 기능을 하나로 연결한 시니어 대상 커뮤니티 앱입니다. 사용자는 관심 있는 게시글과 모임을 확인하고, 주변 모임에 참여하거나 다른 사용자와 실시간으로 대화할 수 있습니다.

## 주요 기능

### 회원 관리

- 이메일 기반 회원가입 및 로그인
- 비밀번호 재설정
- 자동 로그인 및 로그인 상태 유지
- 로그아웃 및 회원탈퇴
- 프로필 이미지, 닉네임, 상태 메시지 관리
- 관심 취미 카테고리 설정

### 커뮤니티

- 관심 카테고리별 커뮤니티 조회
- 게시글 목록 및 상세 조회
- 게시글 작성 및 삭제
- 사진 첨부 및 익명 작성
- 댓글 작성
- 게시글 좋아요 및 댓글 수 표시

### 모임

- 카테고리별 모임 생성
- 날짜, 시간, 장소 및 참여 인원 설정
- 참여 가능 거리 및 보호자 동반 여부 설정
- 모임 목록 및 상세 정보 조회
- 모임 참여 및 참여 취소
- 모집 상태와 참여 인원 확인

### 지도

- 현재 위치 확인
- 지도 위 모임 위치 표시
- 내 위치와 모임 위치 마커 구분
- 선택한 모임의 요약 정보 표시
- 모임 상세 화면 이동
- 위치 권한 요청 및 처리

### 채팅

- 참여 중인 채팅방 목록 조회
- 최근 메시지 순서로 채팅방 정렬
- 사용자 및 채팅방 검색
- 1:1 및 단체 채팅방 생성
- 실시간 메시지 송수신
- 이미지 및 이모티콘 전송
- 읽지 않은 메시지 개수 표시
- 채팅방 참여자 초대 및 나가기

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Kotlin, Java |
| Platform | Android |
| UI | XML, Material Components, ConstraintLayout, RecyclerView |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Image | Glide |
| Map | Kakao Map API |
| Build | Gradle Kotlin DSL |
| Test | JUnit, AndroidX Test, Espresso |

## 개발 환경

- Android Studio
- JDK 8 이상
- Android SDK 34
- Minimum SDK 24
- Target SDK 34

## 프로젝트 구조

```text
Silpi/
├── app/
│   ├── src/main/
│   │   ├── java/com/silpi/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── HomeActivity.kt
│   │   │   ├── SignupActivity.kt
│   │   │   ├── CommunityListActivity.java
│   │   │   ├── PostListActivity.java
│   │   │   ├── MeetingCreateActivity.java
│   │   │   ├── MeetingJoinActivity.java
│   │   │   ├── MapActivity.java
│   │   │   ├── ChatListActivity.kt
│   │   │   ├── ChatActivity.kt
│   │   │   └── ProfileActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## 실행 방법
1. 저장소 복제
```text
git clone https://github.com/lucy071101/Silpi.git
cd Silpi
```
2. Firebase 설정

Firebase Console에서 Android 프로젝트를 생성합니다.
Android 패키지 이름을 com.silpi.app으로 설정합니다.
Firebase Authentication에서 이메일/비밀번호 로그인을 활성화합니다.
Firestore Database와 Firebase Storage를 생성합니다.
발급받은 google-services.json 파일을 app/ 디렉터리에 추가합니다.
```text
Silpi/app/google-services.json
```
3. Kakao 지도 API 설정

프로젝트 루트의 local.properties 파일에 Kakao JavaScript 키를 추가합니다.
```text
KAKAO_JS_KEY=발급받은_카카오_JavaScript_키
```
API 키와 개인 설정 파일은 공개 저장소에 직접 노출하지 않도록 주의합니다.

4. 앱 실행

Android Studio에서 프로젝트를 열고 Gradle 동기화를 진행한 뒤 에뮬레이터 또는 Android 기기에서 실행합니다.
명령어로 Debug APK를 빌드할 수도 있습니다.
```text
./gradlew assembleDebug
```
생성된 APK는 다음 위치에서 확인할 수 있습니다.
```text
app/build/outputs/apk/debug/app-debug.apk
```
화면 구성
```text
로그인 및 회원가입
        ↓
       홈
 ┌──────┼──────┬──────┬──────┐
커뮤니티  모임    지도    채팅   마이페이지
```
## 팀원 및 담당 기능

| 팀원 | 담당 기능 |
|---|---|
| 이혜민 | 회원가입, 로그인, 계정 복구, 홈 |
| 이원제 | 모임 생성·참여, 지도 |
| 정병혁 | 채팅, 마이페이지 |
| 유준호 | 커뮤니티, 게시글 |
| 오가현 | PM, UI/UX, 프로젝트 문서 및 협업 관리 |

## 향후 개선 사항

- 사용자 접근성을 고려한 글자 크기 및 UI 개선
- 모임 및 채팅 알림 기능 추가
- 커뮤니티 검색 및 추천 기능 개선
- 지도 기반 모임 필터링 고도화
- 단위 테스트 및 UI 테스트 확대
- Firebase 보안 규칙 강화
