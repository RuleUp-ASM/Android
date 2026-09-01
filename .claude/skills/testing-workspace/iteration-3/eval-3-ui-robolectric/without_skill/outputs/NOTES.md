# ChallengeDetailScreen UI 테스트 — 작업 노트

## 어느 층을 골랐나, 왜

**Robolectric 위에서 실제 Compose 트리를 그리는 UI 테스트**를 골랐다 (`challenge/presentation/src/test/`).

- 이 화면의 고유 로직은 ViewModel 이 아니라 **Composable 본문**에 있다. `ChallengeDetailScreen.kt` 안에서
  `setup`(서버) + `permissions`(OS) + `targetAppsRegistered`(로컬)를 합쳐 CTA 사다리
  (`GRANT_PERMISSION → REGISTER_APPS → REGISTER_ANCHOR → JOIN`)를 고르고, 라벨 문자열까지 여기서 만든다.
  ViewModel 테스트로는 이 계산에 손이 닿지 않는다.
- 같은 이유로 `ChallengeDetailContent`·`JoinBlockedSheet`·`PermissionBottomSheet` 는 전부 `private` 이라
  중간 진입점이 없다. 유일한 공개 seam 은 `ChallengeDetailScreen(challengeId, modifier, viewModel)` 이다.
- **ViewModel 은 진짜를 쓰고 서버·OS 경계만 대역으로 갈았다.** `ChallengeDetailViewModel` 은 `final` 이라
  가짜로 대체할 수도 없거니와, 대체하면 "인텐트 → 리듀서 → 상태 → 화면"이라는 이 화면의 유일한 배선을
  건너뛰게 된다. 대신 11개 협력자(repository 5 · store · token · permission provider · nav · observability · TTI)를
  전부 가짜로 넣었다.
- 순수 JVM 테스트로 뺄 수 있었던 건 `missingPermissionTokens()` 하나뿐이라 따로 층을 만들지 않았다 —
  UI 테스트가 그 함수의 세 갈래(허용/거부/미조회)를 모두 지난다.

## 새로 세워야 했던 테스트 인프라

레포에 Robolectric·Compose UI 테스트가 **하나도 없었다**(기존 `*ScreenTest` 는 전부 순수 문자열 헬퍼 테스트).
그래서 다음을 새로 만들었다.

| 파일 | 내용 |
|---|---|
| `gradle/libs.versions.toml` | `robolectric = "4.16"` 버전 + 라이브러리 별칭 추가 (`ui-test-junit4`·`ui-test-manifest`·`androidx-junit` 은 이미 카탈로그에 있었다) |
| `challenge/presentation/build.gradle.kts` | `testOptions { unitTests.isIncludeAndroidResources = true }` — `painterResource(ic_arrow_back)` 때문에 필수 · `testImplementation` 3종 · `debugImplementation(ui-test-manifest)` (Robolectric 유닛 테스트는 **debug 변형의 머지된 매니페스트**를 쓰므로 `testImplementation` 으로는 `ComponentActivity` 가 안 잡힌다) |
| `src/test/resources/robolectric.properties` | `sdk=34`. 이 모듈 `compileSdk` 는 37 이라 지정하지 않으면 android-all 이 없어 전부 깨진다 |
| `ChallengeDetailFixtures.kt` | 도메인 엔티티 픽스처(상세·셋업·방·랭킹·권한 스냅샷) |
| `ChallengeDetailFakes.kt` | 협력자 대역 + `ChallengeDetailEnv`(ViewModel 조립기). 화면이 부르지 않는 메서드는 `TODO()` 로 둬서, 나중에 그 경로를 타면 조용히 통과하지 않고 터지게 했다 |
| `ChallengeDetailTestHost.kt` | 화면 세우기 + **연타 가드 우회 헬퍼** |

### 함정 하나: 전역 연타 가드

디자인 시스템의 모든 클릭은 `singleClickable` 이고 그 뒤에 프로세스 전역 `SingleClickGuard`(300ms)가 있다.
`SystemClock.elapsedRealtime()` 를 쓰는데 Robolectric 은 테스트마다 시계를 0 근처로 되돌리므로,
(1) 첫 클릭이 `now(≈100) - last(0) < 300` 에 걸려 삼켜지고, (2) 두 번째 테스트부터는 이전 테스트가 남긴
`lastGlobalClickTime` 보다 시계가 **뒤로** 가 있어 계속 삼켜진다. `tapText`/`tapIcon` 이 클릭 전에
루퍼 시계를 JVM 전역 하한 위로 2초씩 밀어 창을 연다. 이걸 모르면 클릭 테스트가 전부 조용히 무반응이다.

## 경로를 어떻게 열거했나

1. `ChallengeDetailScreen.kt` 의 분기를 그대로 훑었다 — `action` when(4갈래) × 그 앞의 가드
   (`detail == null`, `setup == null`, `setup.manual`), 렌더 when(로딩/에러/방/공개상세),
   하단 CTA 조건(`myRole.isMember`, `hideJoinButton`, `cloneable`), 권한 배너 조건,
   `JoinBlockedSheet` 의 `when(reason)` 7갈래, `PermissionBottomSheet` 의 요청 경로 3갈래.
2. 각 분기를 만드는 **입력의 출처**를 거슬러 올라가 무엇을 대역으로 바꿔야 그 칸에 도달하는지 정리했다
   (예: `REGISTER_ANCHOR` 는 setup 두 플래그 + TargetAppStore 상태를 동시에 맞춰야 도달한다).
3. 코드 주석이 명시한 **회귀**를 따로 케이스로 세웠다 — "room 으로 참여 여부를 판단하면 안 된다",
   "권한 미조회를 차단으로 접지 마라", "setup 실패가 곧 차단이 되면 안 된다", "0% 로 접지 마라".

## 기댓값의 출처

전부 **프로덕션 코드의 문자열 리터럴과 픽스처의 산술**이다. 지어낸 값은 없다.

- CTA 라벨 4종·차단 시트 문구 7종·권한 라벨/고지 문구: `ChallengeDetailScreen.kt` 리터럴.
- `"규칙왕 · 4명 참여 중"`, `"4 / 10명"`, `"2026-08-01 ~ 2026-08-31"`: `DetailHero`/`DetailInfoCard` 의
  포맷 + 픽스처 값.
- `"D-10"`, `"75%"`, `"-"`: `RoomInfoHeader` + `Double.toPercentText()`(0.75 → `"75"`), `remainingDays=10`.
- 빈 상태 문구: `RoomFeedTab.FeedEmptyState`, `RoomRankingTab.memberRanking`.
- 라우트 상수: `AppRoutes`, `ChallengeTargetsPage`.

## 일부러 테스트하지 않은 것

- **로딩 스피너 상태.** `CircularProgressIndicator` 는 무한 애니메이션이라 `waitForIdle()` 을 붙잡는다.
  검증하려면 `mainClock.autoAdvance = false` 로 전체 스위트의 시간 제어 방식을 바꿔야 해서, 값에 비해
  값싸지 않다고 판단했다.
- **실제 OS 권한 요청**(`rememberPermissionRequester().request()` → `ActivityResultLauncher`)과
  **Health Connect 진입**. 결과를 Robolectric 에서 흉내 내려면 `ShadowActivityResultRegistry` 까지
  들어가야 하고, 그 시점부터는 우리 코드가 아니라 프레임워크를 테스트한다. "허용하기 버튼이 존재하고,
  경로가 다른 권한은 다른 버튼으로 갈린다"까지만 지킨다.
- **이의 제기 시트·판정 결과 모달·감시자 섹션·멤버 관리 섹션.** 각각 자기 컴포넌트 파일이 있고,
  `VerificationResultModalTest`·`AppealSheetTest` 처럼 이미 문구 단위 테스트가 있다. 이 화면 테스트는
  "그 섹션을 언제 붙이는가"(watchers 조회 성공 시에만 등)까지가 몫이고, 섹션 내부는 그쪽 몫이다.
- **피드/방 순위 페이징.** `LazyColumn` 의 `visibleItemsInfo` 에 기대는 스크롤 트리거라 Robolectric 의
  고정 화면 크기에서 재현이 불안정하다. 커서 규칙 자체는 `ChallengeDetailState.canLoadMoreThreads` 로
  가둬져 있어 모듈 층에서 다루는 쪽이 맞다.
- **관측 이벤트.** 테스트에서는 `Policy` 를 꺼 페이로드가 만들어지지도 않게 했다 — 이벤트 카탈로그가
  바뀔 때 화면 테스트가 덩달아 깨지면 안 된다.

## 확인하지 못한 것 (gradle 실행 금지 조건)

- 실행해 보지 않았다. 특히 `ModalBottomSheet`(권한·차단 시트)는 Robolectric 에서 애니메이션 정착이
  까다로운 편이라, 처음 돌릴 때 `ChallengeDetailSheetTest` 만 손봐야 할 수 있어 파일을 따로 뒀다.
- Robolectric `4.16` / `sdk=34` 조합은 실제 다운로드 가능 여부를 확인하지 못했다.
