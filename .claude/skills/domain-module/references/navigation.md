# 화면 라우트(Page) 추가하기

경로 문자열은 **한 곳에만 존재한다**(`core:domain` 의 `AppRoutes`). Page 는 그 상수를 참조하고,
`:app` 의 레지스트리가 상수로 렌더러를 건다. 문자열을 두 곳에 쓰면 한쪽만 고쳐진 채 컴파일이 통과한다.

## 순서

### 1. `core:domain` 의 `navigation/AppRoutes.kt` 에 상수 추가

feature 묶음 주석 아래에 한 줄. 다른 feature 가 직접 들어오는 경로면 `// 진입점` 표시를 남긴다.

```kotlin
// challenge
const val CHALLENGE_RANKING = "challenge/ranking" // 그룹 랭킹(방 홈 → 랭킹)
```

경로는 **선행 슬래시 없이** `<feature>/<screen>` 형태. 딥링크와 앱 내 이동이 같은 값을 쓴다.

### 2. feature `domain/navigation/<Name>Page.kt`

인자가 없으면 `object`(profile 은 `data object` 를 쓴다 — 어느 쪽이든 그 모듈의 옆 파일을 따른다),
인자가 있으면 `data class`:

```kotlin
/** 챌린지 상세/참여 페이지(명세 3.3). 홈 카드에서 challengeId 와 함께 진입한다. */
data class ChallengeDetailPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_DETAIL
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
```

- `PATH` 는 반드시 `AppRoutes` 상수를 가리킨다. 리터럴을 쓰지 않는다.
  (`object` 로 만들면 companion 없이 본문에 바로 `const val PATH` 를 둔다.)
- **인자 키도 상수로 둔다** (`ARG_*`). 레지스트리의 디코딩 쪽이 같은 상수를 쓰므로, 오타가 나면
  "화면은 열리는데 값이 빈" 증상으로만 드러난다.
- 화면이 여러 개인 묶음은 한 파일에 모은다(`MyPages.kt`, `VerificationPages.kt`).
- KDoc 에 **어디서 들어오는지**를 적는다. 다른 feature 가 쓰는 진입점이면 그 사실도 함께.

#### 인자는 전부 String 이다

`NavRoute.args` 는 `Map<String, String>` 이다. 백스택에 직렬화돼 남기 때문이다.
숫자·리스트는 인코딩 규칙을 **Page 가 소유하고 구분자도 상수로 노출한다:**

```kotlin
data class VerificationLocationPage(
    val challengeId: String,
    val defaultRadiusM: Float,
    val targetPackages: List<String> = emptyList(),
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            mapOf(
                ARG_CHALLENGE_ID to challengeId,
                ARG_RADIUS to defaultRadiusM.toString(),
                ARG_TARGET_PACKAGES to targetPackages.joinToString(TARGET_PACKAGES_DELIMITER),
            ),
        )

    companion object {
        const val PATH = AppRoutes.VERIFICATION_LOCATION
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_RADIUS = "defaultRadiusM"
        const val ARG_TARGET_PACKAGES = "targetPackages"
        const val TARGET_PACKAGES_DELIMITER = ","
    }
}
```

구조가 복잡하면 JSON 문자열로 인코딩한다(`NavRouteJson` — `ignoreUnknownKeys = true` 라
스키마가 바뀌어도 백스택에 남은 값이 최대한 살아남는다). 다만 **인자에 큰 객체를 실어 나르지 않는다** —
목록에서 상세로 넘길 때는 id 만 넘기고 상세가 다시 조회한다.

### 3. feature `presentation` — 화면 Composable + ViewModel

인자는 Composable 파라미터로 받는다. 화면이 `NavRoute` 를 아는 일은 없다.

### 4. `:app` 의 `navigation/AppRouteRegistry.kt` 에 한 줄

```kotlin
AppRoute(
    path = ChallengeRankingPage.PATH,
    render = { args ->
        RankingScreen(challengeId = args[ChallengeRankingPage.ARG_CHALLENGE_ID].orEmpty())
    },
),
```

디코딩은 여기서 한다. **없거나 깨진 값의 기본값도 여기서 정하고**, 왜 그 기본값인지 주석을 남긴다
(`?: SetupAnchors.DEFAULT_RADIUS_M` — "서버 설정값을 받기 전까지 쓰는 표시용 기본값").

플래그는 넷:

| 플래그 | 기본 | 언제 켜나 |
|---|---|---|
| `isBottomTab` | false | 하단 탭 화면 |
| `isRoot` | false | 이동 시 백스택을 비우고 시작 (스플래시·로그인·홈) |
| `isLoginRequired` | **true** | 공개 화면일 때만 명시적으로 끈다 |
| `syntheticStack` | 자기 자신만 | 딥링크로 중간 화면에 바로 들어와도 뒤로가기가 부모를 거쳐야 할 때 |

**`isLoginRequired` 의 기본값 `true` 는 의도된 것이다.** 등록하면서 깜빡해도 로그인을 요구하는 쪽으로
떨어진다. 공개로 열면 `AppRouteAccessPolicyTest` 가 실패해 리뷰를 강제한다 — 테스트를 고치기 전에
정말 공개해도 되는 화면인지부터 확인한다.

## 화면 이동

ViewModel 이 `NavigationHelper` 로 한다. Composable 이 직접 이동하지 않는다.

```kotlin
navigationHelper.navigateTo(ChallengeDetailPage(challengeId))   // 타입 인자가 있는 이동
navigationHelper.navigateByRoute(route)                          // 이미 NavRoute 를 들고 있을 때
navigationHelper.navigateToBack()
```

- `replaceStackWith` 는 스플래시 → 딥링크 목적지처럼 **백스택을 갈아엎어야 할 때만** 쓴다.
- 딥링크는 인증보다 먼저 도착한다. `PendingDeepLink` 에 보류했다가 자동 로그인 성공 후
  `consumeFor` 로 1회 소비한다. 목적지를 백스택에 먼저 깔지 않는다.

## 흔한 실수

- **`AppRoutes` 를 건너뛰고 Page 에 리터럴을 쓴다** → 딥링크 파싱과 화면 이동이 다른 문자열을 보게 된다.
- **레지스트리 등록을 빼먹는다** → 이동은 되는데 아무것도 안 그려진다. 컴파일은 통과한다.
- **`ARG_*` 상수를 안 만들고 양쪽에 문자열을 쓴다** → 값만 조용히 비는 버그.
- **인자에 entity 를 통째로 JSON 으로 싣는다** → 백스택 복구 시 스키마가 안 맞아 깨진다. id 만 넘긴다.
