# NOTES — ChallengeDetailScreen UI 테스트

## 고른 층: UI (Robolectric + Compose)

"이 규칙이 깨지면 어느 파일을 고치나"로 골랐다. 대상 규칙들이 전부 `ChallengeDetailScreen.kt` 안에서
갈라진다 — 로딩/실패/공개 상세/방 상세의 `when`, 권한 배너 조건, 판정 모달 노출 조건, 하단 CTA 노출·잠금
조건, 방장 메뉴 구성. 화면을 고쳐야 하는 규칙이므로 화면 층이다.

경계는 이렇게 잡았다.

- **문구를 만들어내는 계산은 안 본다.** 실패 사유 문장·이의 마감일·연속 기록 문구는
  `VerificationResultModalTest`·`AppealSheetTest`(케이스 층)가 이미 본다. UI 는 "그 문구가 보이느냐"만.
- **화면 이동은 안 본다.** 화면은 Intent 만 올린다 — 목적지는 ViewModel 층 몫이다.
- **색·간격·폰트는 안 본다.** 디자인 시스템 토큰을 쓴다는 건 코드가 이미 말한다.

## 세운 실행 기반 (레포에 Compose UI 테스트가 하나도 없었다)

| 파일 | 무엇을 |
|---|---|
| `gradle/libs.versions.toml` | `robolectric = "4.16"` + 라이브러리 항목. 동적 버전 안 씀 |
| `challenge/presentation/build.gradle.kts` | `testOptions.unitTests.isIncludeAndroidResources = true`, robolectric·compose BOM·`ui-test-junit4` (test), `ui-test-manifest` (**debug**) |
| `challenge/presentation/src/test/resources/robolectric.properties` | `sdk=34` (모듈은 compileSdk 37 이라 그대로 두면 `android-all` jar 를 못 받는다), `qualifiers=w411dp-h891dp-xhdpi` |

두 가지는 스킬 참조 문서(`references/ui.md`)와 다르게 했다.

1. **`ui-test-manifest` 는 `testImplementation` 이 아니라 `debugImplementation`.** `createComposeRule()` 이
   띄우는 `ComponentActivity` 는 **병합 매니페스트**에 있어야 하는데, 유닛 테스트는 debug 변형의 병합
   매니페스트를 읽는다. `testImplementation` 으로 넣으면 클래스패스에는 들어가도 매니페스트에는 안 들어간다.
2. **`qualifiers` 를 함께 고정했다.** Robolectric 기본 화면은 좁아서 스크롤 아래 내용이 "보이지 않음"으로
   잡힌다. 폰 크기로 맞춰 `assertIsDisplayed` 가 레이아웃 우연에 흔들리지 않게 했다.

### 프로덕션 변경 1건 (테스트 가능성)

`ChallengeDetailScreen.kt` 의 `ChallengeDetailContent` 를 `private` → `internal`. 상태를 인자로 받고 Intent 를
콜백으로 올리는 형태로 **이미** 나뉘어 있어 그 밖의 변경은 없다. 바깥 `ChallengeDetailScreen` 은
`hiltViewModel()` 을 꺼내므로 테스트 대상이 아니다.

### 밟은 지뢰 하나 — 전역 클릭 가드

`RuleUpPrimaryButton`·`singleClickable` 은 `SingleClickGuard`(전역 300ms)를 거친다. 그 시각은 JVM 전역
정적인데 Robolectric 은 테스트마다 `SystemClock` 을 초기값(≈100ms)으로 되돌린다. 그대로 두면 **첫 클릭부터**
`100 - 0 < 300` 으로 삼켜지고, 실패는 "버튼이 안 눌렸다"가 아니라 "의도가 안 올라왔다"로만 나타나 원인을
찾기 어렵다. `@Before` 에서 시계를 앞선 테스트보다 확실히 앞으로 밀고(60초씩 누적), 한 테스트에서 두 번
누를 때는 `clickPastGuard()` 로 1초씩 더 민다. 프로덕션 코드에 리셋 훅을 뚫지 않으려고 이렇게 했다.

## 경로를 어떻게 열거했나

출처를 코드에 뒀지 기억에 두지 않았다.

- `ChallengeDetailContent` 의 `when` 4갈래(`isLoading` / `detail == null` / `room != null` / 나머지) — 각각 하나.
- 조건부로 얹히는 블록 4개(권한 배너 · 판정 모달 · 하단 CTA · 방 상단바 메뉴) — 각 블록의 **불리언 곱을
  다 펴지 않고**, 그 블록이 "생기는 이유"와 "안 생기는 이유"만 하나씩. 판정 모달은 세 조건이 각각 독립적으로
  모달을 없애므로(로딩 중 · 이미 확인 · 미확인 없음) 앞의 둘을 따로 뒀다.
- `errorMessage` 는 nullable → 있음/없음 두 갈래.
- `RoomTab` 은 enum 3종이지만 화면이 갈리는 건 "정보냐 아니냐"(헤더 노출)뿐이라 그 경계만. 탭 전환은
  의도가 올라가는지 하나로 끝냈다 — 어느 탭이 무엇을 그리는지는 각 탭 컴포넌트의 몫이다.
- 상태 → 잠금은 `isJoining` · `isCloning` 두 개(`assertIsNotEnabled`).

총 20개. 층을 건너뛴 검증(예: 정원 숫자 검사, 실패 사유 문장 조립)은 넣지 않았다.

## 일부러 테스트하지 않은 것

- **가입 차단 시트(`JoinBlockedSheet`)의 사유 8종 문구** — `when` 이 private Composable 안에 있고 시트가
  `ModalBottomSheet` 다. 대응표를 순수 함수로 꺼내 케이스 층에서 보는 편이 훨씬 싸다.
- **멤버 확인 다이얼로그(탈퇴·삭제)** — 여는 조작이 방 멤버 섹션 깊숙이 있어 도달 비용이 크다.
- **권한 바텀시트와 CTA 의 다음 단계 결정(`DetailSetupAction`)** — 둘 다 `hiltViewModel()` 을 꺼내는 바깥
  Composable 에 있다. 상태만으로 띄울 수 없다.
- **런타임 권한 다이얼로그·지오펜스·WorkManager·딥링크 진입** — Robolectric 이 못 흉내낸다. 계측 워크플로가
  생기기 전까지는 `VERIFICATION_TEST_PLAN.md` 의 수동 절차가 대신한다.

전부 `TEST_STRATEGY.md` 3절(미검증)에 왜 안 했는지와 함께 올렸다.

## 남은 것

- `./gradlew :challenge:presentation:testDebugUnitTest --tests "*ChallengeDetailScreenTest*"` 미실행(지시에 따라
  gradle 을 돌리지 않았다). 첫 실행은 `android-all` jar 를 받느라 느리다.
- `TEST_STRATEGY.md` 2절 표는 이 PR 의 파일이 실제 트리에 들어간 뒤 `coverage_map.py` 를 다시 돌려 갱신해야
  한다(지금 표는 스크립트 출력에 이 파일의 20개를 반영해 적은 값이다).
- 3절 미검증 목록은 이번에 직접 만난 구멍만 적었다. `--gaps` 전체를 "메울 것/안 메울 것"으로 가르는 일은
  사용자와 함께 해야 한다.
