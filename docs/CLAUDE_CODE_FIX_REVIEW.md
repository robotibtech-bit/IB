# Temi 도서관 앱 코드 안정화 검토 및 수정 지침

## 1. 목적

현재 프로젝트의 전체 구조는 유지한다. 전면 리팩터링이 목적이 아니라, 정적 분석에서 확인된 실제 동작 오류·상태 관리 누락·경쟁 조건·통계 정확성 문제를 우선적으로 안정화하는 것이 목적이다.

수정 전에 반드시 현재 코드를 다시 읽고 아래 지적이 실제 현재 코드에도 존재하는지 확인한다. 지적 내용을 무조건 적용하지 말고, 코드 근거가 확인된 항목만 최소 범위로 수정한다.

## 2. 절대 규칙

- 현재 화면 디자인, 레이아웃, 문구, Navigation 구조를 임의로 변경하지 않는다.
- 기존 정상 기능을 삭제하거나 동작 방식을 임의로 바꾸지 않는다.
- Temi SDK 관련 수정은 현재 `TemiController` / `TemiRepository` 추상화 구조를 유지한다.
- 새로운 대규모 라이브러리나 아키텍처를 도입하지 않는다.
- 문제 하나를 해결하기 위해 관련 없는 파일까지 정리하거나 리팩터링하지 않는다.
- 기존 통계 항목과 관리자 데이터는 보존한다.
- Fake와 Real Temi 구현의 동작 계약이 가능한 한 일치하도록 한다.
- 수정 전 각 문제의 실제 존재 여부를 확인하고, 잘못된 지적이면 수정하지 말고 이유를 보고한다.
- 기존 테스트를 함부로 삭제하거나 약화하지 않는다.

---

# 3. 최우선: Temi 실제 주행 상태 안정화

## 3.1 실제 `goTo()` 중복 호출 방지

관련 파일:

- `robot/TemiRepository.kt`
- `robot/FakeTemiController.kt`
- `ui/facility/NavigationViewModel.kt`
- `ui/common/ConfirmDialog.kt`

현재 확인 사항:

- `FakeTemiController.goTo()`에는 이미 이동 중이면 거부하는 가드가 있다.
- 실제 `TemiRepository.goTo()`에는 동일 수준의 `navigationState.isBusy` 가드가 없다.
- `NavigationViewModel.onConfirmStart()`에도 재진입 방지가 없다.
- 확인 버튼을 빠르게 연속 탭하면 실제 Temi에 `goTo()`가 중복 전달될 가능성이 있다.

수정 목표:

- ViewModel 레벨과 실제 Temi Controller 레벨 모두에서 중복 시작을 안전하게 차단한다.
- 첫 요청이 처리된 뒤 같은 확인 이벤트가 다시 들어와도 두 번째 `goTo()`가 Temi로 전달되지 않도록 한다.
- Fake/Real의 계약 차이도 함께 확인한다.

## 3.2 `goTo()` 실패 반환값 처리

현재 `NavigationViewModel.onConfirmStart()`는 `controller.goTo()`의 Boolean 반환값을 실질적으로 확인하지 않는다.

문제 상황:

1. 시설 상세에서는 Temi가 Ready였음.
2. 확인 팝업을 누르는 사이 연결 상태가 변함.
3. `goTo()`가 false를 반환함.
4. `hasStarted=true`와 시작 TTS만 진행될 수 있음.

수정 목표:

- `goTo()`가 실제 요청을 보내지 못했으면 이동 시작 상태로 확정하지 않는다.
- 사용자가 빈 이동 화면에 남지 않도록 기존 실패 처리 흐름과 자연스럽게 연결한다.

## 3.3 `reportStopTimeout()` 실제 연결

`TemiRepository.reportStopTimeout()`은 구현되어 있지만 현재 실제 호출처가 없는 것으로 확인됐다.

이 함수의 기존 목적은 `stopMovement()` 이후 Temi 최종 콜백이 유실되었을 때 UI가 영구히 `Requested/Moving` 상태에 남지 않도록 하는 것이다.

수정 목표:

- 이동 중지 요청 이후 적절한 제한 시간 동안 최종 상태 콜백이 없을 때만 `reportStopTimeout()`이 동작하도록 연결한다.
- 정상 콜백이 들어온 경우 불필요한 timeout 처리가 발생하지 않아야 한다.

## 3.4 이동 요청 timeout 감시 방식 점검

`NavigationViewModel`에서 `controller.navigationState.collect { Requested -> delay(10초) ... }` 방식으로 timeout을 감시하고 있다.

문제:

- 이전 `Requested`를 위한 `delay` 동안 collector가 다음 상태를 순차적으로 기다리므로 새로운 요청의 timeout 감시 시작이 늦어질 수 있다.

수정 목표:

- 최신 이동 요청만 timeout 감시 대상으로 유지한다.
- 이전 요청을 위한 대기가 새로운 요청의 10초 timeout 시작을 지연시키지 않도록 한다.
- `collectLatest` 등 기존 Coroutine/Flow 구조 안에서 최소 변경으로 해결 가능한지 검토한다.

## 3.5 `consumeNavigationResult()` 연결

`TemiRepository`와 `FakeTemiController` 모두 `consumeNavigationResult()`를 구현하지만 실제 호출처가 없는 것으로 확인됐다.

현재 위험:

- 이전 `Arrived`, `Interrupted`, `Failed`가 StateFlow의 현재값으로 계속 남을 수 있다.
- 이후 새로운 `NavigationViewModel` 생성 시 이전 terminal state가 즉시 replay될 수 있다.
- 이전 이동의 도착 TTS 또는 NAV_SUCCESS/NAV_FAILED/NAV_CANCELLED 통계가 다시 처리될 가능성이 있다.

수정 목표:

- terminal state의 UI/통계/TTS 처리가 필요한 만큼 정확히 1회 수행된 뒤 안전하게 `Idle`로 소비되도록 한다.
- 결과를 너무 일찍 소비해 도착/실패 화면이 사라지는 부작용은 만들지 않는다.

## 3.6 `reportPermissionRequestTimeout()` 실제 연결

`reportPermissionRequestTimeout()` 역시 구현은 되어 있지만 호출처가 없는 것으로 확인됐다.

현재 위험:

- 권한 요청 결과 콜백이 유실되면 `requestInFlight=true`가 계속 남을 수 있다.
- 이후 권한 요청 버튼이 계속 잠긴 상태가 될 수 있다.

수정 목표:

- 권한 요청 후 결과 콜백이 오지 않는 경우에만 timeout fallback을 실행한다.
- 정상 승인/거부 콜백이 온 경우에는 기존 흐름을 그대로 유지한다.

---

# 4. 최우선: 데이터 및 통계 정확성

## 4.1 `FacilityRepository` read-modify-write 경쟁 조건

현재 여러 Repository가 대체로 다음 패턴을 사용한다.

```text
flow.first()
→ 메모리에서 새 List 계산
→ dataStore.edit { 전체 JSON 저장 }
```

특히 시설은 관리자 저장과 Temi POI 자동 동기화가 동시에 발생할 수 있다.

가능한 상황:

```text
관리자 수정 ─┐
             ├→ 둘 다 같은 옛 목록을 읽음 → 마지막 저장이 앞 저장을 덮어씀
POI 동기화 ──┘
```

수정 목표:

- 최소한 `FacilityRepository`의 sync/update/delete가 서로의 변경을 조용히 덮어쓰지 않도록 직렬화 또는 원자적 read-modify-write 전략을 적용한다.
- 같은 문제가 다른 콘텐츠 Repository에도 실제 사용자 동시 작업으로 발생 가능한지 확인한다.
- 불필요하게 전체 저장 계층을 Room으로 변경하지 않는다.

## 4.2 `NOT_FOUND_ON_TEMI` 이용자 노출

`FacilityRepository.visibleFacilities`의 주석은 삭제 확인 대기 시설을 이용자에게 숨긴다고 설명하지만 실제 필터는 현재 다음 조건 위주다.

```text
isEnabled == true
floor != UNSET_FLOOR
```

따라서 `syncStatus == NOT_FOUND_ON_TEMI` 시설이 기존 enabled 상태라면 이용자 화면에 계속 나타날 가능성이 있다.

수정 목표:

- 주석/요구사항의 의도와 실제 이용자 노출 조건을 일치시킨다.
- 관리자 화면에서는 `NOT_FOUND_ON_TEMI`를 계속 확인할 수 있어야 한다.

## 4.3 `ESCORT_START` 통계 기록 시점

현재 시설 상세에서 사용자가 `동행 안내`를 클릭하면 확인 팝업 전에 `ESCORT_START`가 기록된다.

현재 흐름:

```text
동행 안내 클릭
→ FACILITY_REQUEST
→ ESCORT_START
→ 이동 확인 팝업
→ 사용자는 취소할 수도 있음
```

따라서 실제 이동을 시작하지 않아도 동행 시작 통계가 증가한다.

수정 목표:

- `FACILITY_REQUEST`는 요청 시점 의미를 유지한다.
- `ESCORT_START`는 실제 이동 시작을 사용자가 확인하고 `goTo()` 요청이 성공적으로 받아들여진 시점에 1회 기록한다.

## 4.4 통계 DB 기록 실패가 핵심 앱을 죽이지 않도록 처리

`StatsRepository.logEvent()` / `logQuizComplete()`의 Room insert 실패가 호출 coroutine으로 그대로 전파될 수 있다.

통계는 부가 기능이므로 DB 손상, 저장공간 문제 등의 예외 때문에 시설 안내/로봇 이동 앱 전체가 종료되는 것은 피해야 한다.

수정 목표:

- 통계 insert 실패를 안전하게 격리한다.
- 실패했다고 가짜 성공 통계를 만들지는 않는다.
- 필요하면 Log로 진단 가능하게 하되 UI/주행 본 기능은 계속 동작하도록 한다.

---

# 5. 관리자 및 저장 데이터 안정화

## 5.1 기본 데이터 `ensureSeeded()` 초기화 여부 분리

### Kids

`KidsContentRepository.ensureSeeded()`는 퀴즈/책/예절 세 목록이 모두 비면 다시 기본값을 채운다. 관리자가 세 종류를 모두 의도적으로 삭제한 경우 다음 `KidsMenu` 진입 시 기본 데이터가 부활할 수 있다.

최초 실행 여부를 목록의 현재 empty 상태와 분리하는 방식을 검토한다.

### Usage 주의

`UsageRepository`도 empty 여부로 시드를 판단하지만 현재 관리자 UI에서는 기본 부모 카테고리가 남기 때문에 하위 항목을 전부 삭제하는 것만으로 Kids와 동일하게 재현된다고 단정하지 않는다.

현재 UI/저장 구조를 다시 확인한 뒤 정말 필요한 범위만 수정한다.

## 5.2 행사 날짜 검증

현재 시작일은 `yyyy-MM-dd` 모양 정규식만 검사하고 실제 존재 날짜인지 확인하지 않는다. 종료일은 그보다 검증이 더 약하다.

문제 예:

- `2026-13-45`
- 종료일 `미정`
- 종료일이 시작일보다 과거

`ResolveEventListUseCase`는 ISO 날짜 문자열의 사전순 비교를 전제로 한다.

수정 목표:

- 저장되는 startDate/endDate가 비교 가능한 유효한 날짜라는 전제를 보장한다.
- endDate가 있다면 startDate보다 앞설 수 없도록 검토한다.

## 5.3 관리자 저장 더블클릭

신규 행사/공지/퀴즈/책/예절/이용정보 등에서 `System.currentTimeMillis()` 기반 ID 생성과 비동기 저장을 사용한다.

저장 버튼을 매우 빠르게 두 번 누르면 동일 내용이 중복 생성될 수 있는지 확인하고, 확인되면 저장 진행 중 재호출을 차단한다.

## 5.4 백업 import 유효성 검증

현재 `BackupRepository`는 export 시 `version`을 저장하지만 import에서 이를 실질적으로 검증하지 않는다.

또 import는 관리자 설정 화면의 유효성 검사를 거치지 않고 설정을 직접 복원한다.

확인할 값:

- `idleTimeoutSeconds > 0`
- `volume` 유효 범위
- `featuredFacilityCount` 허용값
- 기타 운영에 필요한 필수 조건
- backup version 호환성

손상된 백업 때문에 무입력 timeout이 0/음수가 되어 화면 진입 즉시 홈으로 복귀하는 등의 상태를 막는다.

## 5.5 부분 백업 복구

현재 여러 Repository를 순차적으로 갱신하기 때문에 import 중 예외 또는 프로세스 종료 시 일부 데이터만 새 값으로 바뀔 가능성이 있다.

대규모 저장구조 변경 없이 위험을 줄일 수 있는 방법을 검토하고, 구조상 완전한 원자성을 보장하기 어렵다면 그 사실과 현실적인 대응책을 보고한다.

## 5.6 삭제 후 dangling reference

다음 연결을 확인한다.

```text
QuizQuestion.recommendedBookIds → RecommendedBook
UsageTopic.relatedFacilityId    → Facility
LibraryEvent.relatedFacilityId  → Facility
```

연결된 원본을 삭제한 뒤 존재하지 않는 ID가 계속 저장되는 문제를 점검한다. 현재 `mapNotNull`/`firstOrNull`로 크래시는 방지하지만 데이터 무결성 관점에서 정리가 필요한지 판단한다.

---

# 6. 기능 완성/후순위

아래는 핵심 버그 안정화 이후 처리한다.

## 6.1 관리자 음량 설정

현재 `LibrarySettings.volume`은 DataStore/관리자 UI/백업에는 연결되어 있지만 실제 Temi TTS 음량에 사용되는 코드가 확인되지 않았다.

Temi SDK에서 앱이 안전하게 제어 가능한 음량 API가 실제 사용 SDK 버전(1.137.1)에 존재하는지 먼저 확인하고, 확실한 API가 있을 때만 연결한다. SDK API를 추측해서 작성하지 않는다.

## 6.2 어린이자료실 목적지 결정

`ChildrenFacilityLookup`에서 시설 이름에 `"어린이"`가 포함됐는지를 기반으로 첫 시설을 선택하는 로직을 검토한다.

`장애인·어린이 화장실` 같은 시설이 먼저 잡혀 잘못된 실제 목적지로 연결될 가능성이 있으므로, 명시적인 시설 연결 방식이 더 안전한지 검토한다.

## 6.3 추천도서 통계 필터

`BookRecommendationViewModel`의 최초 BOOK_VIEW 통계가 ageGroup/topic 초기 필터를 반영하지 않는지 확인한다. 현재 실제 호출 경로에서는 필터 파라미터를 넘기지 않는다면 잠재 문제로 분류하고 불필요한 수정은 하지 않아도 된다.

## 6.4 아직 UI와 연결되지 않은 필드

- `Facility.directionText`
- `Facility.mapImagePath`
- `RecommendedBook.coverPath`

현재 프로젝트에서 확장용으로 의도적으로 남긴 것인지 확인한다. 핵심 버그 수정 단계에서는 임의로 기능을 추가하지 않는다.

---

# 7. 수정 권장 순서

## 1차: 실제 로봇 안정성

1. 중복 `goTo()` 방지
2. `onConfirmStart()` 재진입 방지
3. `goTo()` false 처리
4. stop timeout 연결
5. navigation timeout 최신 요청 기준으로 정리
6. navigation terminal state 소비 방식 정리
7. permission timeout 연결

## 2차: POI/통계 정확성

8. Facility 동시 read-modify-write 경쟁조건
9. `NOT_FOUND_ON_TEMI` 이용자 노출 차단
10. `ESCORT_START` 기록 시점
11. 통계 DB 예외 격리

## 3차: 관리자 데이터 안정성

12. Kids 초기 seed 여부 분리
13. 날짜 검증
14. 저장 중복 클릭
15. 백업 import 유효성/version 검증
16. 부분 복구 위험 검토
17. dangling reference 검토

## 4차: 후순위

18. 실제 Temi 음량 연결 가능 여부
19. 어린이자료실 명시적 목적지 연결
20. 추천도서 통계 초기 필터
21. 미연결 확장 필드

---

# 8. 검증 원칙

수정 후 최소한 다음을 확인한다.

### 로봇 이동

- 이동 확인 버튼 연속 클릭에도 `goTo()` 1회
- 정상 이동: Requested → Moving → Arrived
- 사용자 중지: Requested/Moving → USER_STOPPED
- 외부 중단: EXTERNAL_INTERRUPTION
- goTo 콜백 누락: timeout
- stop 콜백 누락: stop timeout
- 이전 이동의 Arrived/Failed가 다음 이동에 재처리되지 않음

### POI

- POI 동기화와 관리자 저장이 겹쳐도 관리자 수정이 사라지지 않음
- 신규 POI는 관리자 설정 전 이용자에게 노출되지 않음
- Temi에서 사라진 POI는 관리자에게는 확인 가능하지만 이용자에게는 노출되지 않음

### 통계

- 확인창에서 취소하면 ESCORT_START 증가하지 않음
- 실제 이동 시작 시 정확히 1회 증가
- 도착/실패/취소 통계가 이동 1회당 중복되지 않음
- 통계 DB 오류가 발생해도 핵심 앱이 종료되지 않음

### 관리자/데이터

- 의도적으로 삭제한 Kids 콘텐츠가 자동 부활하지 않음
- 잘못된 날짜 저장 차단
- 빠른 저장 연속 클릭에도 중복 항목 없음
- 잘못된 백업 설정값이 앱을 사용할 수 없는 상태로 만들지 않음

---

# 9. 작업 완료 보고 형식

수정 완료 후 다음 형식으로 보고한다.

1. 실제로 문제가 확인된 항목
2. 지적과 달라 수정하지 않은 항목 및 이유
3. 수정한 파일 목록
4. 각 파일에서 변경한 핵심 내용
5. 기존 기능에 영향이 없도록 유지한 부분
6. 수행한 테스트/빌드 결과
7. 실제 Temi에서 반드시 확인해야 하는 항목
8. 남아 있는 위험 또는 후순위 항목

한 번에 대규모 리팩터링하지 말고 위 우선순위 순으로 최소 수정한다.
