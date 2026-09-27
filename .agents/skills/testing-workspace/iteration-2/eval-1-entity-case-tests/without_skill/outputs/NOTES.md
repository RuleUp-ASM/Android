# NOTES

## 어느 층을 골랐나

**케이스(엔티티) 층 · 순수 유닛 테스트** 하나만 골랐다.

`challenge/domain/entity/Challenge.kt` 안의 대상들은 전부 협력자가 없다 — enum 의 `fromValue`/파생 프로퍼티,
`object ChallengeLimits` 의 상수, `data class` 들의 `init` 검증뿐이다. repository·코루틴·안드로이드 프레임워크가
끼어들지 않으므로 모듈 층(UseCase/Repository)이나 UI 층으로 올릴 이유가 없다. 실행도 JUnit 만으로 끝난다.

- 파일: `challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/ChallengeTest.kt`
- 기존 컨벤션(`ExploreTest.kt`)을 따라 **한 파일에 타입별 테스트 클래스**를 나눠 담았다
  (`ChallengeModeTest`, `ChallengeVisibilityTest`, `ChallengeStatusTest`, `ChallengeFieldTest`, `ChallengeLimitsTest`).
- 테스트 이름은 백틱 한글 문장, 단언은 `kotlin.test` — 레포의 기존 도메인 테스트와 같다.
- `build.gradle.kts` 는 손대지 않았다. `challenge:domain` 에 `testImplementation(kotlin("test-junit"))` 이 이미 있다.

## 경로를 어떻게 열거했나

1. `Challenge.kt` 의 최상위 선언을 전부 훑어 **행동이 있는 것**과 **값만 나르는 것**을 갈랐다.
   행동이 있는 건 enum 4종(`ChallengeMode`, `ChallengeVisibility`, `ChallengeStatus`, `ChallengeField`)의
   `fromValue`·`isGroup`·`isPrivate`, `ChallengeLimits` 의 상수, `CreateChallengeCommand.init` 뿐이다.
2. 기존 테스트(`CreateChallengeCommandTest`, `JoinBlockReasonTest`, `ExploreTest`, `RoutineDescriptionTest`)를 읽어
   **이미 덮인 범위**를 뺐다. `CreateChallengeCommand` 의 조합 규칙과 범위 거절은 이미 있다.
3. 각 enum 에 대해 세 갈래를 열거했다 — (a) 정의된 값 목록 자체, (b) 모든 entry 의 왕복(`value` → `fromValue`),
   (c) 모르는 값·`null` → `null`. 파생 boolean 은 개별 값 대신 `entries.filter { ... }` 로 훑어
   **새 enum 값이 늘어도 판정이 빠지지 않게** 고정했다.
4. 이 값들이 실제로 어디서 소비되는지 `grep` 으로 확인해 "깨지면 무엇이 조용히 잘못되는지"를 주석에 남겼다.
   - `challenge/data/.../ChallengeResponse.kt`, `ChallengeCommonDto.kt` 가 `fromValue(...) ?: 기본값` 으로 받는다
     → 문자열이 어긋나면 예외 없이 기본값으로 떨어진다(SOLO↔GROUP, UPCOMING↔ACTIVE 오표시).
   - `ChallengeField` 는 PATCH 폼 잠금 키다. 서버가 모르는 키를 **조용히 버리므로** 오타는 "수정했는데 안 바뀜"으로만 드러난다.
5. `ChallengeLimits` 는 기존 테스트가 **거절 쪽만** 보고 있어 off-by-one 이 새는 구멍이 있었다.
   경계값(1·7·1·10,000)이 **통과하는지**와 상수값 자체를 추가로 못 박았다.

## 일부러 테스트하지 않은 것

- **`CreateChallengeCommand` 의 조합 규칙**: `CreateChallengeCommandTest` 가 이미 덮는다. 중복해서 쓰지 않았다.
  경계 **허용** 케이스만 `ChallengeLimitsTest` 로 보탰다.
- **순수 값 운반 data class** (`ChallengePeriod`, `ChallengeDraft`, `ChallengeConfig`, `ChallengeSettings`,
  `ChallengeModeration`, `ChallengePenalties`, `CreatedChallenge`, `ChallengeUpdateResult`, `DeleteResult`,
  `RoutineTemplate`, `DraftResult`): 로직이 없다. 컴파일러가 만들어 준 `equals`/`copy` 를 테스트하는 건
  코드가 아니라 코틀린을 검증하는 일이다.
- **`ModerationState`**: 행동 없는 enum 이고, 의미 있는 규칙(`EXEMPT` 를 `APPROVED` 로 합치기)은 domain 이 아니라
  `challenge/data/dto/ChallengeCommonDto.kt` 의 `moderationState()` 에 있다. 그건 **data 층 매퍼 테스트** 소관이라
  이번 범위(엔티티) 밖으로 뒀다. 여기서 흉내 내면 규칙이 두 군데로 갈라진다.
- **`ChallengeUpdate` 의 기본값**(`removeImage = false`, 나머지 `null` = 미변경): 계약은 "null 이면 직렬화에서 뺀다"인데
  그 **뺀다**는 행위가 domain 이 아니라 data 의 DTO 매핑에 있다. 여기서 기본값만 확인하면 정작 중요한
  "전송되지 않는다"는 보장이 안 잡히므로 data 층에 남겨 뒀다.
- **예외 4종** (`ChallengeNotEditableException`, `InvalidWeeklyCountException`,
  `ChallengeVersionConflictException`, `ModerationLockedException`): 생성자 인자를 그대로 담는 것 외에 로직이 없다.
  의미 있는 지점은 "서버 에러 코드 → 이 예외" 매핑이고, 그건 data 층에 있다.
