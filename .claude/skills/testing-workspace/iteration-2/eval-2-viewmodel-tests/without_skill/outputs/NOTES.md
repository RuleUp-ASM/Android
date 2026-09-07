# ExploreListViewModel 테스트 — 작업 노트

## 어느 층을 골랐나

**모듈 층(ViewModel) 단위 테스트**. 이유:

- 이 클래스의 어려운 부분은 화면 그리기가 아니라 **협력자 세 개를 엮는 조립**이다 —
  `ExploreRepository`(조회 조건), `NavigationHelper`(이동), `Observability`(전환율 이벤트).
  대역 세 개를 꽂으면 인텐트 → (상태 · 서버로 나간 조건 · 발행된 이벤트)로 전부 관찰된다.
- 값 규칙(`ExploreSort.fromValue`, `ExploreFilter.activeCount`, `Category.fromValue`)은 이미 `challenge:domain` /
  `core:domain` 의 케이스 테스트가 잡고 있어 여기서 다시 세지 않았다.
- Compose 화면(`ExploreListScreen`)은 건드리지 않았다. 이 모듈에 Robolectric 세팅이 없고,
  검증하고 싶은 분기가 전부 ViewModel 안에 있어 UI 층까지 올릴 이유가 없었다.

파생 상태(`emptyReason`·`canLoadMore`)만 `ExploreListStateTest` 로 따로 뺐다. 빈 결과 사유는 문구와 CTA 를
가르는 분기라 우선순위가 어긋나면 사용자가 못 고치는 조건을 고치라고 안내하게 되는데, ViewModel 을
거쳐서 4가지 사유를 만들면 조합이 폭발한다.

## 새로 세운 테스트 기반

이 모듈에는 ViewModel 테스트가 하나도 없었다. 아래가 새로 필요했다.

| 산출물 | 왜 |
|---|---|
| `challenge/presentation/build.gradle.kts` | `kotlinx-coroutines-test`(Main 디스패처 교체), `testFixtures(:observability:domain)`(RecordingSink·testObservability) 두 줄 추가. `kotlin("test-junit")` 은 이미 있었다. |
| `MainDispatcherRule` | `viewModelScope` 는 `Dispatchers.Main` 이라 유닛 테스트에서 그냥 죽는다. `UnconfinedTestDispatcher` 를 기본값으로 둬서 인텐트가 그 자리에서 끝까지 돈다 — 테스트마다 `advanceUntilIdle()` 을 흩뿌리지 않아도 되고, "진행 중" 상태는 대역이 명시적으로 붙잡을 때만 생긴다. |
| `FakeExploreRepository` | 응답을 **호출 순서대로 큐**로 준다. 이 화면은 한 인텐트가 실패 → 조건 교정 → 재조회로 두 번 이상 나가는 경로가 많아서, 호출마다 다른 답을 못 주면 자기 복구를 재현할 수 없다. `gate`(CompletableDeferred)로 응답을 붙잡아 두는 게 중복 호출 차단을 검증하는 유일한 수단이다. `calls` 에 (필터·정렬·커서)를 그대로 적어, 커서를 버렸는지/이어 붙였는지를 상태가 아니라 **나간 요청**으로 본다. |
| `RecordingNavigationHelper` | 이동을 `NavSignal` 그대로 적는다. 경로 문자열을 테스트에 다시 적지 않고 `ChallengeDetailPage(id).toRoute()` 와 비교한다. |
| 빌더(`exploreChallenge`·`challenges`·`answer`·`throws`) | `ExploreChallenge` 는 필드가 17개다. 기본값을 깔고 테스트가 관심 있는 축만 지정해야 무엇을 보는 테스트인지 읽힌다. |

관측은 목을 만들지 않고 **실제 `Observability` 에 `RecordingSink` 를 꽂았다**(`testObservability`). 게이트와
채널 일관성 검사가 실제로 도는 상태여야 채널·severity·tag 계약 위반이 드러난다.

## 경로를 어떻게 열거했나

1. **인텐트 8종**을 축으로 잡고(Load / LoadMore / ApplyFilter / SelectSort / ClearEligibleOnly /
   CardImpression / OpenChallenge / Back), 각 인텐트의 분기를 코드에서 하나씩 따라갔다.
2. **가드 세 개**(재진입 `loaded`, `canLoadMore`, 노출 중복 제거 `impressed`)는 각각 "막히는 쪽 / 통과하는 쪽"
   양방향으로 짝을 만들었다.
3. **에러 요인**은 `recoverOrFail` 의 분기 표를 그대로 옮겼다 — 예외 3종 × (교정 여지 있음 / 이미 최소 조건) +
   그 밖의 예외 + 메시지 없는 예외. 자기 복구는 "복구했다"만이 아니라 **몇 번 만에 멈추는지**(무한 재시도가
   아닌지)까지 호출 수로 고정했다.
4. **이벤트 발행 지점**은 `observability.log` 호출부 8곳을 세어, 이름·발행 횟수·전환율 계산에 쓰이는 키
   (`result_count`·`position`·`has_metrics`·`page_index`)를 확인했다.
5. 상태 파생값은 `emptyReason` 의 when 절 순서를 우선순위 그대로 확인했다.

총 33개 테스트 — ViewModel 26개(진입 3 · 필터/정렬 4 · 페이지네이션 6 · 자기 복구 6 · 노출·클릭·이동 7), 파생 상태 7개.

## 일부러 안 한 것 · 작업 중 발견

**안 한 것**
- `ExploreListScreen` Composable, 뷰포트 50%·1초 노출 판정(화면 소관).
- `ExploreSort.fromValue`·`Category.fromValue`·`ExploreFilter.categoriesParam` 등 값 규칙 — `challenge:domain`
  `ExploreTest`, `core:domain` `CategoryTest` 가 이미 잡는다.
- `ChallengeEvents` 페이로드의 전체 스키마 — `ChallengeEventsTest` 가 골든으로 고정한다. 여기서는 ViewModel 이
  **채워 넣는 값**만 봤다.
- Hilt 바인딩·라우트 등록 — 통합 층 관심사.
- `NoEffect` 라 이펙트 흐름은 검증 대상이 없다.

**발견(테스트로 박지 않고 남긴다 — 고칠지 판단이 필요하다)**
1. `logImpression` 이 목록에 없는 카드 id 도 `impressed` 에 먼저 넣는다(`add` 후 조회). 그 카드가 다음
   페이지에서 실제로 들어오면 노출이 영구히 한 번도 안 찍힌다. 지금은 "이벤트를 남기지 않는다"까지만 검증했다.
2. 클래스 KDoc 은 "진행 중인 요청이 있으면 중복 호출을 막는다"고 하지만, 그 가드는 `loadMore` 에만 있다.
   `fetchFirstPage` 는 무방비라 필터·정렬을 빠르게 두 번 바꾸면 두 요청이 경합하고 늦게 온 응답이 이긴다
   (상태의 filter/sort 와 items 가 어긋날 수 있다). 결정적으로 재현하려면 응답 순서를 뒤집는 대역이 필요해
   테스트로 만들지 않았다.
3. 정렬/필터 거절 후 자기 복구로 재조회할 때 `logAfterLoad` 가 따라가지 않아 `explore_sort_change` ·
   `explore_filter_apply` 가 **아예 안 나간다**. 현행 동작으로 한 줄 고정해 뒀으니(분석에서 기대하는 값이면
   테스트가 같이 바뀌어야 한다), 스펙 확인이 필요하다.
4. 첫 페이지에서 `CursorInvalidException` 이 나면 **같은 요청을 그대로** 다시 보낸다. 서버가 계속 이 코드를
   주면 무한 재시도가 된다. 테스트는 1회 실패 후 성공까지만 태웠다.
