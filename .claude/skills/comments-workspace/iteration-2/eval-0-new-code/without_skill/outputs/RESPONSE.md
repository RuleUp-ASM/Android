요청하신 enum 은 이미 `challenge:domain` 에 있습니다. 새로 만들면 중복이라, 있는 것과 요청을 대조했습니다.

## 이미 되어 있는 것

`challenge/domain/src/main/kotlin/com/ruleup/challenge/domain/entity/Explore.kt` 의 `ExploreSort`:

- 6종 정의 + `value` 로 서버 문자열 보유, `companion object { val default = POPULAR }`
- `excludesLowSample` (COMPLETION_RATE·SUCCESS_FAIL_RATIO 만 true) — 말씀하신 문구 분기 기준
- 화면도 이미 갈라 씁니다: `ExploreListContract.kt:79` 에서 `sort.excludesLowSample -> EmptyReason.LOW_SAMPLE`,
  `ExploreListScreen.kt:596` 에서 "아직 기록이 충분한 챌린지가 없어요" / else "조건에 맞는 챌린지가 없어요"
- 테스트도 `challenge/domain/src/test/.../entity/ExploreTest.kt` 에 있습니다

## 차이 나는 것 2가지

**1. 미인식 값 폴백 (수정안 준비함)**

지금 `fromValue` 는 nullable 이고, 폴백은 호출부(`ExploreListViewModel.kt:121` 의 `?: ExploreSort.default`)에
있습니다. 말씀대로 enum 안으로 올리는 게 맞다고 봅니다 — 같은 레포의 `ParamKind.fromValue` 가 이미
`?: NUMBER` 규약입니다. 바뀌는 곳은 세 군데뿐입니다:

- `Explore.kt` — `fun fromValue(value: String?): ExploreSort = entries.find { it.value == value } ?: default`
- `ExploreListViewModel.kt:121` — 엘비스 제거
- `ExploreTest.kt` — `모르는 정렬 값은 null 이다` → `기본 정렬로 떨어진다`

전체 코드는 옆의 `ExploreSort.kt`, `ExploreSortCallSites.kt` 에 뒀습니다.

참고로 이 폴백이 도는 경로는 딥링크·nav 인자뿐입니다. 서버는 sort 를 내려주지 않고 받기만 하며,
정의 밖 값은 400 `INVALID_SORT_TYPE` → `InvalidSortTypeException` 으로 이미 기본 정렬 복귀가 걸려 있습니다.

**2. `LATEST` vs `RECENT` — 확인이 필요합니다 (그래서 손대지 않았습니다)**

레포에는 `RECENT("RECENT")` 로 들어가 있습니다(2026-08-10 명세 반영 커밋 `ecbeef9`). 말씀은 `LATEST` 인데,
이건 그대로 쿼리 파라미터로 나가는 값이라 틀리면 400 `INVALID_SORT_TYPE` 입니다. 어느 쪽인가요?

- 서버가 `LATEST` 로 바뀐 게 맞다면: 상수명·value 를 바꾸고 호출부 4곳을 같이 고칩니다 —
  `ExploreSortSheet.kt:54`, `ExploreListScreen.kt:693`("최근 생성" 라벨),
  `ExploreTest.kt:15`, `ChallengeEventsTest.kt:26`. 딥링크에 옛 `RECENT` 가 남아 있어도 위 폴백으로
  인기순으로 열리니 안전합니다.
- 말씀이 축약이었고 서버는 `RECENT` 그대로라면: 이 항목은 변경 없음입니다.

**표본 10건**은 서버가 목록에서 빼는 컷이라 클라이언트에 값으로 들일 게 없어서 KDoc 에만 명시했습니다.
화면에 "판정 10건 이상" 같은 문구로 노출할 계획이면 알려주세요 — 그때 상수로 올리겠습니다.

## 다음

2번 답 주시면 그대로 반영하겠습니다. 아직 레포 파일은 건드리지 않았습니다.
