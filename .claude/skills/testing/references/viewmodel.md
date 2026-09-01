# 모듈 층 — MVI ViewModel

**이 레포에는 ViewModel 테스트가 아직 하나도 없다.** 실행 기반부터 세우고, 세운 다음 이 문서의 "이미 있다" 표시를 갱신한다.

ViewModel 테스트는 MVI 의 전이 전체 — `onIntent` → (협력자 호출) → `dispatch`/`reduce` → `uiState`·`effect`·네비게이션 — 를 한 덩어리로 본다. 개별 규칙은 이미 케이스 층이 잡았으므로 여기서 다시 훑지 않는다.

## 목차
- [실행 기반 세우기](#실행-기반-세우기)
- [테스트 형태](#테스트-형태)
- [무엇을 검증하는가](#무엇을-검증하는가)
- [자주 밟는 지뢰](#자주-밟는-지뢰)

---

## 실행 기반 세우기

`MviViewModel` 은 `emitEffect` 를 `viewModelScope` 에서 돌린다. `viewModelScope` 는 `Dispatchers.Main` 을 쓰고, JVM 테스트에는 Main 이 없어서 그대로 두면 터진다. `kotlinx-coroutines-test` 로 갈아끼운다.

### 1. presentation 모듈의 `build.gradle.kts`

```kotlin
testImplementation(kotlin("test-junit"))                            // 이미 있음
testImplementation(libs.kotlinx.coroutines.test)                    // 추가
testImplementation(testFixtures(project(":observability:domain")))  // 이벤트를 검증하면
testImplementation(testFixtures(project(":core:domain")))           // 아래 2번을 만든 뒤
```

`kotlinx-coroutines-test` 는 이미 `gradle/libs.versions.toml` 에 있다(`kotlinx-coroutines-test`). 새 버전을 올리지 않는다.

### 2. `NavigationHelper` 공용 Fake

이 레포의 ViewModel 은 화면 이동을 `NavigationHelper` 로 한다 — 이동은 `Effect` 가 아니라 협력자 호출이라서, 그걸 기록하는 대역이 없으면 대부분의 ViewModel 테스트가 성립하지 않는다. `observability:domain` 이 `testFixtures` 로 대역을 내주는 것과 같은 방식으로 `core:domain` 에 둔다.

`core/domain/build.gradle.kts`:
```kotlin
plugins {
    `java-test-fixtures`   // 추가
}

dependencies {
    // 이 줄이 없으면 fixture 가 컴파일되지 않는다. NavigationHelper 의 navigationFlow 가 Flow 인데
    // core:domain 은 coroutines 를 implementation 으로 갖고 있어 testFixtures 컴파일 경로엔 안 온다.
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
```

`core/domain/src/testFixtures/kotlin/com/ruleup/domain/test/RecordingNavigationHelper.kt`:
```kotlin
package com.ruleup.domain.test

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 이동 요청을 순서대로 모아두는 [NavigationHelper]. 어디로 갔는지·안 갔는지를 둘 다 볼 수 있다. */
class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    val routes = mutableListOf<NavRoute>()
    val replaced = mutableListOf<NavRoute>()
    var backCount = 0
        private set

    override val navigationFlow: Flow<NavSignal> = emptyFlow()

    override fun navigateTo(page: Page) { pages += page }

    override fun navigateByRoute(route: NavRoute) { routes += route }

    override fun replaceStackWith(route: NavRoute) { replaced += route }

    override fun navigateToBack() { backCount++ }
}
```

`MessageHelper` 를 주입받는 ViewModel 이 있으면 같은 자리에 `RecordingMessageHelper` 를 함께 둔다.

---

## 테스트 형태

```kotlin
package com.ruleup.challenge.presentation.create.viewmodel

@OptIn(ExperimentalCoroutinesApi::class)
class CreateChallengeViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `초안이 만료됐으면 확인 화면으로 넘기지 않고 다시 쓰라고 알린다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repository = FakeChallengeRepository(/* 만료 응답 */), nav = nav)

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertTrue(viewModel.uiState.value.showsDraftExpired)
            assertTrue(nav.pages.isEmpty())   // 안 간 것도 계약이다
        }
}

// 협력자 기본값을 한 곳에 모아 테스트마다 흔드는 축만 드러나게 한다
private fun viewModel(
    repository: ChallengeRepository = FakeChallengeRepository(),
    nav: NavigationHelper = RecordingNavigationHelper(),
    observability: Observability = testObservability(),
) = CreateChallengeViewModel(/* … */)
```

### Effect 를 보는 법

`effect` 는 `Channel(BUFFERED).receiveAsFlow()` 다. 소비자가 하나뿐이고, 아무것도 안 오면 **영원히 기다린다**. "이펙트가 없다"를 단언하려다 테스트가 멈추는 게 여기서 가장 흔한 사고다.

```kotlin
val effects = mutableListOf<CreateChallengeEffect>()
val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

viewModel.onIntent(CreateChallengeIntent.Submit)

assertEquals(listOf(CreateChallengeEffect.ShowToast("만들었어요")), effects)
job.cancel()
```

이 형태 하나로 "왔다"와 "안 왔다"를 둘 다 본다. 반드시 온다고 확신하는 자리에서만 `viewModel.effect.first()` 로 줄여 쓴다.

### State 를 보는 법

`uiState` 는 `StateFlow` 라 `.value` 를 그냥 읽는다. 흐름을 수집할 필요가 없다.

**전체 State 객체를 통째로 `assertEquals` 하지 마라.** 필드가 하나 늘 때마다 무관한 테스트가 우수수 깨지고, 깨진 이름들이 무엇을 잃었는지도 말해주지 않는다. 그 테스트가 말하는 규칙에 해당하는 필드만 본다.

---

### 인텐트를 빠짐없이 덮었다는 근거

sealed `Intent` 는 화면이 받는 입력의 전부다. 새 인텐트가 생겼는데 테스트를 안 쓰면 조용히 미검증으로 남는데, 그건 리뷰에서 알아채기 어렵다. **`else` 없는 `when` 으로 컴파일러에게 감시를 맡긴다.**

```kotlin
/** 인텐트마다 그것을 지키는 테스트 이름. 인텐트가 늘면 이 when 이 컴파일되지 않아 누락이 드러난다. */
private fun ExploreListIntent.coveredBy(): String =
    when (this) {
        is ExploreListIntent.Load -> "첫 진입이면 기본 조건으로 한 번만 조회한다"
        is ExploreListIntent.LoadMore -> "커서가 남아 있으면 다음 페이지를 이어붙인다"
        is ExploreListIntent.ApplyFilter -> "필터를 바꾸면 1페이지부터 다시 조회한다"
        // …
    }
```

테스트 하나가 이 함수를 호출할 필요는 없다 — **존재 자체가 근거**다. 파일 하단에 두고, 새 인텐트를 추가한 사람이 여기서 막혀 "그럼 테스트도 써야겠네"로 이어지게 한다.

## 무엇을 검증하는가

- **Intent 하나가 State 를 어떻게 옮기는가** — 로딩 시작·성공·실패 세 지점
- **비동기 실패가 화면 상태로 번역되는가** — 예외 타입마다 다른 상태가 되어야 하면 타입마다 하나(`DraftExpiredException`·`RecommendationRateLimitedException` …)
- **이동했는가/안 했는가** — `RecordingNavigationHelper` 로. 실패했는데 다음 화면으로 넘어가는 건 사용자가 가장 크게 다치는 버그다
- **중복 요청 잠금** — `createStartLogged` 같은 1회 잠금, 연타 방지, 진행 중 재진입
- **관측 이벤트** — 전환율 분모가 되는 이벤트는 한 번만 나가야 한다. `RecordingSink` 로 개수까지 단언
- **초기 상태** — `State.initial` 이 화면이 처음 그릴 값으로 맞는가

검증하지 않는 것: 입력값 범위(케이스 층이 끝냈다), 화면 렌더(UI 층), `reduce` 를 직접 호출하는 것(`protected` 이고, 그건 `onIntent` 를 통해 봐야 실제 경로다).

---

## 자주 밟는 지뢰

- **`Dispatchers.setMain` 을 빠뜨림** → `Module with the Main dispatcher had failed to initialize`. `@Before` 에 반드시.
- **`runTest` 안에서 `delay` 를 쓰는 코드** → `UnconfinedTestDispatcher` 는 가상 시간을 쓰므로 `advanceTimeBy` 로 밀거나, 실제로 기다리게 하려면 `StandardTestDispatcher` 를 쓴다. `CreateChallengeViewModel` 처럼 `delay` 로 폴링하는 코드가 있으면 이게 필요하다.
- **effect 를 수집하지 않은 채 `first()`** → 60초 뒤 타임아웃. 위의 `toList` 형태를 쓴다.
- **`@HiltViewModel` 이라 못 만든다고 생각하기** → 아니다. 생성자 주입이라 테스트에서는 그냥 `CreateChallengeViewModel(...)` 로 만든다. Hilt 는 앱에서만 쓴다.
- **`SavedStateHandle` 을 받는 ViewModel** → `SavedStateHandle(mapOf("challengeId" to "ch1"))` 로 직접 만들어 넘긴다.
- **`private` 인 Content Composable** → UI 층 얘기지만 ViewModel 작업 중에도 마주친다. 가시성을 `internal` 로 여는 건 프로덕션 변경이므로 사용자에게 알리고 진행한다(→ `ui.md`).
