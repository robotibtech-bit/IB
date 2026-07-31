# temi Robot App Development Roadmap

## [x] Phase 1: SDK Integration & Setup
- [x] Add temi SDK dependency in `app/build.gradle.kts`.
  - `gradle/libs.versions.toml` 에 `temiSdk = "1.137.1"` / `temi-sdk` 라이브러리 별칭 추가.
  - `AndroidManifest.xml` 에 `com.robotemi.sdk.metadata.SKILL` meta-data 추가 (Launcher 스킬 인식용).
- [x] Create `TemiRepository.kt` to encapsulate temi `Robot` instance and listeners.
  - `data/temi/TemiRepository.kt` — `Robot` 인스턴스 캡슐화, 리스너 등록/해제 대칭 관리,
    SDK 콜백을 `StateFlow` 단일 상태로 정규화.
  - `data/temi/TemiState.kt` — `TemiConnectionState`, `NavigationState` (SDK 가이드 17장 권장 상태 머신).
  - `MainActivity` 의 `onStart` / `onStop` 에 저장소 생명주기 연결.

## [x] Phase 2: Navigation & Voice Features
- [x] Implement ViewModel for handling Robot speech (`speak`) and movement (`goTo`).
  - `ui/main/TemiViewModel.kt` — `goTo` / `stopMovement` / `speak` / `cancelSpeech` /
    `refreshLocations` 명령과 상태 콜백 타임아웃 폴백.
- [x] Add status flow to track whether the robot is currently moving or standing by.
  - `ui/main/TemiUiState.kt` — 연결·이동·발화·POI 를 하나로 합친 `TemiUiState`,
    상태 배지용 `RobotActivity` (UNAVAILABLE / CONNECTING / STANDBY / MOVING / SPEAKING).
  - `TemiRepository` 에 `Robot.TtsListener` 추가 → `speechState` 흐름.
- [x] temi 지도 POI(위치) 목록 조회.
  - `robot.locations` + `OnLocationsUpdatedListener` → `TemiRepository.locations` StateFlow.
  - `onRobotReady` 시 자동 조회, 관리자가 위치를 바꾸면 리스너로 갱신, 수동 `refreshLocations()` 제공.
- [x] 검증: `:app:testDebugUnitTest` 11개 통과 (`TemiUiStateTest`), `:app:assembleDebug` 성공.

## [x] Phase 3: Compose UI Design
- [x] Create a main dashboard screen showing temi's current status (Location, Battery/Connection, Current Action).
  - `ui/main/MainDashboardScreen.kt` — 상태 카드(연결/배터리/목적지/상태 배지),
    이동 중 `LinearProgressIndicator`, POI 목록, 명령 버튼.
  - `TemiRepository` 에 `OnBatteryStatusChangedListener` 추가 → `batteryStatus` 흐름.
- [x] Add interactive buttons: "Move to Office", "Move to Lobby", "Speak Welcome Message".
  - 지도에 없는 POI 는 버튼을 비활성화하고 이유를 표시한다.
  - POI 목록의 각 항목도 탭하면 그 위치로 이동한다.
- [x] 런타임 권한 요청.
  - `AndroidManifest.xml` 에 `com.robotemi.permission.map` 선언.
  - `TemiRepository` 에 `OnRequestPermissionResultListener` + `checkSelfPermission` /
    `requestPermissions` → `permissionStatus` 흐름.
  - 준비 완료 후 1회 자동 요청, 이후에는 화면의 '권한 허용' 버튼으로만 재요청.
  - `onResume` 마다 승인 상태를 재조회 (설정에서 바꾸고 돌아온 경우 대비).
- [x] `MainActivity` 연결 완료 — `viewModels()` + `collectAsStateWithLifecycle()`.
- [x] 검증: `:app:testDebugUnitTest` 18개 통과, `assembleDebug` 성공.

## [x] Phase 4: Testing & Error Handling
- [x] Add fallback toast messages or UI alerts when navigation fails or destination is unreachable.
  - `ui/main/UiAlert.kt` — 안내 종류(도착·이동실패·도달불가·미준비·권한필요·권한거부·발화실패·SDK오류)와
    `strings.xml` 매핑.
  - `MainActivity` 의 `Scaffold(snackbarHost = …)` 로 스낵바 표시. 새 안내가 오면 이전 것을 즉시 밀어낸다.
  - `TemiViewModel.alerts` (`SharedFlow`) — 상태가 아니라 1회성 사건으로 다룬다.
- [x] SDK 오류 처리: `OnSdkExceptionListener` 추가. `CODE_PERMISSION_DENIED` 수신 시 승인 현황을 재조회한다.
- [x] 테스트: `TemiController` 인터페이스 분리 + `FakeTemiController` 로 ViewModel 을 JVM 에서 검증.
  - `TemiViewModelTest` 17개 — 명령 거절 사유, 결과 안내, 중복 안내 방지, 타임아웃 폴백, 권한 흐름.
  - `TemiUiStateTest` 18개 — 상태 파생 규칙.
- [x] 검증: `:app:testDebugUnitTest` **36개 전부 통과**, `clean assembleDebug` 성공.

---

## Phase 1 구현 노트

### 버전 기준
- temi SDK: **1.137.1** (mavenCentral 에서 정상 해석 확인).
  문서 기준 최신은 1.138.0 이지만, 가이드 15장 권장에 따라 검증 기준선인 1.137.1 로 시작한다.
  1.138.0 은 로봇 Launcher 를 138 호환 빌드로 맞춘 뒤 별도 브랜치에서 회귀 테스트한다.
- SDK AAR 버전과 각 로봇의 Launcher 빌드 번호를 항상 한 쌍으로 기록한다.

### 알려진 SDK 특이사항
- 리스너 해제 메서드명이 비대칭이다: `addOnLocationsUpdatedListener` ↔ `removeOnLocationsUpdateListener`.
- 이동 상태 문자열(`start` / `calculating` / `going` / `complete` / `abort`)은 버전별 상수명 변경
  영향을 피하려고 `TemiRepository` 에 직접 상수로 정의했다.

### 대상 기기 (확정)
- 대상은 **temi V3 (Android 11 / API 30)** 전용이다. V2 는 지원 대상이 아니다.
  따라서 `minSdk = 24` 를 그대로 유지한다.
- V3 기준이므로 API 30 이상에서만 제공되는 기능·정책(스코프드 스토리지 등)을 전제로 구현해도 된다.

### Phase 2 설계 결정
- **버튼은 상태를 확정하지 않는다.** `goTo` 호출 성공 ≠ 주행 시작이므로, UI 는 명령만 보내고
  상태 전환은 `OnGoToLocationStatusChangedListener` 콜백에서만 일어난다.
- **타임아웃 폴백.** 콜백이 누락돼도 UI 가 영구 '이동 중' 에 남지 않도록,
  `goTo` 후 10초, `stopMovement` 후 5초 안에 콜백이 없으면 상태를 정리한다.
- **중지와 실패를 구분한다.** `abort` 수신 시 사용자가 직접 멈췄으면 `USER_STOPPED`,
  아니면 조이스틱 개입 등으로 보고 `EXTERNAL_INTERRUPTION` 으로 표시한다.
- **문구는 데이터 계층에 두지 않는다.** 사유는 `NavigationIssue` enum 으로만 전달하고,
  표시 문구는 Phase 3 에서 `strings.xml` 로 매핑한다.
- **TTS 상태는 요청 ID 로 필터링한다.** 다른 화면·다른 앱의 발화 상태가 섞이지 않게 한다.

### Phase 3 설계 결정
- **temi 권한은 일반 Android 권한이 아니다.** `ActivityResultContracts.RequestPermission` 이 아니라
  `robot.requestPermissions()` 와 `OnRequestPermissionResultListener` 로 처리해야 한다.
  Manifest 선언(`com.robotemi.permission.map`)만으로는 부족하고 실행 중 사용자 승인이 필요하다.
- **다이얼로그 표시 ≠ 승인.** `granted` 는 `checkSelfPermission` 결과와 승인 콜백으로만 갱신한다.
- **자동 요청은 1회.** 거부한 사용자에게 반복해서 묻지 않고, 이후에는 화면 버튼으로만 재요청한다.
- **지도에 없는 POI 버튼은 비활성화.** `strings.xml` 의 위치 이름이 현장 지도와 다르면
  눌러도 실패하므로, 미리 막고 이유를 표시한다.
- **중첩 스크롤 회피.** POI 목록은 바깥 `Column` 이 스크롤을 담당하므로 `LazyColumn` 을 쓰지 않는다.

### Phase 4 설계 결정
- **상태와 사건을 나눈다.** 지속되는 상황(권한 없음 → 카드·버튼 비활성)은 `uiState` 로,
  한 번만 알리면 되는 것(도착·이동 실패)은 `alerts` 로 보낸다.
  결과를 안내한 즉시 이동 상태를 `Idle` 로 되돌려 같은 안내가 반복되지 않게 한다.
- **Toast 대신 Snackbar.** 로드맵의 "toast messages or UI alerts" 중 Compose 에 맞고
  화면 안에서 일관되게 보이는 Snackbar 를 택했다.
- **거절 사유를 구분한다.** 버튼이 비활성이어도 POI 목록 탭 등 다른 경로로 들어올 수 있으므로
  `goTo` 안에서 미준비 / 권한없음 / 지도에 없음 / 이동중을 다시 검사하고 각각 다르게 안내한다.
  '이미 이동 중'은 오류가 아니므로 안내하지 않는다.
- **`SharingStarted.Eagerly`.** ViewModel 이 명령을 거를 때 `uiState.value` 를 직접 읽으므로
  화면 구독 여부와 무관하게 항상 최신이어야 한다.
- **`TemiController` 인터페이스 분리.** `Robot.getInstance()` 가 실기에서만 동작해
  ViewModel 을 JVM 단위 테스트로 검증하려면 대역이 필요했다.

### 실기 테스트 수정 (권한 팝업이 뜨지 않던 문제)
- **증상:** 앱 실행 시 지도 권한 요청 팝업이 표시되지 않음.
- **원인:** temi 권한을 `<uses-permission android:name="com.robotemi.permission.map" />` 로 선언했다.
  temi 권한은 일반 Android 권한이 아니라서 Launcher 가 이 선언을 읽지 않는다.
  `Robot.requestPermissions()` 는 `applicationInfo.metaData` 의
  `com.robotemi.sdk.metadata.PERMISSIONS` 문자열만 확인하며, 값이 비어 있으면
  `"There is no valid permission in metadata"` 를 로그로 남기고 **조용히 반환한다.**
  (SDK 1.137.1 `Robot.requestPermissions` 바이트코드로 확인)
- **수정:** `<uses-permission>` 을 지우고 `<application>` 안에 meta-data 로 선언.
  ```xml
  <meta-data
      android:name="@string/metadata_permissions"
      android:value="@string/temi_permissions" />
  ```
  값은 `res/values/strings.xml` 의 `temi_permissions` (`com.robotemi.permission.map`).
  SDK 는 선언 문자열에 대해 `contains()` 로 검사하므로 여러 개는 쉼표로 구분하면 된다.
- **확인:** APK 바이너리 매니페스트에서 `@string/metadata_permissions` →
  `com.robotemi.sdk.metadata.PERMISSIONS`, `@string/temi_permissions` →
  `com.robotemi.permission.map` 로 해석되는 것까지 검증했다.
- **함께 넣은 예외 처리**
  - `TemiRepository.verifyPermissionDeclaration()` — 준비 시점에 meta-data 선언 누락을
    `Log.e` 로 남긴다. 같은 실수를 다시 하면 즉시 로그에서 드러난다.
  - `reportPermissionRequestTimeout()` — 결과 콜백이 유실되면 `requestInFlight` 가 true 로
    남아 '권한 허용' 버튼이 영구히 잠기던 문제를 60초 폴백으로 해제하고 실제 승인 상태를 재조회한다.
  - 거부 시 `UiAlert.PermissionDenied` 안내 + 권한 카드의 버튼으로 재요청.
- **참고:** `Permission.MAP` 은 키오스크 전용 권한이 아니다(`isKioskPermission = false`).
  즉 선택된 키오스크 앱이 아니어도 승인받을 수 있다. `GRANTED = 1`, `DENIED = 0`.

### 후속 확인 필요
- **`strings.xml` 의 `location_office` / `location_lobby` 는 현장 temi 지도의 POI 이름과
  정확히 일치해야 한다.** 현재는 "사무실" / "로비" 로 두었고, 다르면 이 값을 수정한다.
- `TemiFeaturePermission` enum 에 권한을 추가하면 `strings.xml` 의 `temi_permissions` 에도
  같이 추가해야 한다. 한쪽만 고치면 요청이 조용히 무시된다.
- 빌드 환경 정렬: `androidx.core 1.19.0` / `lifecycle 2.11.0` 은 AGP 9.1.0 + compileSdk 37 을
  요구해 빌드가 실패했다. AGP 8.13.2 기준으로 `core-ktx 1.16.0`, `lifecycle 2.9.2`,
  `activity-compose 1.10.1` 로 정렬했다. 추후 AGP 를 9.x 로 올릴 때 되돌린다.
