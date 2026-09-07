# ChallengeDetailScreen UI 테스트 — 작업 노트

## 어느 층을 골랐나

**UI 층(Robolectric + Compose 테스트)**. 이유는 검증 대상이 "상태 → 화면" 매핑이기 때문이다.

`ChallengeDetailScreen` 이 실제로 결정하는 것은 계산이 아니라 **분기**다 — 로딩/실패/공개 상세/방 홈
어느 쪽을 그릴지, 하단 CTA·복제 버튼·감시자 섹션·권한 배너를 낼지 말지, 어떤 탭의 어떤 빈 상태를
띄울지. 이건 순수 함수로 뽑아낼 수 없는(=Composable 안의 `when`·`if` 로만 존재하는) 로직이라
케이스 층으로 내릴 수 없고, ViewModel 층에서 잡으면 "상태가 이렇게 나온다"까지만 확인되고
그 상태가 화면에 실제로 어떻게 나타나는지는 검증되지 않는다.

계기 판단(`instrumented` androidTest)이 아니라 **JVM(Robolectric)** 으로 간 이유:
이 화면의 분기는 전부 상태 입력으로 재현되고 실기기 고유 자원(GPS·카메라·Health Connect)에
의존하지 않는다. CI(`./gradlew test`)에서 그대로 돌아야 회귀가 잡힌다.

반대로 **문구·날짜 계산은 UI 층에서 다시 세지 않았다.** `TodayVerificationCopyTest`,
`VerificationResultModalTest`, `AppealSheetTest` 가 이미 순수 함수로 지키고 있어서, 같은 문장을 두 층에서
검증하면 문구를 한 번 고칠 때마다 두 곳이 깨진다. UI 테스트는 "그 문구가 나오는 분기가 선택됐는가"만 본다.

## 새로 세운 테스트 인프라

이 레포에는 Compose UI 테스트가 **하나도 없었다**(`presentation` 모듈의 기존 `*Test.kt` 는 전부
화면에서 뽑아낸 순수 함수 테스트다). 그래서 아래를 처음 만들었다.

| 파일 | 내용 |
|---|---|
| `gradle/libs.versions.toml` | `robolectric = "4.16"` 버전과 라이브러리 별칭 추가. `androidx-compose-ui-test-junit4` · `-test-manifest` 별칭은 **이미 카탈로그에 선언만 되어 있고 아무 모듈도 쓰지 않던 것**이라 그대로 재사용했다. |
| `challenge/presentation/build.gradle.kts` | `testOptions.unitTests.isIncludeAndroidResources = true`(드로어블 없이 그리면 `painterResource` 에서 죽는다) + 테스트 의존성. `ui-test-manifest` 는 `createComposeRule()` 이 띄우는 `ComponentActivity` 를 등록하므로 `debugImplementation` 이다. |
| `challenge/presentation/src/test/resources/robolectric.properties` | `sdk=34` 고정(=명시하지 않으면 `targetSdk` 추론에 끌려다닌다), `qualifiers=w411dp-h891dp-xhdpi`. 기본 화면(320x470dp)에서는 상세가 세로로 잘려 하단 CTA 가 화면 밖으로 밀린다. |
| `ChallengeDetailTestHost.kt` | 테마 래핑 + 인텐트 레코더 + **클릭 가드 해제 헬퍼**. |
| `ChallengeDetailFixtures.kt` | 도메인 엔티티 빌더(필드가 20개 넘는 `ChallengeDetail` 등). 화면이 읽는 값만 인자로 열었다. |

프로덕션 코드 변경은 **한 줄**이다: `ChallengeDetailContent` 의 가시성 `private` → `internal`.
공개 진입점 `ChallengeDetailScreen` 은 `hiltViewModel()` 로 ViewModel 을 스스로 만들고 진입 즉시
`Load` 인텐트를 쏘므로, 화면째 그리려면 Hilt 컴포넌트와 네트워크 스택을 세워야 한다. 상태별 렌더는
전부 상태를 인자로 받는 조립부에 들어 있어 그 경계를 여는 편이 싸다.

### 이 레포에서만 걸리는 함정 두 가지 (테스트에 주석으로 남겼다)

1. **`RuleUpTheme.colors` 는 `staticCompositionLocalOf { error(...) }` 다.** 테마로 감싸지 않고 그리면
   렌더가 예외로 죽는다. 호스트 헬퍼가 항상 `RuleUpTheme { }` 로 감싼다.
2. **`Modifier.singleClickable` 의 전역 연타 가드는 `SystemClock.elapsedRealtime()` 을 본다.**
   Robolectric 의 시계는 멈춰 있어서(초기값이 가드 임계값 300ms 보다 작다) 그냥 두면 **첫 클릭부터**
   삼켜진다 — `performClick()` 은 성공하는데 콜백이 안 불려 원인을 찾기 어렵다. 그래서 모든 클릭은
   `performGuardedClick()`(= `ShadowSystemClock.advanceBy(1s)` 후 클릭)으로 통일했다.

## 경로를 어떻게 열거했나

`ChallengeDetailContent` 와 그 아래 컴포넌트의 **조건 분기를 코드에서 그대로 훑어** 목록을 만들었다.
상태 필드를 조합해 열거하면(부울 20개) 폭발하므로, "화면을 갈라놓는 조건문" 단위로만 셌다.

1. 최상위 `when`: `isLoading` → `detail == null` → `room != null`(방 홈) → 공개 상세. 4갈래.
2. 조건부 오버레이: 권한 배너(`!isLoading && room != null && missingPermissionTokens().isNotEmpty()`),
   판정 결과 모달(`!isLoading && !resultAcknowledged && unacknowledged != null`), 가입 차단 시트,
   탈퇴·삭제 확인 다이얼로그.
3. 하단 CTA 영역: `!myRole.isMember` 게이트 → `cloneable` → `hideJoinButton` → `isJoining`.
4. 방 홈 3탭: 정보(오늘 상태·세부 설정 유무·진행 정보), 피드(첫 로딩/첫 실패/빈 상태 3종/목록+이어받기 실패),
   랭킹(멤버 로딩/실패/목록+미등재, 방 순위).
5. 각 분기의 **경계값**: `remainingDays 0`(D-day), `ranking == null`(달성률 "-"), `roomSuccessRate == null`
   ("아직 집계 전"), `permissions == null`(모름 ≠ 거부), `owner == null`(봇방장), 앱이 모르는 차단 사유(null).

각 분기마다 (a) 보여야 하는 것, (b) **보이면 안 되는 것**, (c) 눌렀을 때 올라가는 인텐트를 확인한다.
(b)를 뺀 테스트는 "숨겨야 할 걸 숨겼는지"를 못 잡는데, 이 화면은 그게 절반이다 —
비공개 방의 참여 버튼, 이미 멤버인데 뜨는 CTA, 방장이 있는 방의 "방장 되기" 같은 것들.

산출: `ChallengeDetailScreenTest` 23개 + `ChallengeDetailRoomTabsTest` 27개 = 50개.

## 일부러 테스트하지 않은 것

- **`ChallengeDetailScreen`(공개 Composable) 자체**. `hiltViewModel()`·`LifecycleEventEffect`·권한 런처·
  카카오톡 공유 이펙트가 붙어 있어 Robolectric 에서 그리려면 Hilt 테스트 컴포넌트가 필요하다.
  거기 있는 로직 중 검증 가치가 있는 건 CTA 라벨을 고르는 `DetailSetupAction` 계산인데,
  지금은 Composable 함수 본문 안에 있어 어느 층에서도 잡히지 않는다. **미검증 구멍으로 남겨 뒀다** —
  가두려면 순수 함수로 뽑아야 하고, 그건 이번 요청 범위(테스트 작성) 밖의 프로덕션 변경이다.
- **문구 조립·날짜 계산**(`RoomDates.kt`, `TodayVerificationCopy.kt`, `resultNote` 등). 이미 순수 함수
  테스트가 있다. 위의 "두 층에서 같은 문장을 세지 않는다" 원칙.
- **이의 제기 시트(`AppealSheet`)의 입력 검증**. `AppealSheetTest` 가 이미 담당한다. UI 층에서는
  "실패 + `appeal.eligible` + `verificationId` 가 있을 때만 진입점이 열린다"는 분기만 봐도 되는데,
  이건 정보 탭 카드 깊숙한 곳이라 이번 범위에서 뺐다.
- **무한 스크롤 페이징**(`LaunchedEffect` + `snapshotFlow` 로 하단 도달 시 `LoadMoreThreads`).
  Robolectric 에서 레이아웃 기반 스크롤 트리거를 재현하면 뷰포트 높이에 의존하는 취약한 테스트가 된다.
  `canLoadMoreThreads`·`canLoadMoreCrossRanking` 은 상태 파생값이라 ViewModel/모듈 층이 더 맞는 자리다.
- **색·간격·정렬**. 스냅샷 도구가 없고, 색을 문자열로 단언하면 디자인 토큰을 두 번 적는 셈이 된다.
  대신 접근성 관점의 "색 말고 글자로도 구분되는가"(피드 성공/실패 칩)는 텍스트로 확인한다.
- **`ktlint`/실행 검증**. 지시대로 gradle 을 돌리지 않았으므로 컴파일·통과 여부는 미확인이다.
  특히 `ModalBottomSheet`(가입 차단 시트)는 Robolectric 에서 애니메이션 정착 이슈가 알려져 있어,
  실제로 돌렸을 때 가장 먼저 손볼 후보다.
