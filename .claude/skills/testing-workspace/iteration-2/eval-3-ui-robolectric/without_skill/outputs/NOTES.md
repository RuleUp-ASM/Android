# ChallengeDetailScreen UI 테스트 — 작업 노트

## 어느 층을 골랐나

**UI 층(Robolectric + Compose)** 을 골랐고, 대상은 화면 전체가 아니라 상태만 받는
`ChallengeDetailContent` 다.

- `ChallengeDetailScreen` 자체는 `hiltViewModel()` 로 ViewModel 을 직접 만들고, `LaunchedEffect` 로
  Load 를 쏘고, `LifecycleEventEffect` 두 개로 재진입을 처리한다. 이 화면을 통째로 띄우려면 상태
  조합마다 Hilt 컴포넌트와 가짜 repository 를 세워야 하는데, 그러면 "상태 → 그림" 이라는 이 층의
  검증 대상이 DI 설정에 가려진다.
- 반대로 그리기 규칙을 순수 함수로 뽑아 케이스 층에서만 검증하면(기존 `AppealSheetTest`·
  `TodayVerificationCopyTest` 방식) **분기 자체**는 검증되지 않는다. 이 화면의 위험은 문구 계산이
  아니라 `when` 분기다 — 로딩 · 실패 · 방 홈 · 공개 상세 네 갈래, 하단 CTA 세 갈래, 가입 차단 8종,
  피드·랭킹의 빈/실패/목록 상태가 전부 조건식 하나에 걸려 있다. 그래서 실제로 렌더해 확인한다.
- 다만 조합 폭발을 UI 로 끌고 가지 않으려고, `missingPermissionTokens()` 처럼 축이 여럿인 계산은
  Robolectric 없는 순수 JVM 테스트(`MissingPermissionTokensTest`)로 따로 고정했다.

## 세운 테스트 인프라 (이 레포에 Robolectric 이 처음 들어온다)

레포 전체를 뒤졌지만 Robolectric·`createComposeRule` 사용처가 없었다. 그래서 다음을 새로 만들었다.

| 파일 | 변경 |
|---|---|
| `gradle/libs.versions.toml` | `robolectric = "4.16"` 버전 + 라이브러리 별칭 추가 |
| `challenge/presentation/build.gradle.kts` | `testOptions.unitTests.isIncludeAndroidResources = true`, `testImplementation`(robolectric · compose BOM · `ui-test-junit4`), `debugImplementation(ui-test-manifest)` |
| `challenge/presentation/src/test/resources/robolectric.properties` | `sdk=34`, `qualifiers=w411dp-h891dp-xhdpi` |
| `.../detail/ChallengeDetailScreen.kt` | `private fun ChallengeDetailContent` → `internal` **(유일한 프로덕션 변경)** |
| `.../test/.../ChallengeDetailFixtures.kt` | 상태 픽스처 |
| `.../test/.../ChallengeDetailTestHost.kt` | 렌더 헬퍼 · 의도 수집기 · 클릭 가드 우회 |

세부 근거:

- **`ui-test-manifest` 는 `debugImplementation`**. `createComposeRule()` 은 `ComponentActivity` 를 띄우는데
  그 선언이 이 매니페스트에만 있고, 유닛 테스트는 debug 변형의 병합 매니페스트를 읽는다.
- **화면 크기 지정(`qualifiers`)**. Robolectric 기본 화면(320dp)에서는 하단 고정 CTA·멤버 섹션이
  화면 밖으로 밀려 "안 보임" 이 오탐이 된다.
- **`sdk=34`**. compileSdk(37)를 따라갈 이유가 없다 — 화면에 SDK 분기가 없고, Robolectric 이 아직
  받지 못하는 SDK 를 쓰면 전 테스트가 통째로 깨진다.
- **클릭 가드(가장 아픈 함정).** `singleClickable` 은 `SystemClock.elapsedRealtime()` 로 연타를 막는데
  Robolectric 의 시계는 멈춰 있다. 그대로 두면 `0 - 0 < 300` 이라 **첫 클릭부터** 전역 가드에 먹혀
  모든 클릭 테스트가 조용히 실패한다. 게다가 `SingleClickGuard` 의 마지막 클릭 시각은 static 이라
  테스트 경계를 넘어 살아남으므로, 시계만 리셋되면 다음 테스트의 첫 클릭이 "과거"로 읽혀 또 막힌다.
  그래서 되돌아가지 않는 커서를 두고 매 클릭 전에 그 앞으로 시계를 민다(`clickText`).
- **시트·다이얼로그는 `awaitText`** 로 등장을 기다린다. `ModalBottomSheet`·`AlertDialog`·`DropdownMenu`
  는 애니메이션이 끝나야 붙어서 `waitForIdle` 직후 조회하면 "없다" 로 잘못 실패한다.

### `private` → `internal` 로 바꾼 것에 대해

프로덕션 변경은 이 한 줄뿐이다. 상태만 받는 stateless content 를 `internal` 로 여는 것은 이 레포의
다른 컴포넌트(`RoomInfoTab`·`RoomFeedTab` 등 전부 `internal`)와도 일관된다. 이걸 피하려면 스크린을
ViewModel 채로 띄워야 하는데, 그쪽이 훨씬 큰 변경이다.

## 경로를 어떻게 열거했나

`ChallengeDetailScreen.kt`(935줄)와 그 아래 컴포넌트 7개(`RoomHeader`·`RoomInfoTab`·`RoomFeedTab`·
`RoomRankingTab`·`RoomSections`·`WatcherSection`·`VerificationResultModal`)를 읽고 **분기를 만드는
조건식만** 뽑아 상태 축으로 되돌렸다.

1. 화면 골격: `isLoading` → `detail == null` → `room != null` → 공개 상세, 4갈래.
2. 상단바: `detail != null && room != null` 이면 `RoomAppBar`(메뉴는 `myRole.isOwner` 일 때만), 아니면 `DetailTopBar`.
3. 오버레이: 권한 배너(`!isLoading && room != null && 꺼진 토큰 있음`), 판정 결과 모달
   (`todayResult.unacknowledged != null && !isLoading && !resultAcknowledged`).
4. 하단 CTA: `myRole.isMember` → 없음 / `cloneable` → 복제 버튼(+`isCloning`) / `hideJoinButton` →
   안내 문구 / 그 외 → 참여 버튼(+`isJoining`).
5. 시트·다이얼로그: `joinBlock.reason` 8종 + 미지값, `MemberConfirm` LEAVE·DELETE.
6. 방 3탭: `selectedTab` 3갈래 × (피드: 첫로딩·빈+에러·빈+봇방장·빈+방장·빈+멤버·목록·이어받기 실패,
   랭킹: 세그먼트 2 × 로딩·실패·빈·목록).
7. 각 분기마다 **"그려졌나"** 와 **"어떤 의도를 올렸나"** 를 함께 본다 — 버튼이 보이는데 아무것도
   올라가지 않는 회귀가 이 화면에서 실제로 있었다(`myRole` 로 CTA 를 판단하게 된 경위).

케이스는 축 하나만 바꾸는 원칙으로 뽑았다. `roomState()` 같은 "막히지 않는 평범한 방" 을 기본으로 두고
테스트마다 필드 하나만 흔든다.

## 일부러 테스트하지 않은 것

- **CTA 문구 산출 로직**(`DetailSetupAction` 계산). `ChallengeDetailScreen` 안의 인라인 `when` 이라
  ViewModel 없이 부를 수 없다. 본문은 `ctaLabel` 을 파라미터로 받으므로 "받은 문구를 그린다" 까지만
  검증했다. 이 계산을 `internal fun detailSetupAction(state, permissionGranted)` 로 뽑으면 순수 함수
  테스트가 되지만, 요청 범위를 넘는 리팩터링이라 하지 않았다. **후속 작업 후보다.**
- **권한 바텀시트**(`PermissionBottomSheet`)와 그 안의 런타임/사용정보/헬스 3분기. 스크린 로컬 상태
  (`showPermissionSheet`)로만 열리고 `rememberPermissionRequester`·`healthConnectAvailable` 등 OS 의존이
  붙어 있다. 여기는 Robolectric 보다 실기기 인수 테스트(`VERIFICATION_TEST_PLAN.md`) 쪽이 맞다.
- **감시자 카카오톡 공유**(`ShareWatcherInvite` 이펙트). 카카오 SDK 로 나가는 경로라 UI 층 밖이다.
  화면이 `InviteWatcher` 의도를 올리는 데까지만 본다.
- **이의 시트 제출 흐름**. `AppealSheetTest` 가 이미 문구·카운터를 케이스 층에서 고정하고 있어
  겹치지 않게 진입점(실패 카드에 버튼이 뜨는지)까지만 다뤘다.
- **페이징 트리거**(`snapshotFlow` + `visibleItemsInfo`). 스크롤 위치에 의존해 Robolectric 화면 크기에
  결과가 묶인다 — 화면 크기를 바꾸면 깨지는 테스트라 넣지 않았다. 대신 페이징 **결과 상태**
  (하단 스피너·이어받기 실패 행)는 검증한다.
- **날짜에 따라 답이 바뀌는 문구**. `feedDateHeader`/`rankingUpdatedLabel` 은 오늘 날짜에 의존하므로
  "오늘"·"어제" 같은 고정 결과만 쓰고, 나머지는 부분 문자열(`매일 1회 갱신`)로만 확인한다.

## 확인하지 못한 것

지시대로 gradle 을 돌리지 않았다. 따라서 다음은 **미검증**이다.

- Robolectric `4.16` 이 AGP 9.2.1 / compileSdk 37 조합에서 도는지. 안 되면 버전만 올리면 된다.
- `ModalBottomSheet`(가입 차단 시트)가 Robolectric 에서 idle 로 떨어지는지. `awaitText` 로 방어했지만
  여기서 막히면 그 시트 테스트만 문구 단위(케이스 층)로 내리는 게 대안이다.
