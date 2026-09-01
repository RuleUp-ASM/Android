# ExploreListViewModel 테스트 — 작업 메모

## 고른 층과 이유

**모듈 층** 하나 + **케이스 층** 하나로 나눠 썼다. 기준은 "이 규칙이 깨지면 어느 파일을 고치나"다.

- `ExploreListViewModelTest` (모듈 층) — 언제 서버를 부르고/안 부르고, 서버 거절을 어떻게 스스로 고쳐 다시 부르고,
  어디로 이동하고, 어떤 이벤트를 몇 번 보내는가. 깨지면 고칠 파일은 `ExploreListViewModel.kt` 다.
- `ExploreListStateTest` (케이스 층) — `emptyReason`·`canLoadMore`·`initial` 은 협력자 없이 값만으로 성립하는
  파생 규칙이라 `ExploreListContract.kt` 를 고치게 된다. ViewModel 테스트에서 이 5가지 빈 결과 사유를 다시
  훑으면 "조회 경로 × 빈 사유"로 곱이 되므로 아래층으로 내렸다. 모듈 층은 0건일 때 **이벤트가 나가는지**만 본다.
  (`challenge/presentation` 의 기존 테스트들도 순수 함수를 보는 케이스 층이라 이웃 컨벤션과도 맞는다.)

UI 층(Robolectric)은 손대지 않았다. 요청이 ViewModel 이고, 화면 렌더는 Figma 를 기준으로 따로 세워야 한다.

## 세워야 했던 실행 기반

이 레포에는 ViewModel 테스트가 하나도 없어서 인프라부터 만들었다. 전부 기존 버전 카탈로그 안에서 해결했고
새 라이브러리·새 버전은 들이지 않았다.

1. `challenge/presentation/build.gradle.kts`
   - `testImplementation(libs.kotlinx.coroutines.test)` — `viewModelScope` 가 `Dispatchers.Main` 이라 JVM 테스트에서
     `Dispatchers.setMain` 없이는 초기화부터 터진다.
   - `testImplementation(testFixtures(project(":core:domain")))` / `(":observability:domain")`.
2. `core/domain/build.gradle.kts` — `java-test-fixtures` 플러그인 + `testFixturesImplementation(libs.kotlinx.coroutines.core)`
   (fixture 가 `NavigationHelper.navigationFlow: Flow` 를 구현해야 하는데 coroutines 가 `implementation` 이라
   testFixtures 컴파일 경로에 안 온다).
3. `core/domain/src/testFixtures/.../RecordingNavigationHelper.kt` — 이 레포의 화면 이동은 이펙트가 아니라
   협력자 호출이라, 이 대역이 없으면 "실패했는데 상세로 넘어갔다"를 아예 단언할 수 없다. `observability:domain` 이
   `RecordingSink`·`testObservability` 를 fixture 로 내주는 방식을 그대로 따랐다.
4. `challenge/presentation/src/test/.../fake/FakeExploreRepository.kt` — 목킹 라이브러리를 쓰지 않는 컨벤션대로 손으로 썼다.
   응답을 **호출 순서 목록**으로 주는 형태인데, 이 ViewModel 의 핵심이 "한 인텐트가 두 번 조회하는" 복구 경로라
   몇 번째 호출에 무엇이 왔는지가 곧 검증 대상이기 때문이다. `gate`(CompletableDeferred)로 진행 중 재진입을 재현한다.
   `exploreChallenge`/`exploreResult` 픽스처 빌더도 여기 뒀다.

이펙트 채널 수집 코드는 없다 — 이 ViewModel 은 `NoEffect` 라 이펙트가 없다. (`effect.first()` 로 걸려 60초 타임아웃 나는
흔한 사고를 애초에 만들 일이 없다.)

## 경로를 어떻게 열거했는가

- **인텐트**: `sealed interface ExploreListIntent` 의 8종 전부. 파일 하단에 `else` 없는 `when` 으로
  `ExploreListIntent.coveredBy()` 를 두어, 인텐트가 늘면 컴파일이 깨져 "테스트 안 썼다"가 드러나게 했다.
  같은 방식으로 케이스 층에는 `EmptyReason.coveredBy()` 를 뒀다.
- **에러 요인**: `recoverOrFail` 의 `when` 을 그대로 따라갔다 — `InvalidSortTypeException`(되돌릴 여지 있음/없음 2갈래),
  `InvalidFilterValueException`(2갈래), `CursorInvalidException`(첫 페이지/이어붙이기 2갈래), 그 외 일반 실패.
  `loadMore` 는 실패 처리 분기가 따로라(커서 손상 vs 나머지) 그 둘도 각각 하나씩.
- **상태 경로**: 로딩 시작 → 성공 → 실패 세 지점 + "이어붙이기 실패는 목록을 지우지 않는다"는 별도 규칙.
- **nullable 인자**: `Load(category, sort)` 의 있음/없음, `nextCursor` 의 있음/없음.
- **중복 방지 잠금 3종**: `loaded`(재진입), `canLoadMore`(연타·진행 중), `impressed`(세션 내 노출 중복).

## 일부러 하지 않은 것

- **`ExploreFilter`·`ExploreSort` 값 규칙** — `challenge/domain` 의 `ExploreTest` 가 이미 케이스 층에서 끝냈다. 다시 안 훑는다.
- **이벤트 이름·속성 스키마 전체 고정** — `ChallengeEventsTest`(케이스 층)가 팩토리 출력을 이미 고정한다. 모듈 층에서는
  "그 이벤트가 이 순간에 몇 번 나갔는가"와 호출부가 계산해 넣는 값(`result_count`·`position`·`page_index`·`entry`·`sort_from`)만 본다.
- **`emptyReason` 조합을 ViewModel 에서 재검증** — 케이스 층으로 내렸다(위 참조).
- **`reduce` 직접 호출** — `protected` 이고, 실제 경로는 `onIntent` 다.
- **화면 렌더·문구** — UI 층 소관이고 기대값 출처가 Figma 라 여기서 베끼면 동어반복이 된다.
- **`TEST_STRATEGY.md` 갱신** — 레포에 아직 그 문서가 없다. 신설은 커버리지 격자 전체를 뜨는 별도 작업 단위라
  이 작업(ViewModel 테스트)과 한 PR 에 섞지 않았다. 신설할 때 이 작업분으로 올릴 항목은
  "challenge:presentation 모듈 층 신설(ViewModel 실행 기반 포함), 미검증: explore 목록 UI 층".

## 작업 중 발견한 것 (테스트로 고정하지 않음)

`fetchFirstPage` 의 `CursorInvalidException` 복구는 **같은 조건으로 무한히 재조회**한다(첫 페이지엔 커서가 없어
같은 실패가 반복되면 멈출 조건이 없다). 재현 테스트를 쓰면 그대로 무한 루프에 빠져 CI 가 멈추므로 넣지 않았다.
서버가 커서 없는 요청에 `CURSOR_INVALID` 를 주지 않는다는 가정에 기대고 있는 코드이니, 재시도 상한을 둘지
별도 이슈로 논의가 필요하다.

## 실행 (이 워크스페이스에서는 돌리지 않았다)

```bash
./gradlew :challenge:presentation:testDebugUnitTest --tests "*ExploreList*"
./gradlew ktlintFormat && ./gradlew test
```
