# ExploreListViewModel 테스트 — 작업 노트

## 어느 층을 골랐고 왜인가

**모듈 테스트(JVM 유닛 테스트)** 한 층만 썼다. `challenge/presentation/src/test/` 에 두고 Compose·Robolectric 없이 ViewModel 을 직접 생성한다.

- `ExploreListViewModel` 이 지고 있는 계약 — ① (필터, 정렬) 이 바뀌면 커서를 버리고 첫 페이지부터 다시 받는다, ② 서버가 조건을 거절하면 사용자에게 되묻지 않고 스스로 고쳐 재조회한다(정렬→기본, 필터→초기화, 커서→첫 페이지), ③ 탐색 퍼널 이벤트를 정확히 한 번씩 남긴다 — 는 **전부 화면을 띄우지 않고 관측된다**. `uiState` 와 협력자(`ExploreRepository`·`NavigationHelper`·`Observability`)에 남은 흔적만으로 충분하다.
- 위로 올리면(Robolectric Compose) 커서·필터·정렬 같은 요청 파라미터를 볼 수 없고, 아래로 내리면(엔티티 케이스 테스트) 조립이 빠진다. `ExploreSort`·`ExploreFilter`·`ExploreChallenge.hasMetrics` 는 이미 `challenge/domain` 의 `ExploreTest` 가 덮고 있어 여기서 다시 세지 않았다.
- 레포지토리에 **무엇을 보냈는지**까지 단언한다. 반환값만 맞아도 커서를 빠뜨리거나 필터를 흘리면 목록이 조용히 잘못 채워지기 때문이다.

## 새로 세운 테스트 인프라

레포에 ViewModel 테스트가 **하나도 없었다**(`grep` 결과 코루틴 테스트는 `core:datastore`·`verification:data` 둘뿐). 그래서 세 가지를 새로 만들었다.

1. `testing/MainDispatcherRule.kt` — `viewModelScope` 는 `Dispatchers.Main.immediate` 를 쓴다. 유닛 테스트에는 메인 루퍼가 없어 교체 없이는 첫 `launch` 에서 터진다. `UnconfinedTestDispatcher` 를 골랐다: 실기기에서도 `Main.immediate` 라 `launch` 블록이 첫 중단점까지 `onIntent` 안에서 그대로 실행되고, "인텐트가 반환되기 전에 이미 `isLoadingMore` 가 켜져 있다"에 기대는 중복 호출 가드는 이 디스패처에서만 재현된다. `StandardTestDispatcher` 로는 그 가드가 거짓 실패한다.
2. `fake/FakeExploreRepository.kt` — 응답을 호출 순서대로 깔아두고(`succeed`/`fail`) 실제 요청(`filter`·`sort`·`cursor`)을 기록한다. 검증 대상이 아닌 `getTrending`·`getCategories`·`clone` 은 호출되면 `NotImplementedError` 로 터진다(`challenge/domain` 의 `FakeChallengeRepository` 컨벤션 그대로). 로딩 가드용으로 `CompletableDeferred` 게이트를 하나 달았다.
3. `fake/RecordingNavigationHelper.kt` — 어디로 보냈는지만 기록한다.

관측은 새로 만들지 않고 `observability:domain` 의 testFixtures(`RecordingSink`·`testObservability`)를 그대로 썼다. `challenge/presentation/build.gradle.kts` 에 `testImplementation(libs.kotlinx.coroutines.test)` 와 `testImplementation(testFixtures(project(":observability:domain")))` 두 줄만 늘었다.

## 경로를 어떻게 열거했나

`onIntent` 의 인텐트 8종을 축으로 잡고, 각 인텐트마다 (a) 정상, (b) 상태 가드, (c) 서버 실패 갈래, (d) 로깅을 따로 세었다.

- **인텐트 축**: `Load`(카테고리 프리필 유/무 × 정렬 인자 유효/무효/없음, 중복 진입 가드) · `LoadMore`(커서 있음/없음/진행 중) · `ApplyFilter` · `SelectSort` · `ClearEligibleOnly` · `CardImpression`(신규/중복/목록 밖) · `OpenChallenge`(목록 안/밖) · `Back`.
- **실패 축**: `recoverOrFail` 의 분기 4개를 각각 "복구 가능"과 "더 되돌릴 곳 없음" 둘로 갈랐다 — `InvalidSortType`×(비기본/기본), `InvalidFilterValue`×(필터 있음/없음), `CursorInvalid`(첫 페이지/다음 페이지), 그 밖(메시지 있음/없음). 다음 페이지 실패는 첫 페이지 실패와 상태 전이가 다르므로(`loadMoreFailed` vs `errorMessage`) 따로 세었다.
- **로깅 축**: `ChallengeEvents` 중 이 화면이 발행하는 5종(`explore_list_view`·`explore_filter_apply`·`explore_sort_change`·`explore_empty_result`·`challenge_card_impression`·`challenge_card_click`·`explore_list_load_more`)을 이름·속성·**발행 횟수**까지 고정했다. 이벤트는 릴리즈 게이트라 이름이 바뀌어도 컴파일은 통과한다.
- **상태 파생 축**: `emptyReason` 은 우선순위가 있는 4갈래인데, ViewModel 을 통해 도달 가능한 두 갈래(`TIER_FILTER`·`LOW_SAMPLE`)만 여기서 확인했다. 나머지는 아래 참고.

총 34개 테스트.

## 일부러 테스트하지 않은 것

- **`ExploreListState.emptyReason`·`canLoadMore` 전수** — 협력자 없이 성립하는 순수 파생값이라 케이스 테스트 층 소관이다. ViewModel 경로로는 우선순위가 갈리는 두 갈래만 짚었고, `FILTERED`·`CATEGORY_EMPTY` 를 포함한 4갈래 우선순위는 `ExploreListStateTest`(별도 파일)로 빼는 게 맞다. 이번 요청 범위 밖이라 만들지 않았다.
- **엔티티 규칙** — `ExploreSort.fromValue`·`ExploreFilter.activeCount`·`categoriesParam()`·`ExploreChallenge.hasMetrics` 는 `challenge/domain` 의 `ExploreTest` 가 이미 덮는다.
- **이벤트 스키마 자체** — `ChallengeEventsTest` 가 팩토리 출력을 골든으로 고정하고 있다. 여기서는 "언제·몇 번 부르는가"만 본다.
- **화면(Compose)** — 무한 스크롤 트리거(하단 근접), 노출 판정(뷰포트 50%·1초), 시트 상호작용은 화면 책임이라 UI 층에 속한다. 레포에 Robolectric 설정이 없어 이번에 들이지 않았다.
- **`size` 파라미터** — ViewModel 이 넘기지 않는다(레포 기본값). 검증할 동작이 없다.

## 코드에서 발견한, 테스트로 굳히지 않은 것 두 가지

1. **첫 페이지 `CursorInvalidException` 의 무한 재시도.** `recoverOrFail` 이 `CursorInvalid` 를 받으면 조건 없이 `fetchFirstPage(filter, sort)` 를 다시 부른다. 첫 페이지 호출은 `cursor = null` 이라 서버가 계속 이 오류를 주면 루프에서 못 빠져나온다(다음 페이지 경로는 커서가 사라지므로 한 번에 수렴한다). 재시도 상한을 두는 게 맞아 보이는데, 임의로 고치는 대신 남겨 둔다. 테스트는 "한 번 실패 후 복구"만 고정했다.
2. **복구가 로그를 삼킨다.** `ApplyFilter`/`SelectSort` 가 `InvalidFilterValue`·`InvalidSortType` 으로 복구되면 재조회 호출에 `logAfterLoad = null` 이 넘어가 `explore_filter_apply`·`explore_sort_change` 가 아예 발행되지 않는다. 의도인지 누락인지 명세로 확인이 필요해 테스트로 못 박지 않았다(현재 동작을 단언하면 잘못된 쪽을 고정할 위험이 있다). 상태 복구만 검증했다.

## 산출물

```
challenge/presentation/build.gradle.kts                                       (수정: testImplementation 2줄)
challenge/presentation/src/test/kotlin/com/ruleup/challenge/presentation/
  testing/MainDispatcherRule.kt                                               (신규)
  fake/FakeExploreRepository.kt                                               (신규)
  fake/RecordingNavigationHelper.kt                                           (신규)
  explore/list/viewmodel/ExploreListViewModelTest.kt                          (신규, 34 tests)
```

지시대로 실제 레포는 건드리지 않았고 gradle 도 돌리지 않았다.
