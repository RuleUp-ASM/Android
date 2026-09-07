`challenge/domain/.../entity/ExploreSort.kt` 에 정렬 enum 을 넣었습니다. (Explore 관련 타입이 이미
`entity/Explore.kt` 에 모여 있으므로, 실제 반영 시에는 새 파일 대신 그 파일 안으로 넣는 편이 맞습니다.)

## 무엇을 어디에 뒀나

- **enum + `value: String`** — 서버 계약이라 와이어 값을 명시했습니다. 프로퍼티명과 같아도 문자열을
  적어 둬야 리네임·난독화에도 값이 남습니다.
- **`fromValue` 는 `POPULAR` 폴백** — 요청대로입니다. 예외를 던지지 않는 쪽이라, 서버가 정렬을
  하나 추가해도 구버전 앱이 목록을 통째로 못 여는 일은 없습니다.
- **`excludesLowSample` 파생 프로퍼티** — 화면이 `sort == COMPLETION_RATE || sort == SUCCESS_FAIL_RATIO`
  를 직접 쓰면 그 조건이 화면마다 복제되고 한쪽만 고쳐집니다. 판단에 이름을 붙여 타입에 올렸습니다.
  빈 결과 문구는 이 값 하나로 갈리면 됩니다.

## 주석으로 남긴 것 / 남기지 않은 것

- **표본 10건 규칙은 KDoc 한 줄로만** 남겼습니다. 제외는 서버가 하고 앱은 결과만 받으므로,
  `MIN_SAMPLE = 10` 같은 상수를 두면 아무도 안 쓰는 숫자가 domain 에 남아 서버 정책이 바뀔 때
  조용히 거짓말이 됩니다. 코드에 근거가 없는 바깥 계약이라 주석의 자리입니다.
- **상수 6개에는 주석을 달지 않았습니다.** 이름이 곧 정렬 기준이라 한국어로 옮겨 적으면 정보가
  0인 주석이 됩니다. 다만 `COMPLETION_RATE`·`SUCCESS_FAIL_RATIO` 의 정확한 산식(무엇 대비 무엇의
  비율인지)은 못 받아서 비워 뒀습니다 — 알려주시면 상수 위에 한 줄로 붙이겠습니다.

## 확인이 필요한 것 두 가지

1. 레포에 이미 `ExploreSort` 가 있습니다(`challenge/domain/.../entity/Explore.kt`). 지금 것과
   두 군데가 다릅니다 — **`LATEST` 가 아니라 `RECENT`** 이고, **`fromValue` 가 `null` 을 돌려줍니다.**
   신설이 아니라 기존 enum 수정이 맞을 텐데, `RECENT` → `LATEST` 는 서버 값 자체가 바뀐 건지
   확인이 필요합니다. 와이어 값이 그대로 `RECENT` 라면 상수명만 바꾸면 안 됩니다.
2. 폴백을 `POPULAR` 로 접으면 **사용자가 고른 정렬과 화면에 표시되는 정렬 칩이 어긋날 수 있습니다.**
   서버가 `INVALID_SORT_TYPE` 400 을 주는 경로(`InvalidSortTypeException`)와 겹치는데, 응답 파싱
   폴백은 조용히 인기순으로, 요청 거절은 예외로 처리하는 지금 구조가 의도한 대로인지 봐주세요.

테스트도 같이 넣었습니다(`ExploreSortTest.kt`) — 6종 목록·기본값, `excludesLowSample` 구분,
미인식 값 폴백 세 가지입니다.
