# ExploreListViewModel 테스트 — 작업 노트

## 1. 고른 층: 모듈 층 (+ 갈라져 나온 케이스 층 하나)

`ExploreListViewModel` 은 "이 규칙이 깨지면 어느 파일을 고치나" 로 물었을 때 대부분
`ExploreListViewModel.kt` 를 고치게 되는 규칙들을 갖고 있다 — 인텐트 → 서버 조회 → `dispatch/reduce`
→ `uiState`·네비게이션·관측 이벤트. 그래서 **모듈 층**이다. 이 화면은 특히 실패를 사용자에게 되묻지 않고
스스로 조건을 고쳐 재조회하므로, "한 인텐트가 서버에 몇 번 어떤 조건으로 나갔는가"가 곧 계약이다.
단언이 상태뿐 아니라 호출 기록(`FakeExploreRepository.calls`)에도 걸리는 이유다.

다만 `ExploreListState.emptyReason` · `canLoadMore` 는 협력자 없이 값만으로 성립하는 파생이라
깨지면 `ExploreListContract.kt` 를 고친다 → **케이스 층**. ViewModel 테스트에서 빈 결과 4가지를
다시 훑으면 (상태 경로 × 사유) 로 곱해지므로, 사유는 `ExploreListStateTest` 로 내리고 ViewModel 은
"0건이면 빈 결과 이벤트가 나간다" 하나만 본다.

필터·정렬 값 자체의 규칙(정렬 6종, `activeCount`, csv 직렬화)은 이미
`challenge/domain/.../entity/ExploreTest.kt` 가 끝냈다. 이벤트 이름·키 스키마는
`ChallengeEventsTest` 가 고정했다. 둘 다 여기서 다시 세지 않고, ViewModel 테스트는
**어떤 이벤트가 몇 번, 어떤 값으로 나갔는지**만 본다.

## 2. 세운 테스트 인프라 (이 레포에 ViewModel 테스트가 하나도 없었다)

1. `challenge/presentation/build.gradle.kts`
   - `testImplementation(libs.kotlinx.coroutines.test)` — `viewModelScope` 가 `Dispatchers.Main` 을 쓰는데
     JVM 테스트에는 Main 이 없어 그대로 두면 `Module with the Main dispatcher had failed to initialize` 로 죽는다.
     버전 카탈로그에 이미 있던 좌표를 쓰고 새 버전을 올리지 않았다.
   - `testImplementation(testFixtures(project(":core:domain")))`, `...(":observability:domain")))`
2. `core/domain/build.gradle.kts` + `core/domain/src/testFixtures/.../RecordingNavigationHelper.kt`
   - 이 레포의 화면 이동은 Effect 가 아니라 `NavigationHelper` 협력자 호출이라, 대역이 없으면
     "실패했는데 다음 화면으로 넘어갔다"를 어떤 ViewModel 테스트도 잡을 수 없다. `observability:domain` 이
     `testFixtures` 로 대역을 내주는 것과 같은 방식으로 `java-test-fixtures` 를 붙였다.
   - **주의**: `core:domain` 은 coroutines 를 `implementation` 으로 갖고 있어 testFixtures 컴파일 경로에
     오지 않는다(`navigationFlow: Flow<NavSignal>`). `testFixturesImplementation(libs.kotlinx.coroutines.core)` 를 같이 넣었다.
3. `challenge/presentation/src/test/.../fake/FakeExploreRepository.kt`
   - 소비자가 이 모듈 하나뿐이라 `testFixtures` 로 올리지 않고 테스트 소스셋에 뒀다. 응답을 **호출 순서대로**
     소비하고, 준비한 것보다 많이 부르면 실패한다 — 조용한 재요청 루프가 초록으로 통과하지 않게 하는 장치다.

목킹 라이브러리는 들이지 않았다(기존 컨벤션대로 Fake 를 손으로). 테스트 이름은 백틱 한국어 문장,
`kotlin.test` + JUnit4, 코루틴은 `runTest` + `UnconfinedTestDispatcher`.

## 3. 경로를 어떻게 열거했나

- **해피 경로**: `ExploreListIntent` 가 sealed interface라 하위 8종이 곧 목록이다. 파일 끝의
  `ExploreListIntent.coveredBy()` 가 `else` 없는 `when` 으로 8종을 받고 각 인텐트를 지키는 테스트 이름을
  돌려준다 — **인텐트가 늘면 컴파일이 깨진다**. 이게 "열거를 끝냈다"의 근거다.
- **에러 요인**: `ExploreRepository.explore` 가 던질 수 있는 domain 예외 3종
  (`InvalidSortTypeException`·`InvalidFilterValueException`·`CursorInvalidException`) + 그 외. 앞의 둘은
  **되돌릴 곳이 남았는가 / 없는가** 로 다시 갈려 각각 2개(총 4개), 커서는 loadMore 중 조용한 재조회 1개,
  그 외 실패는 첫 페이지 1개 + 다음 페이지 1개. `error.message == null` 분기도 하나(빈 화면 방지).
- **빈 결과 사유**: `EmptyReason.entries` 를 실제 상태에서 만들어 낸 사유 집합과 비교한다 —
  사유가 늘면 이 단언이 깨진다.
- **컬렉션·경계**: 목록 0건 / 1건 / 이어붙임, 커서 있음·없음(마지막 페이지), 페이지 깊이 1·2.

테스트 이름 목록(작성 전에 먼저 뽑은 것)은 그대로 파일에 남아 있다 — `ExploreListViewModelTest` 25개
(전이 규칙 24 + 인텐트 열거 근거 1), `ExploreListStateTest` 8개.

## 4. 일부러 하지 않은 것

- **`reduce` 직접 호출**: `protected` 이고, 실제 경로는 `onIntent` 다. 전이는 전부 `onIntent` 로만 밟았다.
- **Effect 수집**: 이 화면은 `NoEffect` 라 채널이 비어 있다. `effect.first()` 로 60초 대기하는 사고가 애초에 없다.
- **화면 렌더**: 로딩 스피너·빈 상태 문구·하단 "다시 불러오기" 버튼이 실제로 그려지는지는 UI 층(Robolectric)이고,
  이 레포엔 아직 실행 기반이 없다. 상태 필드까지만 본다.
- **입력값 범위·이벤트 스키마**: 케이스 층(`ExploreTest`, `ChallengeEventsTest`)이 이미 잡았다.
- **`TEST_STRATEGY.md` 갱신**: 레포에 아직 이 문서가 없다. `references/strategy-doc.md` 의 "처음 만들 때"는
  3절(미검증)·4절(인수 시나리오)을 **사용자와 함께 가려서** 채우라고 못박고 있어, 혼자 지어내지 않고 남겨 뒀다.
  실제 PR 이라면 "ViewModel 층 실행 기반이 생겼다"를 2·3절에 반영하는 작업이 별도 단위로 따라와야 한다.

## 5. 테스트를 쓰다 발견한 것 (코드는 고치지 않음)

- **첫 페이지에서 `CursorInvalidException` 이 오면 무한 재요청**이 된다.
  `recoverOrFail` 이 `CursorInvalidException -> fetchFirstPage(filter, sort)` 를 조건 없이 태우는데,
  첫 페이지는 이미 `cursor = null` 이라 똑같은 요청이 반복된다. 정렬·필터 거절에는 "되돌릴 곳이 없으면 실패"
  가드가 있는데 커서에는 없다. (테스트에서는 Fake 가 준비된 응답을 넘기면 실패로 끊어서 드러난다.)
- **`logImpression` 이 목록에 없는 id 를 `impressed` 에 먼저 넣는다.** `if (!impressed.add(id)) return` 이
  멤버십 확인보다 앞이라, 목록에 아직 없는 카드의 노출이 한 번 들어오면 그 카드는 이후 목록에 나타나도
  영구히 노출이 안 잡힌다. 지금은 `목록에 없는 카드는 노출로 세지 않는다` 로 현재 동작만 고정해 뒀다.

둘 다 별도 이슈로 올려 고칠지 사용자 판단이 필요해 손대지 않았다.
