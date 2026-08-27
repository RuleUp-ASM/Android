`challenge:domain` 의 entity 로 `ExploreSort` 를 추가했습니다.

- 서버 값 6종(`POPULAR`, `PARTICIPANTS`, `COMPLETION_RATE`, `SUCCESS_FAIL_RATIO`, `LATEST`, `DEADLINE`)을 `value` 로 들고 있고, 기존 도메인 enum 컨벤션(`Tier`, `Category`)과 같이 `value` 파라미터 + `fromValue` 조합입니다.
- `default = POPULAR`. `fromValue` 는 모르는 값이 오면 null 대신 `default` 로 떨어뜨립니다(`Tier.fromValue` 가 미지 값을 BRONZE 로 떨어뜨리는 것과 같은 방식).
- 표본 미달 제외 여부는 `excludesLowSample` 로 노출했습니다. 화면은 결과가 0건일 때 이 값으로 "조건에 맞는 방이 없어요"와 "아직 데이터가 모인 방이 없어요"를 가르면 됩니다.
- 임계값 10은 `MIN_SAMPLE` 상수로 두었습니다. 실제 필터링은 서버가 하므로 클라이언트는 문구·안내용으로만 씁니다(예: "판정 10건 이상인 방만 보여요").

확인이 필요한 지점 하나:

- 레포의 `challenge/domain/.../entity/Explore.kt` 에 이미 `ExploreSort` 가 있고, 거기서는 최신순 값이 `LATEST` 가 아니라 **`RECENT`** 입니다(테스트 `ExploreSortTest` 도 `RECENT` 를 고정하고 있습니다). 또 현재 `fromValue` 는 미지 값에 null 을 반환하도록 테스트가 잠가 두었습니다. 이번 요청대로 `LATEST` + POPULAR 폴백으로 가려면 기존 enum·테스트를 함께 고쳐야 하는데, 서버 계약이 `RECENT` → `LATEST` 로 바뀐 것이 맞는지 확인 후 진행하겠습니다. 맞다면 `Explore.kt` 의 기존 enum 을 교체하고 `ExploreSortTest` 의 값 목록과 폴백 케이스를 같이 수정하면 됩니다.
