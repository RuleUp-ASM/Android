# ExploreListViewModel 테스트 — 작업 메모

## 고른 층: 모듈 층 (+ 아래층 보강 1개)

`ExploreListViewModel` 은 인텐트를 받아 repository 를 부르고, 그 결과를 상태·이동·관측 이벤트로 옮긴다.
**이 규칙이 깨지면 고칠 파일이 `ExploreListViewModel.kt` 하나**라서 모듈 층이다. `presentation` 안에 있다고
UI 층이 아니다 — 이 화면의 Composable 이 옳게 그리는지는 별개 문제고 여기서 보지 않는다.

작업 중 아래층 구멍이 하나 드러나서 함께 채웠다. `ExploreListState.emptyReason` / `canLoadMore` 는
협력자 없이 값만으로 성립하는 파생 규칙(= 케이스 층)인데 테스트가 없었다. ViewModel 테스트에서
"필터 걸고 0건이면 FILTERED" 같은 조합을 훑기 시작하면 층을 건너뛴 검증이 되고 조합이 곱으로 늘어난다.
그래서 `ExploreListStateTest` 로 내리고, ViewModel 테스트는 그 조합을 한 번도 보지 않는다.

## 세운 테스트 실행 기반

이 레포에는 ViewModel 테스트가 하나도 없었다. 새 프레임워크는 들이지 않고 기존 컨벤션
(`kotlin.test` + JUnit4, 목 라이브러리 없이 손으로 쓴 Fake, 백틱 한국어 이름) 위에 세 가지를 얹었다.

1. **`challenge/presentation/build.gradle.kts`**
   - `testImplementation(libs.kotlinx.coroutines.test)` — `viewModelScope` 가 `Dispatchers.Main` 을 쓰는데
     JVM 테스트에는 Main 이 없어 `Dispatchers.setMain` 으로 갈아끼워야 한다. 버전 카탈로그에 이미
     있는 항목이라 새 버전을 올리지 않았다.
   - `testImplementation(testFixtures(project(":core:domain")))` / `(":observability:domain")` — 아래 2·3번.

2. **`core/domain` 에 `java-test-fixtures` + `RecordingNavigationHelper`**
   이 레포의 ViewModel 은 화면 이동을 이펙트가 아니라 `NavigationHelper` **호출**로 한다. 기록하는 대역이
   없으면 "실패했는데도 다음 화면으로 넘어갔다"를 어떤 테스트도 못 잡는다. `observability:domain` 이
   `RecordingSink`·`testObservability` 를 testFixtures 로 내주는 것과 같은 방식으로 `core:domain` 에 뒀다.
   `navigationFlow` 가 `Flow` 라서 `testFixturesImplementation(libs.kotlinx.coroutines.core)` 를 함께 걸었다
   (모듈의 `implementation` 은 testFixtures 컴파일 경로로 오지 않는다). `java-library` 도 같이 붙였다 —
   `observability:domain` 과 같은 조합이다.

3. **`FakeExploreRepository`** (`challenge/presentation` test 소스셋)
   응답을 **큐**로 받는다. 이 ViewModel 은 한 인텐트에 두 번 조회하는 경로(정렬·필터 거절 후 자가 복구,
   커서 손상 후 첫 페이지 재요청)를 갖고 있어서, "첫 호출은 거절 / 두 번째는 성공"을 그대로 적을 수
   있어야 복구가 실제로 도는지 볼 수 있다. 큐가 비면 빈 결과를 돌려주므로 자가 복구가 무한히
   되풀이돼도 테스트가 멈추지 않고 `calls` 로 드러난다. `explore` 외의 메서드는 호출되면 실패한다.
   응답 도착 전 재요청(연타)을 재현하려고 `gate: CompletableDeferred` 를 뒀다.

이펙트 채널은 다루지 않았다 — 이 화면은 `NoEffect` 라 이펙트가 없다. `viewmodel.md` 가 경고하는
"effect 를 수집하지 않은 채 `first()` 로 멈추는 사고"는 여기서 성립하지 않는다.

## 경로를 어떻게 열거했나

**해피 경로는 `sealed interface ExploreListIntent` 에서 읽었다.** 8종(Load·LoadMore·ApplyFilter·SelectSort·
ClearEligibleOnly·CardImpression·OpenChallenge·Back) 전부에 테스트가 있고, 근거는 파일 하단의
`ExploreListIntent.coveredBy()` — `else` 없는 `when` 이라 **인텐트가 늘면 컴파일이 깨진다**. 아무도
호출하지 않아도 되고 존재 자체가 열거의 근거다. 인텐트 안의 갈래도 타입에서 뽑았다:
`Load(category: String?, sort: String?)` 의 nullable 두 개 → 있음/없음/모르는 값,
`nextCursor: String?` → 남음/없음, `loaded` 잠금 → 첫 진입/재진입.

**에러 요인은 `recoverOrFail` 과 domain 예외 목록에서 읽었다.** `InvalidSortTypeException`·
`InvalidFilterValueException` 은 각각 "고칠 게 남았을 때 = 재조회" / "이미 기본값일 때 = 오류"로 갈리므로
둘씩, `CursorInvalidException` 은 첫 페이지·다음 페이지에서 처리가 달라 각각, 그 외 예외는 첫 페이지
(오류 문구)·다음 페이지(목록 유지 + 하단 재시도)로 각각. 메시지가 없는 예외의 기본 문구도 하나.

**관측은 릴리즈 게이트라 개수까지 봤다.** 진입 1회·빈 결과·필터 적용(result_count)·정렬 변경(from→to)·
노출(세션 중복 제거)·클릭(position)·스크롤 깊이(page_index). 이벤트 **스키마**는 다시 고정하지 않고
`ChallengeEvents` 팩토리 출력과 통째로 비교했다 — 스키마는 `ChallengeEventsTest`(케이스 층) 소관이고,
여기서는 "ViewModel 이 옳은 인자를 넘겼는가"만 본다.

결과: ViewModel 26개, State 7개.

## 일부러 테스트하지 않은 것

- **정렬 6종·필터 배지 수·csv 직렬화** — `ExploreSortTest`/`ExploreFilterTest`(케이스 층)가 끝냈다.
- **이벤트 이름·속성 키** — `ChallengeEventsTest` 가 골든으로 고정한다.
- **화면 렌더** — 빈 사유별 문구, 하단 "다시 불러오기" 버튼, 스크롤 하단 근접 판정(뷰포트 50%·1초)은
  UI 층(Robolectric)이다. 이 레포에 아직 그 기반이 없어 별도 작업 단위다.
- **`reduce` 직접 호출** — `protected` 이고, 전이는 `onIntent` 를 통해 봐야 실제 경로다.
- **`size` 파라미터** — ViewModel 이 한 번도 넘기지 않아 서버 기본값에 맡긴다. Fake 는 받기만 한다.
- **`ExploreRepository.getTrending`/`getCategories`/`clone`** — 이 화면이 쓰지 않는다. Fake 에서 호출되면
  실패하게 두어, 나중에 누가 부르면 조용히 지나가지 않게 했다.
- **`TEST_STRATEGY.md`** — 레포에 아직 없다. 스킬의 `strategy-doc.md` 는 처음 만들 때 3절(미검증)·4절
  (인수 시나리오)을 **사용자와 함께** 골라야 한다고 못박는다(지어내지 말 것). 이 작업에서 임의로
  만들면 추측이 되고 범위도 넘어서므로, 별도 작업 단위로 남긴다. 이번에 채운 칸은
  "challenge:presentation × 모듈 층", 남는 큰 구멍은 "UI 층 전부"다.

## 작업 중 발견한 것 (테스트가 아니라 리포트)

1. **첫 페이지 `CursorInvalidException` 이 무한 루프가 될 수 있다.** `recoverOrFail` 은 이 예외에
   `fetchFirstPage(filter, sort)` 를 같은 인자로 다시 건다. 첫 페이지 요청은 `cursor = null` 이라 애초에
   커서 오류가 날 자리가 아니지만, 서버가 이 코드를 잘못 내려주면 종료 조건 없이 되풀이된다.
   재시도 횟수 제한이 없어 **테스트로 고정하면 테스트가 멈춘다** — 그래서 테스트 대신 여기 적는다.
2. **`logImpression` 이 목록에 없는 카드도 `impressed` 에 먼저 넣는다.** `impressed.add()` 가 아이템
   조회보다 앞이라, 아직 목록에 없는 카드의 노출 인텐트가 한 번 들어오면 그 카드는 이후 목록에
   나타나도 영원히 노출로 기록되지 않는다. 지금 화면 구조에선 일어나기 어렵지만 순서를 뒤집으면
   공짜로 없어지는 위험이다.

두 건 모두 프로덕션 코드 변경이라 이 작업(테스트 추가)에서는 건드리지 않았다.

## 실행 (참고 — 이번 평가에서는 돌리지 않음)

```bash
./gradlew :challenge:presentation:testDebugUnitTest --tests "*ExploreListViewModelTest*"
./gradlew :challenge:presentation:testDebugUnitTest --tests "*ExploreListStateTest*"
./gradlew ktlintFormat && ./gradlew test
```

## 산출물

| 파일 | 성격 |
|---|---|
| `core/domain/build.gradle.kts` | 수정 — `java-library`·`java-test-fixtures`·`testFixturesImplementation(coroutines)` |
| `core/domain/src/testFixtures/.../RecordingNavigationHelper.kt` | 신규 — 공용 대역 |
| `challenge/presentation/build.gradle.kts` | 수정 — coroutines-test·testFixtures 2종 |
| `challenge/presentation/src/test/.../fake/FakeExploreRepository.kt` | 신규 — Fake + 빌더 |
| `challenge/presentation/src/test/.../viewmodel/ExploreListViewModelTest.kt` | 신규 — 모듈 층 26개 |
| `challenge/presentation/src/test/.../viewmodel/ExploreListStateTest.kt` | 신규 — 케이스 층 7개 |
