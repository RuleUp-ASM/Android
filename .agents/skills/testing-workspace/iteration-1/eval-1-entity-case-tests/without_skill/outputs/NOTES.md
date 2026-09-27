# NOTES

## 어느 층을 골랐나 — 케이스(엔티티) 단위 순수 테스트

`Challenge.kt` 는 협력자가 없는 값 타입·enum·`init` 검증만 있는 파일이다. 협력자가 없으므로 fake·코루틴·
Robolectric 이 필요 없고, `challenge/domain` 의 `testImplementation(kotlin("test-junit"))` 만으로
`testDebugUnitTest` 에서 그대로 돈다. 같은 모듈의 기존 케이스 테스트(`RoutineDescriptionTest`,
`ExploreTest`, `JoinBlockReasonTest`)와 같은 층·같은 스타일로 맞췄다.

- 파일: `challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/ChallengeTest.kt` (신규)
- 파일: `.../entity/CreateChallengeCommandTest.kt` (기존 파일에 경계 통과 케이스만 추가)
- `build.gradle.kts` 는 손대지 않았다. 새 의존이 필요 없다.

## 경로를 어떻게 열거했나

1. `Challenge.kt` 를 통째로 읽어 선언을 분류했다 — enum 4종(`ChallengeMode`, `ChallengeVisibility`,
   `ChallengeStatus`, `ChallengeField`) + `ModerationState`, 파생 프로퍼티 2개(`isGroup`, `isPrivate`),
   `init` 검증 1곳(`CreateChallengeCommand`), 나머지는 로직 없는 data class·예외 클래스.
2. `grep -rln "ChallengeMode\|ChallengeVisibility\|ChallengeStatus\|ChallengeField" --include="*Test.kt"`
   로 기존 커버리지를 확인했다. 레포 전체에서 `CreateChallengeCommandTest` 하나뿐이었고, 그 파일은
   `init` 의 실패 경로만 본다. → **enum 4종의 `fromValue`·파생 프로퍼티는 전부 미커버**.
3. 각 enum 에 대해 (a) 명세 값 전건 왕복 매핑 (b) `entries.size` 로 항목 수 고정 (c) 모르는 값·`null` →
   `null` (d) 파생 프로퍼티의 참/거짓 양쪽, 이렇게 네 갈래를 돌렸다.
4. `init` 검증은 실패 경계(0/8, 0/10001)만 있고 **통과 경계(1/7, 1/10000)** 가 비어 있어 그것만 채웠다 —
   범위를 `1 until 7` 로 잘못 좁혀도 기존 테스트는 전부 통과한다.

## 일부러 테스트하지 않은 것

- **로직 없는 data class** (`ChallengeModeration`, `ChallengePeriod`, `ChallengePenalties`,
  `ChallengeDraft`, `ChallengeConfig`, `ChallengeSettings`, `ChallengeUpdate`, `CreatedChallenge`,
  `DeleteResult`, `RoutineTemplate`, `ChallengeUpdateResult`): 컴파일러가 만든 `equals`/`copy` 를 테스트하는
  꼴이라 회귀를 잡지 못한다.
- **`ModerationState`**: `fromValue` 가 없다. `EXEMPT → APPROVED` 합치기는 data 레이어 매퍼가 하므로
  그 검증은 `:challenge:data` 의 매퍼 테스트 몫이지 domain 케이스 테스트가 아니다.
- **예외 클래스 4종** (`ChallengeNotEditableException` 등): 메시지 문자열과 생성자 기본값뿐이다. 어떤 HTTP
  코드가 어느 예외로 번역되는지가 진짜 계약인데, 그건 data 레이어 경계에서 봐야 한다.
- **`DraftResult` sealed 분기**: 분기 자체엔 로직이 없고, `Fallback` 을 에러로 그리지 않는다는 정책은
  presentation 이 지킨다 — 케이스 층에서 확인할 대상이 아니다.
- **`ChallengeDraft`/`ChallengeConfig` 의 `weeklyCount` 범위**: 이 타입들엔 `init` 검증이 없다(서버 응답을
  담는 수신 전용). 없는 불변식을 테스트로 요구하면 실패한다 — 검증을 붙일지는 별도 판단이 필요해 남겨 뒀다.
