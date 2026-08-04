# 1단계 - 요구사항 명세와 화면 흐름 확정

이 문서는 `temi_app_ROADMAP.md`(요구사항)와 `temi.pdf`(디자인/흐름)를 코드 작업 단위로 변환한
구현 명세서다. 2단계부터는 이 문서의 라우트·컴포넌트·데이터 소스 이름을 그대로 따른다.

## 0. 표기 규칙

- 라우트는 Navigation Compose 문자열 기준으로 표기한다. `{}`는 필수 인자, `?x=`는 선택 인자.
- "구현 위치"는 2단계에서 만들 패키지 기준이다 (아래 1.1 참고). 아직 존재하지 않는 파일이다.
- "테스트 항목"은 해당 화면/로직을 검증할 단위·UI 테스트의 핵심 케이스만 적는다(전체 목록 아님).

### 0.1 패키지 구조 (2단계에서 생성)

```text
com.example.ibtech
├─ MainActivity.kt
├─ navigation/          NavGraph, Route 상수
├─ robot/                (기확정) TemiController / TemiRepository / TemiState
├─ domain/
│  ├─ model/             Facility, UsageTopic, QuizQuestion, RecommendedBook, LibraryEvent, LibrarySettings
│  └─ usecase/           GetVisibleFacilitiesUseCase, ResolveGuideOptionUseCase, SyncPoiUseCase 등
├─ data/
│  ├─ local/room/        Entity + Dao (Facility/UsageTopic/Quiz/Book/Event/Statistics)
│  ├─ datastore/         AppSettingsDataStore (도서관명, 환영문구, 음량, 무입력시간, 관리자 비밀번호 해시)
│  └─ repository/        FacilityRepository, UsageRepository, KidsContentRepository,
│                         EventRepository, SettingsRepository, StatisticsRepository
├─ ui/
│  ├─ common/            LibraryTopAppBar, LibraryScaffold, ConfirmDialog, EmptyState, ErrorState
│  ├─ home/
│  ├─ facility/          list, detail, map, navigationprogress
│  ├─ usage/             category, subcategory, answer
│  ├─ kids/              menu, quizcategory, quizplay, quizresult, bookrecommendation, etiquette
│  ├─ events/            list, detail
│  └─ theme/             (기존, 2단계에서 민트 테마로 교체)
└─ admin/
   ├─ AdminLoginScreen, FacilityAdminScreen, UsageInfoAdminScreen,
      KidsContentAdminScreen, EventAdminScreen, SettingsAdminScreen, StatisticsScreen
```

> **정정 (2026-08-04):** 도서관명은 **"신트리도서관"으로 고정**한다(로드맵 2.1절 정정과 동일).
> 이에 따라 `initial_setup` 라우트/화면은 두지 않는다. 이 문서의 1절 라우트 표, 2.1/2.2/6.1절,
> 7절 추적표에서 최초 설정·도서관명 관리자 입력을 언급하는 부분은 모두 이 정정을 따른다.

## 1. 화면·라우트 목록

| # | 화면 | 라우트 | 상단바 유형 |
|---|---|---|---|
| 0 | 최초 설정 | `initial_setup` | 없음(전용 레이아웃) |
| 1 | 홈 | `home` | 메인(뒤로/홈 없음, 도서관명만) |
| 2 | 시설 목록 | `facility_list?query={query}` | 서브 |
| 3 | 시설 상세/안내 방식 | `facility_detail/{facilityId}` | 서브 |
| 4 | 위치만 보기 | `facility_map/{facilityId}` | 서브 |
| 5 | 동행 이동 진행 | `facility_navigation/{facilityId}` | 서브(이동 중 뒤로/홈 확인) |
| 6 | 이용방법 1차 메뉴 | `usage_category` | 서브 |
| 7 | 이용방법 세부 항목 | `usage_subcategory/{categoryId}` | 서브 |
| 8 | 이용방법 답변 | `usage_answer/{topicId}` | 서브 |
| 9 | 어린이 콘텐츠 메뉴 | `kids_menu` | 서브 |
| 10 | 퀴즈 주제 선택 | `kids_quiz_category` | 서브 |
| 11 | 퀴즈 진행 | `kids_quiz_play/{category}` | 서브 |
| 12 | 퀴즈 결과 | `kids_quiz_result/{category}` | 서브 |
| 13 | 추천도서 | `kids_book_recommendation?ageGroup={ageGroup}&topic={topic}` | 서브 |
| 14 | 도서관 예절 | `kids_etiquette` | 서브 |
| 15 | 오늘의 행사 목록 | `events` | 서브 |
| 16 | 행사 상세 | `event_detail/{eventId}` | 서브 |
| 17 | 관리자 로그인 | `admin_login` | 서브(진입 경로 별도, 3.6절 참고) |
| 18 | 시설 관리 | `admin_facility` | 서브(관리자) |
| 19 | 이용정보 관리 | `admin_usage` | 서브(관리자) |
| 20 | 어린이 콘텐츠 관리 | `admin_kids` | 서브(관리자) |
| 21 | 행사·공지 관리 | `admin_event` | 서브(관리자) |
| 22 | 설정 관리(도서관명/음량/무입력시간/백업) | `admin_settings` | 서브(관리자) |
| 23 | 이용 통계 | `admin_statistics` | 서브(관리자) |

`admin_login` 진입 경로: 홈 화면에는 노출하지 않는다(고령층·어린이 오조작 방지). 도서관명/로고
영역 길게 누르기 등 숨겨진 제스처로 진입한다(8절 결정 사항). 정확한 트리거(누르는 시간 등)는
2단계 홈 화면 구현 시 확정한다.
`직원 도움`은 별도 라우트가 아니라 다이얼로그(연락처/내선 안내)로 처리한다.

## 2. 화면별 상세 명세

각 행은 `버튼/액션 → 결과` 형식이다. "입력"은 화면이 구독하는 상태, "출력"은 다음 화면 전달값이다.

### 2.1 최초 설정 `initial_setup`

- 진입 조건: `SettingsRepository.libraryName`이 비어 있을 때, 스플래시 직후 이용자 화면보다 우선 진입.
- 표시: 도서관명 입력 필드, 저장 버튼.
- 액션: 저장 → `SettingsRepository.setLibraryName()` → `home`으로 이동, 백스택 제거.
- 입력: 없음(최초 실행 감지만). 출력: 없음.
- 구현 위치: `ui/home/InitialSetupScreen.kt`, `data/repository/SettingsRepository.kt`.
- 테스트: 도서관명 미설정 시 홈 대신 이 화면으로 리다이렉트되는지, 저장 후 홈에 즉시 반영되는지.

### 2.2 홈 `home`

- 표시: 도서관명(관리자 설정값), 환영 문구, 4개 버튼(시설/이용방법/로봇과 놀아요/오늘의 행사).
- 액션:
  - `시설을 찾고 있어요` → `facility_list`
  - `이용방법이 궁금해요` → `usage_category`
  - `로봇과 놀아요` → `kids_menu`
  - `오늘의 행사` → `events`
- 입력: `SettingsRepository.librarySettings`(이름/환영문구), 이동 상태(`robot.NavigationState` — 이동 중이면
  상단에 "이동 중" 배지만 보여주고 버튼은 그대로 사용 가능, 새 이동 명령은 3.4절 규칙 적용).
- 출력: 없음(각 라우트로 단순 이동).
- 구현 위치: `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`.
- 테스트: 4개 버튼 각각 올바른 라우트로 이동, 도서관명 변경 시 재구성 없이 즉시 반영,
  무입력 타이머가 홈에서는 비활성(=복귀 대상이라 자기 자신에 대해 동작 불필요).

### 2.3 시설 목록 `facility_list?query={query}`

- 표시: `노출=true`인 Facility 카드 그리드, 검색창(POI 많을 때), `다른 장소 찾기`(검색 포커스 이동
  또는 숨김 목록 토글 — PDF는 버튼으로 제시하므로 "검색 모드 전환" 버튼으로 구현).
- 액션: 카드 탭 → `facility_detail/{facilityId}`.
- 입력: `FacilityRepository.visibleFacilities: Flow<List<Facility>>`.
- 빈 데이터: 노출 시설이 0개면 "등록된 시설이 없습니다. 관리자에게 문의해 주세요" + 홈 버튼.
- 구현 위치: `ui/facility/FacilityListScreen.kt`, `FacilityListViewModel.kt`.
- 테스트: 검색어 필터링, 빈 목록 상태, `노출=false`/`미설정` 시설이 목록에서 제외되는지.

### 2.4 시설 상세 `facility_detail/{facilityId}`

- 표시: 시설명, 층, 짧은 설명, 안내 방식 버튼(4절 로직 결과에 따라 동적 구성).
- 액션:
  - `동행 안내` (같은 층만 노출) → 확인 다이얼로그 → 승인 시 `facility_navigation/{facilityId}`
  - `위치만 보기` → `facility_map/{facilityId}`
- 입력: `Facility`(id로 조회), `ResolveGuideOptionUseCase(facility, robotState)`.
- 예외: `미설정` 시설은 이 화면에 도달하지 않도록 목록 단계에서 걸러진다(3.2절).
  Temi 미연결 상태면 `동행 안내` 버튼을 비활성화하고 사유 텍스트를 보여준다.
- 구현 위치: `ui/facility/FacilityDetailScreen.kt`, `FacilityDetailViewModel.kt`,
  `domain/usecase/ResolveGuideOptionUseCase.kt`.
- 테스트: 같은 층/다른 층 분기, SDK 미연결 시 버튼 비활성, 잘못된 facilityId 처리(뒤로 이동 안내).

### 2.5 위치만 보기 `facility_map/{facilityId}`

- 표시: 층·이동 방향·엘리베이터 위치 텍스트, 지도 이미지(있으면) 또는 텍스트 설명.
- 액션: 없음(정보 표시 전용). 관련 시설 안내 링크(있는 경우) 제공 가능.
- 입력: `Facility.directionText`, `Facility.mapImagePath`.
- 빈 데이터: `mapImagePath`가 없으면 텍스트 설명만 표시(레이아웃 깨지지 않게).
- 구현 위치: `ui/facility/LocationMapScreen.kt`.
- 테스트: 이미지 없음/있음 두 상태 스냅샷.

### 2.6 동행 이동 진행 `facility_navigation/{facilityId}`

- 상태 머신(로드맵 8장)과 1:1 대응:

| NavigationState | 화면 표시 | 사용자 액션 |
|---|---|---|
| `Requested`/확인 전 | 목적지 확인 다이얼로그 | 승인(이동 시작)/취소(뒤로) |
| `Moving` | 목적지, 진행 표시, `이동 중지` 버튼 | 중지 → 확인 후 `stopMovement()` |
| `Interrupted`(USER_STOPPED) | "이동을 중지했습니다" + 홈/재시도 | 재시도 → 같은 라우트 재진입 |
| `Interrupted`(EXTERNAL_INTERRUPTION) | 사유 안내 + 재시도/위치만 보기/홈 | - |
| `Arrived` | 도착 음성/화면 안내 + 홈 버튼 | 자동 3~5초 후 홈 복귀 또는 버튼 |
| `Failed` | 실패 사유 + 재시도/위치만 보기/홈 | - |

- 뒤로가기/홈 버튼: `Moving` 상태에서 누르면 "안내 이동을 중지하고 이동하시겠습니까?" 확인창(3.6절).
- 입력: `robot.navigationState`(via `robot/TemiController`).
- 구현 위치: `ui/facility/NavigationProgressScreen.kt`, `NavigationViewModel.kt`
  (기존 `robot/TemiRepository`를 그대로 사용, UI 전용 상태 매핑만 새로 만든다).
- 테스트: 상태별 화면 분기, 중복 `goTo` 방지, 뒤로/홈 확인창 노출 조건, 타임아웃 폴백 반영.

### 2.7 이용방법 1차 메뉴 `usage_category`

- 표시: 4개 카테고리 카드(대출·반납/회원가입/열람실 이용/운영시간·휴관일).
- 액션: 카드 탭 → `usage_subcategory/{categoryId}`. 세부 항목이 1개뿐인 카테고리(회원가입,
  운영시간·휴관일 등)는 세부 목록 없이 바로 `usage_answer/{topicId}`로 건너뛴다.
- 입력: `UsageRepository.categories`.
- 구현 위치: `ui/usage/UsageCategoryScreen.kt`.
- 테스트: 하위 항목 1개일 때 스킵 라우팅.

### 2.8 이용방법 세부 항목 `usage_subcategory/{categoryId}`

- 표시: 대출·반납 예시 4개(대출 가능 권수/대출 기간/연장·예약/무인반납 방법) 등 카테고리별 하위 목록.
- 액션: 항목 탭 → `usage_answer/{topicId}`.
- 입력: `UsageRepository.subtopics(categoryId)`.
- 구현 위치: `ui/usage/UsageSubcategoryScreen.kt`.

### 2.9 이용방법 답변 `usage_answer/{topicId}`

- 표시: 핵심 답변(3줄 이내 우선), QR(있으면), 관련 시설 안내 버튼, 직원 도움 버튼.
- 액션:
  - `관련 시설 안내` → `facility_detail/{relatedFacilityId}` (없으면 버튼 숨김)
  - `직원 도움` → 다이얼로그
  - 긴 답변은 "더 보기"로 확장(2.1절 접근성 규칙과 함께, 잘림 없이 스크롤도 허용)
- 입력: `UsageTopic`.
- 구현 위치: `ui/usage/UsageAnswerScreen.kt`.
- 테스트: QR 없음/있음, 관련 시설 없음/있음, 긴 답변 확장.

### 2.10 어린이 콘텐츠 메뉴 `kids_menu`

- 표시: 오늘의 퀴즈 / 나에게 맞는 책 / 도서관 예절 / (행사 연계는 카드 하단 배너로 오늘 행사 있으면 노출).
- 액션: `kids_quiz_category`, `kids_book_recommendation`, `kids_etiquette`로 각각 이동.
- 구현 위치: `ui/kids/KidsMenuScreen.kt`.

### 2.11 퀴즈 주제 선택 `kids_quiz_category`

- 표시: 동물/공룡/과학/동화 등 `QuizQuestion.category` distinct 목록(관리자 등록 기준, 하드코딩 아님).
- 액션: 주제 탭 → `kids_quiz_play/{category}`.
- 빈 데이터: 문제 0개인 주제는 카드 비활성 + "문제 준비 중" 표시.
- 구현 위치: `ui/kids/QuizCategoryScreen.kt`.

### 2.12 퀴즈 진행 `kids_quiz_play/{category}`

- 표시: 문제(최대 3문제), 보기 3개, 진행 표시(1/3 등).
- 액션: 보기 선택 → 즉시 정오답 피드백(짧은 음성+화면) → 자동으로 다음 문제 또는 결과 화면.
- 입력: `KidsContentRepository.quizQuestions(category, limit = 3)`.
- 예외: 해당 주제 문제가 3개 미만이면 있는 만큼만 출제(최소 1개). 중복 문제 방지를 위해
  세션 내 이미 낸 문제 id는 재출제하지 않는다.
- 출력: `kids_quiz_result/{category}`로 정답 수·추천도서 후보를 SavedStateHandle로 전달.
- 구현 위치: `ui/kids/QuizPlayScreen.kt`, `QuizViewModel.kt`.
- 테스트: 문제 3개 미만, 중복 방지, 정답률 계산, 중간에 홈 이동 시 세션 폐기.

### 2.13 퀴즈 결과 `kids_quiz_result/{category}`

- 표시: 정답/오답 요약, 추천도서 카드(정답 문제의 `recommendedBookIds` 기반), 액션 버튼 2개.
- 액션: `어린이자료실 안내` → `facility_detail/{어린이자료실 facilityId}`, `다른 퀴즈 하기` → `kids_quiz_category`.
- 구현 위치: `ui/kids/QuizResultScreen.kt`.

### 2.14 추천도서 `kids_book_recommendation?ageGroup=&topic=`

- 표시: 표지/제목/저자/한줄소개/위치정보 카드 목록, 연령·주제 필터.
- 액션: `어린이자료실 안내` 버튼(공통) → `facility_detail/{facilityId}`.
- 빈 데이터: 필터 결과 없음 → "조건에 맞는 책이 없습니다" + 필터 초기화 버튼.
- 구현 위치: `ui/kids/BookRecommendationScreen.kt`.

### 2.15 도서관 예절 `kids_etiquette`

- 표시: 카드 또는 짧은 퀴즈 형태(행동 중심 문구, PDF 예시: "책 다 읽은 뒤 책 수레에 놓아 주세요").
- 구현 위치: `ui/kids/LibraryEtiquetteScreen.kt`.

### 2.16 오늘의 행사 목록 `events`

- 표시: 오늘/예정 행사 카드(행사명/시간/장소/대상/짧은 설명/신청여부), 주요 공지 섹션.
- 빈 데이터: "오늘 예정된 행사가 없습니다" + 다음 행사 카드(있으면) 또는 홈 버튼.
- 액션: 카드 탭 → `event_detail/{eventId}`.
- 구현 위치: `ui/events/EventsScreen.kt`.
- 테스트: 오늘 행사 없음/있음, 다음 행사 표시 로직.

### 2.17 행사 상세 `event_detail/{eventId}`

- 표시: 상세 정보, 신청 QR, `장소 안내` 버튼.
- 액션: `장소 안내` → `facility_detail/{relatedFacilityId}`(없으면 버튼 숨김).
- 구현 위치: `ui/events/EventDetailScreen.kt`.

### 2.18~2.23 관리자 화면군 (`admin_*`)

공통: `admin_login` 통과 전에는 접근 불가(딥링크 포함). 세션은 무입력 자동 로그아웃(관리자
전용 타이머, 3.6절 일반 무입력 복귀와 별도) 적용을 2단계 설계에서 확정.

| 화면 | 핵심 기능 | 구현 위치 |
|---|---|---|
| `admin_facility` | Temi POI 새로고침/동기화, 신규·변경·삭제 상태 표시, POI별 표시명/층/설명/안내방식/아이콘/노출 설정 | `admin/FacilityAdminScreen.kt` |
| `admin_usage` | 이용정보 CRUD(카테고리/세부항목/답변/QR/관련시설) | `admin/UsageInfoAdminScreen.kt` |
| `admin_kids` | 퀴즈/추천도서/예절 CRUD | `admin/KidsContentAdminScreen.kt` |
| `admin_event` | 행사/공지 CRUD | `admin/EventAdminScreen.kt` |
| `admin_settings` | 도서관명/환영문구/음량/무입력시간/JSON 백업·복구/비밀번호 변경 | `admin/SettingsAdminScreen.kt` |
| `admin_statistics` | 4.3절 통계 집계 조회, CSV/JSON 내보내기 | `admin/StatisticsScreen.kt` |

- 입력값 검증과 저장 성공/실패는 모든 관리자 화면 공통 스낵바 패턴으로 통일한다(`ui/common`).
- 테스트: 각 CRUD의 저장 실패(빈 값/중복) 검증, POI 동기화 3상태(신규/변경/삭제) 표시.

## 3. 공통 상단바 규칙 (`LibraryTopAppBar`)

- 구성: 왼쪽 `뒤로가기`(아이콘+텍스트), 가운데 화면 제목, 오른쪽 `홈`(아이콘+텍스트).
- 메인(`home`, `initial_setup`)에는 표시하지 않는다. 그 외 모든 라우트는 반드시 표시한다.
- `뒤로가기`: `NavController.popBackStack()`. 논리적으로 직전 화면이 없는 진입(딥링크 등)이면
  `home`으로 대체 이동.
- `홈`: 항상 `navigate(home) { popUpTo(home) { inclusive = true } }` 로 스택을 비운다.
- Android 시스템 뒤로가기(`BackHandler`)는 상단바 `뒤로가기`와 동일 동작으로 매핑한다.
- 예외: `facility_navigation`의 `NavigationState.isBusy == true`일 때는 `뒤로가기`/`홈`/시스템
  뒤로가기 모두 즉시 이동하지 않고 "안내 이동을 중지하고 이동하시겠습니까?" `ConfirmDialog`를
  띄운다. 확인 시 `stopMovement()` 호출 후 이동, 취소 시 화면 유지.
- 관리자 화면(`admin_*`)도 동일 상단바를 쓴다. `홈`은 로드맵 원문대로 이용자 홈 화면으로
  이동하며 관리자 세션은 종료된다(8절 결정 사항). 관리자 화면 간 이동은 별도 뒤로가기/관리자
  메뉴로 처리하고 `홈`과 혼동하지 않는다.

## 4. 안내 방식 결정 로직 (의사코드)

로드맵 원문(2.1/3.2)은 "1층/타 층"으로 서술하지만, 도서관마다 로봇 운영 기준층이 다를 수
있으므로 **하드코딩 금지 원칙에 따라 "기준층(baseFloor)"을 관리자 설정값으로 둔다**
(기본값 1). 이는 PDF 요구사항을 일반화한 설계 결정이며, 2단계 검토 시 확정한다.

```text
fun resolveGuideOptions(facility: Facility, settings: LibrarySettings): GuideOptionSet {
    if (!facility.isEnabled) return GuideOptionSet.Hidden
    if (facility.floor == UNSET_FLOOR) return GuideOptionSet.TemiEscortOnlyUnconfigured
    // 관리자가 명시적으로 지정한 안내 방식이 최우선
    return when (facility.guideMode) {
        ESCORT ->
            if (facility.floor == settings.baseFloor) GuideOptionSet.EscortAndLocationOnly
            else GuideOptionSet.LocationOnlyWithDirections   // 안전장치: 타 층 동행 강제 금지
        LOCATION_ONLY -> GuideOptionSet.LocationOnlyWithDirections
        BOTH ->
            if (facility.floor == settings.baseFloor) GuideOptionSet.EscortAndLocationOnly
            else GuideOptionSet.LocationOnlyWithDirections
    }
}
```

```text
fun canStartEscort(facility: Facility, robot: RobotSnapshot): EscortGate {
    if (robot.connection != Ready) return EscortGate.Blocked(REASON_SDK_NOT_READY)
    if (!robot.permissions.allGranted) return EscortGate.Blocked(REASON_PERMISSION)
    if (!robot.knownLocations.contains(facility.sourcePoiName)) return EscortGate.Blocked(REASON_POI_MISSING)
    if (robot.navigation.isBusy) return EscortGate.Blocked(REASON_ALREADY_MOVING)
    if (facility.floor != settings.baseFloor) return EscortGate.Blocked(REASON_DIFFERENT_FLOOR)
    return EscortGate.Allowed
}
```

## 5. 예외 흐름 정리

| 시나리오 | 적용 화면 | 화면 동작 |
|---|---|---|
| 정상 | 전체 | 2장 명세대로 동작 |
| 빈 데이터(시설/이용정보/퀴즈/추천도서/행사 0건) | 각 목록 화면 | 빈 화면 대신 안내 문구 + 홈/대안 버튼(4.4·9단계 원칙) |
| Temi SDK 미연결(`TemiConnectionState.Unavailable`) | `facility_detail`, `facility_navigation` | 동행 관련 버튼 비활성 + "로봇 연결을 확인해 주세요", 위치만 보기·이용방법·어린이콘텐츠는 정상 이용 가능(오프라인 우선 원칙, 3.8절) |
| 로봇 위치/권한 미확인 | `facility_detail` | `canStartEscort()` 결과에 따라 사유별 안내 문구(REASON_* 매핑) |
| 잘못된/없는 POI(`facilityId` 오류, `sourcePoiName` 지도에 없음) | `facility_detail`, `facility_navigation` | REASON_POI_MISSING 안내 + 관리자 문의 유도, 목록에서 자동 재검증 |
| 이동 요청 거절/실패 | `facility_navigation` | `NavigationState.Failed` 분기(2.6절 표) |
| 네트워크 끊김 | QR 열기, 통계 내보내기 등 네트워크 의존 기능만 | 실패 토스트, 핵심 안내(시설/이용방법/기본 퀴즈)는 로컬 데이터로 계속 동작 |
| QR 링크 오류 | `usage_answer`, `event_detail` | QR 이미지/버튼은 유지하되 실패 시 "QR을 열 수 없습니다" 안내, 화면 이탈 없음 |

## 6. 도서관명 변경 / Temi POI 동기화 흐름

### 6.1 도서관명 변경

```text
관리자(admin_settings) 도서관명 저장
  → SettingsRepository.setLibraryName(newName)  (DataStore 갱신)
  → HomeViewModel/그 외 구독자는 Flow 구독 중이므로 재실행/재조회 없이 즉시 재구성
  → 앱 재설치·코드 수정 불필요 (요구사항 3.1 충족)
```

### 6.2 Temi POI 동기화 (앱 시작 시 자동, 관리자 화면에서 수동 트리거 가능)

```text
SyncPoiUseCase(local: List<Facility>, remote: List<String> = robot.getKnownPois()):
  신규 = remote 중 local에 sourcePoiName 없는 것
       → Facility(floor = UNSET, guideMode = LOCATION_ONLY(임시), isEnabled = false) 로 추가
         ("미설정" 상태, 관리자가 설정 전까지 이용자 화면 비노출 — 3.2절)
  변경 = local과 remote에 모두 있는 것 → local의 관리자 메타데이터(표시명/층/설명/안내방식/
         아이콘/노출여부)는 절대 덮어쓰지 않는다. sourcePoiName 매칭만 갱신.
  사라짐 = local에는 있으나 remote에 없는 것 → 즉시 삭제하지 않고
         local.syncStatus = NOT_FOUND_ON_TEMI 로 표시, 관리자 확인 후 수동 삭제만 허용.
  결과를 admin_facility 화면에 신규/변경/삭제(미확인) 3구간으로 표시.
```

- 트리거 시점: 앱 프로세스 시작(`onRobotReady`) 1회, `admin_facility`의 "새로고침" 버튼.
- 이용자 화면(`facility_list`)은 `isEnabled == true && floor != UNSET`인 Facility만 구독하므로
  동기화 중간 상태가 이용자에게 노출되지 않는다.

## 7. 요구사항 추적표 (구현 위치·테스트 항목 1:1 연결)

| 요구사항 | 화면/모듈 | 구현 위치 | 핵심 테스트 |
|---|---|---|---|
| 목적 중심 4개 메인 버튼 | 2.2 홈 | `ui/home/HomeScreen.kt` | 라우팅 4종 |
| 도서관명 사용자 설정 | 2.1, 2.2, 6.1, 관리자 설정 | `data/repository/SettingsRepository.kt`, `admin/SettingsAdminScreen.kt` | 저장 후 즉시 반영 |
| Temi POI 동적 시설 목록 | 2.3, 6.2 | `domain/usecase/SyncPoiUseCase.kt`, `data/repository/FacilityRepository.kt` | 신규/변경/삭제 3분기 |
| 1층 동행 / 타 층 위치 안내 | 2.4, 4장 의사코드 | `domain/usecase/ResolveGuideOptionUseCase.kt` | 같은 층/다른 층/미설정 분기 |
| 동행 안내 상태(시작/중/도착/실패) | 2.6 | `ui/facility/NavigationProgressScreen.kt`, 기존 `robot/TemiRepository.kt` | 상태별 화면, 중복 goTo 방지 |
| 대출·반납/회원가입/열람실/운영시간 | 2.7~2.9 | `ui/usage/*`, `data/repository/UsageRepository.kt` | 3줄 답변, QR 유무, 관련시설 유무 |
| 퀴즈(최대 3문제·보기 3개) | 2.11~2.13 | `ui/kids/QuizPlayScreen.kt`, `QuizViewModel.kt` | 문제 부족, 중복 방지, 정답률 |
| 추천도서/예절 | 2.14, 2.15 | `ui/kids/*` | 필터 결과 없음 |
| 오늘의 행사·공지 | 2.16, 2.17 | `ui/events/*` | 행사 없음 상태 |
| 상단 뒤로가기·홈(전 화면) | 3장 | `ui/common/LibraryTopAppBar.kt` | 전 라우트 홈 복귀, 시스템 뒤로가기 동일 동작 |
| 이동 중 뒤로/홈 확인 | 3장, 2.6 | `ui/facility/NavigationProgressScreen.kt` | 확인창 노출/취소/승인 |
| 음성 안내(중복 방지) | 3.7 | `robot/TemiRepository.kt`(기존 TTS 큐 로직 재사용) | 연속 발화 취소 정책 |
| 13.3인치·큰 버튼·짧은 문장 | 2단계 디자인 토큰 | `ui/theme/*`(재작성 예정) | 큰 글씨 잘림 없음 스냅샷 |
| 무입력 자동 홈 복귀(이동 중/충전 중 제외) | 전 화면 | `ui/common/IdleTimeoutObserver.kt` | 이동 중 미동작, 충전 중 미동작 |
| 관리자 CRUD + 검증 | 2.18~2.23 | `admin/*`, `data/repository/*` | 저장 성공/실패 표시 |
| 이용 통계(개인정보 없음) | `admin_statistics` | `data/local/room/StatisticsDao.kt` | 이벤트별 집계, CSV/JSON 내보내기 |
| 오프라인 핵심 안내 | 시설/이용방법/기본 퀴즈 | 각 Repository의 Room 우선 조회 | 네트워크 끊김 상태에서도 조회 가능 |
| SDK/네트워크 장애 시 앱 미정지 | 5장 예외 흐름 | 전 화면 공통 | 각 장애 케이스별 화면 스냅샷 |

## 8. 확정된 결정 사항 (사용자 승인 완료)

1. **기준층(`baseFloor`)**: 관리자 설정값으로 둔다(기본값 1). PDF의 "1층 고정" 대신 도서관마다
   다른 로봇 운영 층을 지원한다. `LibrarySettings.baseFloor: Int = 1`을 데이터 모델에 반영한다.
2. **관리자 진입**: 홈 화면 숨김 제스처(예: 도서관명/로고 영역 길게 누르기)로 `admin_login`에
   진입한다. 이용자 화면에는 관리자 버튼을 노출하지 않는다. 제스처 트리거 상세(길게 누르기
   시간, 연속 탭 횟수 등)는 2단계 홈 화면 구현 시 확정한다.
3. **관리자 화면 `홈` 버튼**: 로드맵 원문대로 이용자 홈 화면으로 이동한다(관리자 세션은
   종료됨). `admin_*` 화면 간 이동에는 별도 관리자 내비게이션(뒤로가기 또는 관리자 메뉴
   탭)을 쓰고, `홈`은 항상 이용자 화면 복귀 용도로 통일한다.

이 결정들은 2절 화면 명세와 4절 의사코드에 이미 반영되어 있다.

이 문서가 승인되면 2단계(프로젝트 뼈대와 공통 디자인 시스템)로 진행합니다.
