# ExploreListViewModel 테스트 — 작업 노트

## 어느 층을 골랐나

**모듈 층(JVM 유닛 테스트)** — `:challenge:presentation/src/test` 에 ViewModel 을 직접 생성하고 협력자
셋(`ExploreRepository`·`NavigationHelper`·`Observability`)을 테스트 대역으로 갈아끼웠다.

- ViewModel 이 만지는 Android 타입은 `androidx.lifecycle.ViewModel`(=`viewModelScope`) 뿐이라
  **Robolectric 이 필요 없다.** 메인 디스패처만 갈아끼우면 JVM 에서 그대로 돈다 — 느린 Robolectric 층으로
  올릴 이유가 없다.
- 화면 Composable(`ExploreListScreen`·`ExploreFilterSheet`·`ExploreSortSheet`)은 건드리지 않았다. 이 화면의
  판단은 거의 전부 ViewModel 과 `ExploreListState` 에 있고, UI 층으로 올리면 같은 분기를 렌더 비용을 물고
  다시 세게 된다.
- 이벤트 **스키마**(이름·키·값 타입)는 `:challenge:domain` 의 `ChallengeEventsTest` 가 이미 고정하고 있다.
  그래서 여기서는 스키마를 다시 박지 않고 **팩토리에 넘긴 인자**(position·result_count·sort·page_index 등)만
  본다 — 기대값을 같은 팩토리 호출로 만들어 비교했다. 스키마가 바뀌어도 이 테스트는 안 깨지고, 인자가
  틀리면 깨진다.

## 새로 깐 테스트 인프라

이 레포에는 **ViewModel 테스트가 하나도 없었다.** 그래서 다음을 처음 세웠다(전부 `src/test` 안, 프로덕션
코드는 손대지 않았다):

| 파일 | 역할 |
|---|---|
| `challenge/presentation/build.gradle.kts` | `testImplementation(libs.kotlinx.coroutines.test)` 한 줄 추가(카탈로그에 이미 있는 별칭). 유일한 빌드 변경. |
| `.../presentation/MainDispatcherRule.kt` | `Dispatchers.Main` → `UnconfinedTestDispatcher`. 인텐트 한 줄 뒤에 상태를 바로 읽을 수 있어 `runTest`/`advanceUntilIdle` 없이 읽힌다. |
| `.../presentation/fake/FakeExploreRepository.kt` | `explore()` 호출 인자(필터·정렬·커서·size)를 순서대로 기록하고, 응답을 **호출 순서대로** 스크립트한다. 서버 거절 → 자가 수정 재조회처럼 한 인텐트가 두 번 호출을 만드는 경로 때문에 큐가 필요했다. `gate` 로 응답을 붙잡아 진행 중 상태를 본다. 나머지 메서드는 호출되면 `NotImplementedError`(기존 `FakeChallengeRepository` 컨벤션). |
| `.../presentation/fake/RecordingNavigationHelper.kt` | 이동한 `NavRoute` 와 뒤로가기 횟수만 기록. |
| `.../presentation/fake/RecordingObservability.kt` | `Observability` 는 인터페이스가 아니라 파이프라인 본체라 포트(Clock·ContextProvider·Policy·Sink)만 대역으로 조립했다. 프로필을 `DEV` 로 둬 **게이트 정합성 검사(channel/severity/tag)** 도 같이 돈다. |

## 경로를 어떻게 열거했나

세 축을 교차시켰다.

1. **입력** — `ExploreListIntent` 8종(`Load`/`LoadMore`/`ApplyFilter`/`SelectSort`/`ClearEligibleOnly`/
   `CardImpression`/`OpenChallenge`/`Back`)을 하나도 빼지 않고 훑고, 인자에 분기가 있는 것(`Load` 의
   category·sort 문자열: 유효/무효/없음)은 그 분기까지 폈다.
2. **응답** — 각 조회마다 성공(비어 있음/차 있음/다음 커서 있음·없음) × 실패 4종
   (`InvalidSortTypeException`·`InvalidFilterValueException`·`CursorInvalidException`·그 외)을 폈다. 자가 수정
   경로는 **되돌릴 곳이 있을 때/없을 때**가 갈리므로(기본 정렬에서 난 정렬 오류, 빈 필터에서 난 필터 오류)
   양쪽을 모두 넣었다.
3. **부수 효과** — `ChallengeEvents` 중 이 화면이 부르는 6종(`explore_list_view`·`explore_filter_apply`·
   `explore_sort_change`·`explore_empty_result`·`challenge_card_impression`·`challenge_card_click`·
   `explore_list_load_more`)이 **언제·몇 번** 나가는지. 중복 제거(노출)·리셋(필터 변경 시 노출 집합,
   첫 페이지 재조회 시 page_index)처럼 "안 나가는 것"도 같이 고정했다.

총 32개 테스트. 상태 전이는 리듀서를 따로 부르지 않고 인텐트 → 상태로만 봤다 — 리듀서는 `private` 이고,
상태 전이의 의미는 인텐트 경로에서만 성립한다.

## 일부러 테스트하지 않은 것

- **`ExploreListState.emptyReason`·`canLoadMore`** — 협력자 없이 상태만으로 결정되는 순수 함수라 ViewModel 이
  아니라 케이스 층(`ExploreListStateTest`)에 속한다. ViewModel 을 돌려 4가지 사유를 만들면 조합만 늘고
  검증 대상은 그대로다. 요청 범위(ViewModel)를 넘어서므로 만들지 않았다.
- **Composable 화면·시트** — 위 참조.
- **`ExploreRepositoryImpl` DTO 매핑, Hilt 그래프, `MviViewModel` 베이스** — 각각 data 층·컴포지션 루트·
  `core:ui` 소관이다.
- **`explore(size = ...)`** — ViewModel 이 이 인자를 넘기지 않는다(서버 기본값에 맡긴다). fake 는 기록만 한다.
- **이펙트 채널** — 이 화면은 `NoEffect` 라 검증할 게 없다.

## 테스트를 짜면서 눈에 띈 것 (코드는 고치지 않았다)

1. **첫 페이지 `CursorInvalidException` 은 재시도 횟수 제한이 없다.** `recoverOrFail` 이 같은 조건으로
   `fetchFirstPage` 를 다시 부르므로, 서버가 계속 이 오류를 주면 무한 재조회가 된다. 테스트는 "한 번 실패 후
   성공" 시나리오로 고정했다(무한 루프를 테스트로 재현하지 않았다).
2. **목록에 없는 카드의 노출은 영구히 삼켜진다.** `logImpression` 이 `impressed.add(id)` 를 **항목 조회보다
   먼저** 하므로, 아직 목록에 없는 id 가 한 번 올라오면 나중에 그 카드가 실제로 붙어도 다시는 노출로 세지
   않는다. 지금 동작(로그 안 나감)만 고정했고, 삼킴 자체는 의도인지 확인이 필요하다.
3. **첫 페이지 조회에는 중복 호출 가드가 없다.** 클래스 KDoc 은 "진행 중인 요청이 있으면 중복 호출을
   막는다"고 하지만, 실제로 막는 건 `loadMore`(`canLoadMore`) 뿐이다. 필터·정렬을 빠르게 두 번 바꾸면
   요청이 겹치고 **늦게 도착한 응답이 이긴다.** 스펙 확인이 필요해 테스트로 고정하지 않았다.
4. **정렬 오류 복구는 사용자가 고른 정렬을 말없이 되돌린다**(`state.sort` 까지 기본값으로 바뀐다).
   의도된 동작으로 보여 그대로 고정했다.
