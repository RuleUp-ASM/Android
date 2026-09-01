# UI 층 — Robolectric + Compose

**이 레포에는 Compose UI 테스트가 아직 하나도 없다.** `ui-test-junit4` 의존성은 `:app` 에만 선언돼 있고 실제로 쓰는 파일은 없다. 실행 기반부터 세운다.

Robolectric 을 쓰는 이유는 하나다 — **CI 가 이미 도는 `./gradlew test` 안에서 돌기 때문**이다. 계측 테스트(`androidTest`)로 쓰면 에뮬레이터 워크플로를 새로 붙여야 하고, 붙여도 느려서 PR 마다 돌리지 못한다.

## 목차
- [실행 기반 세우기](#실행-기반-세우기)
- [기대값은 Figma 에서 가져온다](#기대값은-figma-에서-가져온다)
- [테스트 형태](#테스트-형태)
- [무엇을 검증하는가](#무엇을-검증하는가)
- [계측 테스트로 남겨야 하는 것](#계측-테스트로-남겨야-하는-것)
- [이 레포 고유의 함정 둘](#이-레포-고유의-함정-둘)
- [자주 밟는 지뢰](#자주-밟는-지뢰)

---

## 실행 기반 세우기

### 1. 버전 카탈로그

`gradle/libs.versions.toml` — 동적 버전(`4.+`)은 쓰지 않는다.

```toml
[versions]
robolectric = "4.14.1"      # 도입 시점의 최신 안정판을 확인하고 넣는다

[libraries]
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
```

### 2. presentation 모듈의 `build.gradle.kts`

```kotlin
android {
    testOptions {
        unitTests {
            // Compose 가 테마·리소스를 읽어야 렌더된다. 없으면 리소스 조회에서 터진다.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // manifest 는 반드시 debugImplementation 이다. 단위 테스트는 debug 변형의 **병합된 매니페스트**를
    // 읽는데, testImplementation 으로 넣으면 클래스는 오지만 createComposeRule 이 띄울
    // ComponentActivity 항목이 매니페스트에 안 실려 런타임에 터진다. app/build.gradle.kts 도 같은 방식이다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 3. SDK 를 고정한다 — 여기서 가장 많이 막힌다

모듈들이 `compileSdk 37` 로 선언돼 있는데 Robolectric 은 자기가 지원하는 SDK 의 `android-all` jar 만 받아온다. 그대로 두면 최신 SDK 를 찾다가 실패한다. 모듈마다 고정해 둔다.

`<module>/src/test/resources/robolectric.properties`:
```
sdk=34
qualifiers=w411dp-h891dp-xhdpi
```

테스트마다 `@Config(sdk = [34])` 를 붙여도 되지만 파일 하나로 모듈 전체를 덮는 쪽이 낫다 — 새 테스트를 쓰는 사람이 이 사실을 몰라도 된다. 설치한 Robolectric 이 지원하는 최대 SDK 를 확인하고 그 값을 쓴다.

`qualifiers` 도 같이 못 박는다. 기본 창이 320dp 라 `assertIsDisplayed()` 가 **화면 밖으로 밀려났는지**에 좌우된다 — 규칙은 맞는데 창이 좁아서 빨개지는 테스트는 무엇이 틀렸는지 말해주지 않는다. 흔한 폰 크기를 고정해 두면 그 축이 사라진다.

### 4. 첫 테스트로 기반을 확인한다

인프라를 세운 PR 에서는 아무 화면이나 하나를 골라 "뜬다"만 확인하는 테스트를 넣고 `./gradlew :<module>:testDebugUnitTest` 가 초록인 걸 본 다음 넘어간다. 기반이 안 도는 상태로 테스트를 여러 개 쓰면 무엇이 문제인지 갈라내지 못한다.

---

### 5. 대상 Composable 의 가시성을 확인한다

화면은 상태를 받고 의도를 올리는 순수 함수여야 테스트할 수 있다. 바깥 `<Screen>` Composable 은 `hiltViewModel()` 을 직접 꺼내므로 테스트 대상이 아니고, 안쪽 `<Screen>Content`(State 를 인자로 받고 Intent 를 콜백으로 올리는 쪽)가 대상이다.

이 레포의 화면은 이미 그렇게 나뉘어 있지만 **Content 가 `private` 인 경우가 많다.** `internal` 로 여는 것 말고 다른 방법이 없는데, 이건 엄연한 프로덕션 변경이다. 조용히 바꾸지 말고 사용자에게 "테스트를 위해 가시성만 연다"고 알리고 진행한다 — 동작은 그대로이고 모듈 밖으로 새지도 않지만, 테스트 편의로 프로덕션을 고치는 건 사용자가 판단할 몫이다.

상태 호이스팅 자체가 안 돼 있다면(Content 가 ViewModel 을 직접 받는다면) 그건 가시성보다 큰 변경이다. 화면을 쪼개는 게 먼저이고, 그 역시 먼저 합의한다.

## 기대값은 Figma 에서 가져온다

**현행 디자인: `https://www.figma.com/design/JDWCrvnTlAyrtzMI2MtpcC/RuleUp-디자인?node-id=1134-2`**
(파일 `JDWCrvnTlAyrtzMI2MtpcC`, 페이지 `1134:2` 「🏁 23 · 최종 · 전체 화면」)

UI 테스트를 쓰기 전에 대상 화면의 프레임을 연다. 이건 형식이 아니라 이 층이 성립하는 조건이다.

기대 문구를 **구현에서 베껴 오면 그 테스트는 동어반복**이다. 화면이 지금 뭘 그리든 그대로 못 박을 뿐이라, "오늘 이후의 변경"은 잡아도 "처음부터 틀렸다"는 절대 못 잡는다. 문구가 명세와 다른 채로 나갔다면 그 테스트는 틀린 화면을 초록불로 지켜 준다. 기대값이 **바깥 권위**에서 와야 단언에 의미가 생기고, 이 프로젝트에서 그 권위는 Figma다.

다행히 이 파일은 텍스트 레이어 이름이 곧 화면 문구다 — `1134:143`(상세·정보)의 레이어가 `오늘 내 인증` · `내 세부 설정` · `인증 규칙` · `규칙 전체 보기` · `감시자 꺼짐` · `관리` 처럼 돼 있어, `onNodeWithText` 에 넣을 값을 그대로 읽어 올 수 있다.

### 프레임 찾는 법

`mcp__figma__get_metadata` 에 `fileKey=JDWCrvnTlAyrtzMI2MtpcC` 와 노드 ID 를 준다. 화면 이름은 `상세 · 정보` 처럼 **화면 · 상태/탭** 형식이라, 상태마다 프레임이 따로 있는 경우가 많다 — 그 갈래가 곧 테스트할 상태 경로다.

인증 관련 주요 프레임: 상세·정보 `1134:143` · 실패/이의 3종 `1134:512` · 루틴 세부 설정 `1134:1040` · 권한 GPS요청 `1134:849` / GPS거부 `1134:894` / 스크린타임 `1134:920` / 헬스커넥트 `1134:959` / 재연결 `1134:997` · 이의 작성 `1134:768` / 완료 `1134:824` / 내역 `1134:2291` · 인증 기록 `1134:2334` · 솔로 상세 `1134:1930`.

**노션 테크 스펙에 적힌 `420:*` · `425:*` · `629:*` · `968:*` 대의 노드 ID 는 구 페이지 것이라 현재 디자인과 다르다.** 문서에서 노드 ID 를 주워 쓰지 말고 `1134:2` 아래에서 이름으로 다시 찾는다.

### 무엇을 가져오고 무엇을 안 가져오나

가져온다 — 화면에 보이는 **문구**, 상태마다 **무엇이 나타나고 사라지는지**, 버튼의 **활성·비활성**, 빈 상태에 뭐라고 쓰는지.
안 가져온다 — 색·간격·폰트·정렬(아래 "무엇을 검증하는가" 참고). 목업의 더미 값(`D-12` · `86%` · `8명 · 방장 지현`)도 데이터지 명세가 아니다. 그 자리에 **무엇이 오는지**가 명세다.

### 어긋날 때

- **디자인과 코드의 문구가 다르다** → 어느 쪽이 맞는지 임의로 정하지 않는다. 둘 다 적어 사용자에게 묻는다. 테스트로 한쪽을 못 박는 순간 그게 정답이 되어 버린다.
- **그 상태의 프레임이 디자인에 없다** → 지어내지 않는다. 2026-08 기준 판정 결과 모달·수동 인증 체크는 최종 페이지에 프레임이 아예 없다(구 노드만 있다). 이런 건 구현에서 읽어 임시로 못 박되, 테스트 KDoc 에 "디자인 미확정"을 남기고 `TEST_STRATEGY.md` 미검증 목록에 올린다.

---

## 테스트 형태

```kotlin
package com.ruleup.challenge.presentation.detail

@RunWith(RobolectricTestRunner::class)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `아직 결과가 안 나온 날은 실패로 보이지 않는다`() {
        // PENDING 을 빨간 실패로 그리면 사용자가 하지도 않은 실패를 믿는다. 가장 비싼 오독이다.
        compose.setContent {
            RuleUpTheme { ChallengeDetailContent(state = state(TodayStatus.PENDING), onIntent = {}) }
        }

        compose.onNodeWithText("확인 중이에요").assertIsDisplayed()
        compose.onNodeWithText("실패").assertDoesNotExist()
    }

    @Test
    fun `인증하기를 누르면 인증 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        compose.setContent {
            RuleUpTheme { ChallengeDetailContent(state = state(TodayStatus.PENDING), onIntent = { intents += it }) }
        }

        // 맨 performClick 은 SingleClickGuard 에 삼켜진다 — 아래 "이 레포 고유의 함정 둘" 참고
        compose.onNodeWithText("인증하기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.Verify), intents)
    }
}
```

노드를 찾을 때는 **사용자가 보는 것**으로 찾는다 — `onNodeWithText`, `onNodeWithContentDescription`. `testTag` 는 텍스트가 없거나(아이콘·차트) 같은 문구가 여러 개일 때만 쓰고, 그때도 태그 이름은 화면이 아니라 역할로 짓는다.

---

## 무엇을 검증하는가

- **상태 → 화면의 분기** — sealed State 나 status enum 이 갈리는 갈래마다 하나. 로딩·비었음·오류·정상은 각각 다른 화면이므로 각각 본다
- **오독하면 가장 비싼 표시** — "대기 중"이 "실패"로 보이는 것 같은 것. 무엇이 보이는지뿐 아니라 **무엇이 안 보이는지**(`assertDoesNotExist`)를 함께 단언한다
- **조작 → Intent** — 버튼·입력이 올바른 Intent 를 올리는가
- **입력 차단** — 범위를 벗어나는 입력을 화면이 막는가. `CLAUDE.md` 상 입력 차단은 화면 책임이라 여기가 유일한 검증 자리다
- **비활성 상태** — 조건 미충족일 때 버튼이 잠기는가(`assertIsNotEnabled`)

검증하지 않는 것: 색·간격·폰트(디자인 시스템 토큰을 쓴다는 건 코드가 말한다), 문구를 만들어내는 계산(케이스 층), 화면 이동(ViewModel 층 — 화면은 Intent 만 올린다).

---

## 계측 테스트로 남겨야 하는 것

Robolectric 이 흉내내지 못하는 것들이다. 지금은 실행 기반이 없으니 **만들지 말고 `TEST_STRATEGY.md` 의 미검증 목록에 올려라.** 필요가 쌓이면 그때 에뮬레이터 워크플로를 붙인다.

- 런타임 권한 다이얼로그(위치·Usage Access·Health)
- 지오펜스·WorkManager 주기 실행 같은 실제 시스템 서비스
- 딥링크로 앱이 열리는 진입 (`app/src/androidTest` 의 `NavRouteUriParserTest` 가 `android.net.Uri` 때문에 계측에 있는 것과 같은 이유)
- 실기기 렌더링 회귀(스크린샷 비교)

수동 확인 절차는 `VERIFICATION_TEST_PLAN.md` 에 이미 있다. 새로 만들지 말고 거기에 이어 쓴다.

---

## 이 레포 고유의 함정 둘

둘 다 프로덕션 코드는 옳은데 Robolectric 환경에서만 어긋난다. 모르면 "테스트를 잘못 썼나" 하며 오래 헤맨다.

### `SingleClickGuard` 가 첫 클릭을 삼킨다

`core:designsystem` 의 `SingleClickGuard` 는 이중 내비게이션을 막는 **전역** 가드다.

```kotlin
object SingleClickGuard {                 // object = JVM 전역, 테스트 간에 살아남는다
    private var lastGlobalClickTime = 0L
    fun tryPass(now: Long, throttleMillis: Long): Boolean {
        if (now - lastGlobalClickTime < throttleMillis) return false   // 막힐 땐 갱신도 안 한다
        lastGlobalClickTime = now
        return true
    }
}
```

`now` 는 `SystemClock.elapsedRealtime()` 이고 Robolectric 은 이 시계를 **테스트마다 작은 초기값으로 되돌린다.** 그래서 첫 클릭에서 `(작은 값) - 0 < 300` 이 되어 **맨 처음 클릭부터 막힌다.**

더 고약한 건 그 다음이다. `lastGlobalClickTime` 은 `object` 의 필드라 JVM 이 살아 있는 동안 테스트를 건너 남는데, 시계만 매번 되감기니 앞선 테스트가 밀어 둔 값보다 작은 시각이 들어와 **차이가 음수**가 되고 계속 막힌다. 즉 테스트를 하나만 돌리면 통과하는데 클래스 전체를 돌리면 깨지는, 원인을 가장 찾기 어려운 형태로 나타난다.

증상은 "클릭했는데 Intent 가 안 올라옴"이라 화면 코드부터 의심하게 되지만 원인은 시계다. `Modifier.singleClickable`·`rememberSingleClick` 을 쓰는 버튼(대부분의 CTA)이 전부 해당한다.

**시계를 단조 증가시켜 가드를 넘긴다.** 프로덕션에 테스트용 리셋 훅을 뚫지 않는다 — 그 훅은 릴리스 빌드에도 남는다.

```kotlin
@Before
fun advanceClockPastPreviousTests() {
    // 앞선 테스트가 남긴 전역 시각보다 확실히 앞으로. 테스트마다 누적시켜 되감기지 않게 한다.
    ShadowSystemClock.advanceBy(Duration.ofSeconds(60))
}

/** 한 테스트에서 두 번 이상 누를 때. 가드(300ms) 너머로 밀고 누른다. */
private fun SemanticsNodeInteraction.clickPastGuard() {
    ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
    performClick()
}
```

핵심은 **단조 증가**다 — 되감으면 가드가 다시 막는다. 그림자 시계 전진 API 이름은 쓰는 Robolectric 판에서 확인한다.

### `RuleUpTheme` 없이 렌더하면 터진다

색 토큰이 기본값 없는 `staticCompositionLocalOf` 라서다.

```kotlin
private val LocalRuleUpColors = staticCompositionLocalOf<RuleUpColorScheme> {
    error("RuleUpColorScheme is not provided. Wrap your content in RuleUpTheme { }.")
}
```

`setContent` 안은 예외 없이 `RuleUpTheme { }` 으로 감싼다. 위 문구가 보이면 원인은 이것 하나뿐이다.

---

## 자주 밟는 지뢰

- **`isIncludeAndroidResources = true` 누락** → 리소스를 못 찾아 터진다. 오류 메시지가 원인을 잘 안 알려준다.
- **`robolectric.properties` 없이 `compileSdk 37`** → `android-all` jar 다운로드 실패. 위 3번.
- **첫 실행이 아주 느림** → Robolectric 이 `android-all` jar 를 받는 중이다. CI 에서는 Gradle 캐시에 얹힌다.
- **애니메이션이 끝나기를 기다림** → `compose.waitUntil { … }` 로 조건을 기다린다. `Thread.sleep` 은 쓰지 않는다.
