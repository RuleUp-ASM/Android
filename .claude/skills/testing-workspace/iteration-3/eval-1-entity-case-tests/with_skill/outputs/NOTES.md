# 작업 노트 — `Challenge` 엔티티 테스트

## 산출물

- `challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/ChallengeTest.kt` (신규, 테스트 11개)

## 고른 층: 케이스

기준은 "이 규칙이 깨지면 어느 파일을 고치나"다. `ChallengeMode.fromValue("SOLO")` 가 null 을 주면 고칠 파일은
`Challenge.kt` 하나다 — 협력자도 코루틴도 없다. 그래서 케이스 층이고, Fake 없이 `kotlin.test` 만 쓴다.
이웃(`ExploreTest.kt`·`JoinBlockReasonTest.kt`)의 형태를 그대로 따랐다: enum 은 `entries` 순회 + 개수 단언,
"모르는 값은 null" 을 따로 한 개.

한 파일에 여러 테스트 클래스를 모으는 기존 관례(`ExploreTest.kt` 안의 `ExploreSortTest`·`ExploreFilterTest`)를
따라 `ChallengeTest.kt` 하나에 `ChallengeModeTest` · `ChallengeVisibilityTest` · `ChallengeStatusTest` ·
`ChallengeFieldTest` · `ModerationStateTest` 를 담았다. CI 리포트에는 클래스가 아니라 이름이 뜨므로
이름마다 주어를 넣어(참여 형태 / 공개 범위 / 생애주기 / 수정 폼 필드) 파일 안에서 겹치지 않게 했다.

## 경로를 어떻게 열거했나

`Challenge.kt` 의 선언을 위에서 아래로 훑어 **분기를 가진 타입만** 뽑았다.

| 타입 | 분기 출처 | 어떻게 열거했나 |
|---|---|---|
| `ChallengeMode` | enum 2종 + `isGroup` + `fromValue` | `entries.map { it.value }` 를 기대 리스트와 통째로 비교 → 값이 늘면 자동 실패. `entries.filter { it.isGroup }` 로 파생 속성도 순회로 고정 |
| `ChallengeVisibility` | enum 2종 + `isPrivate` + `fromValue` | 위와 같음 |
| `ChallengeStatus` | enum 3종 + `fromValue` | 위와 같음 |
| `ChallengeField` | enum 13종, **이름(SNAKE) ≠ 요청 키(camel)** | 키→값 map 을 손으로 나열하고 `expected.size == entries.size` 로 "지금 몇 개인지 안다"를 못 박음. 키 중복도 별도 단언 |
| `ModerationState` | enum 4종, `EXEMPT` 를 `APPROVED` 로 접는 정책 | `entries.map { it.name }` 리스트 고정 1개만 |

열거를 끝냈다는 근거는 전부 **`Enum.entries` 순회 또는 기대 리스트 전체 비교**다 — 값이 하나 늘면 손대지
않아도 빨개진다. 주석으로 "다 다뤘다"고 쓴 곳은 없다.

## 일부러 테스트하지 않은 것

- **`CreateChallengeCommand` 의 불변식** — `CreateChallengeCommandTest` 가 이미 케이스 층에서 본다.
  같은 경로를 두 번 검증하지 않는다.
- **`ChallengePeriod`·`ChallengeModeration`·`ChallengePenalties`·`ChallengeConfig`·`ChallengeSettings`·
  `ChallengeUpdate`·`ChallengeDraft`·`CreatedChallenge`·`DraftResult`** — 로직 없는 데이터 홀더다.
  `copy`·`equals` 는 컴파일러가 보장하므로 테스트할 값이 없다. `ChallengeUpdate.removeImage` 의 "null=미변경 /
  removeImage=기본 이미지" 계약은 **직렬화 시점에 갈리므로 data 층 요청 DTO 테스트 몫**이다.
- **`ChallengeLimits` 상수 값 자체** — 상수를 다시 적어 비교하는 건 동어반복이다. 이 값들은
  `CreateChallengeCommandTest` 가 경계로 이미 밟는다.
- **예외 클래스들**(`ChallengeNotEditableException`·`InvalidWeeklyCountException`·
  `ChallengeVersionConflictException`·`ModerationLockedException`) — 던지는 주체가 data 의 상태코드 변환이라
  케이스 층에서 볼 게 없다. 409/429 → 도메인 예외 변환은 `RepositoryImpl` 테스트(모듈 층)에서 잡아야 한다.
- **`"EXEMPT" → APPROVED` 실제 매핑** — `challenge/data` 의 `moderationState()` 가 `private` 이라 domain 에서
  닿지 않는다. DTO 의 `toDomain()` 을 통과시키는 **data 층 매퍼 테스트**가 있어야 잡힌다. 지금은 없다.

## 남아 있는 구멍 (고치지 않고 보고만 한다)

1. **`CreateChallengeCommandTest` 에 경계 *안쪽* 단언이 없다.** `weeklyCount` 0·8 과 `capacity` 0·10,001 은
   막는지 보지만, 1·7 과 1·10,000 이 **통과하는지**는 아무도 안 본다. `require` 를 `in 2..6` 으로 좁혀도 초록이다.
   또 그 파일은 리터럴(`0`, `8`, `10_001`)을 쓰는데 `ChallengeLimits` 상수를 쓰면 범위가 바뀔 때
   테스트가 따라 움직여야 하는지가 그 자리에서 드러난다. 기존 파일 수정이라 요청 범위 밖이므로 손대지 않았다.
2. **`challenge/data` 의 응답 매퍼에 테스트가 하나도 없다.** 이 파일이 고정한 enum 계약은 "값이 무엇인가"까지고,
   `ChallengeMode.fromValue(mode) ?: SOLO` 처럼 **모르는 값을 무엇으로 접는가**는 매퍼 테스트가 있어야 잡힌다.
3. **`TEST_STRATEGY.md` 가 레포에 없다.** 스킬 절차 8번은 갱신을 요구하지만, `references/strategy-doc.md` 는
   그 문서를 처음 만들 때 `coverage_map.py` 출력과 **"메울 것 / 안 메울 것" 사용자 합의**를 전제로 한다.
   혼자 지어내면 60줄짜리 거짓 할 일 목록이 되므로 만들지 않았다. 만들 때 3절(미검증)에 위 1·2번을 올린다.

## 실행 (참고 — 이번엔 돌리지 않았다)

```bash
./gradlew :challenge:domain:testDebugUnitTest --tests "*ChallengeModeTest*"
./gradlew :challenge:domain:testDebugUnitTest --tests "*Challenge*Test*"
./gradlew ktlintFormat && ./gradlew test
```

`:challenge:domain` 은 android library 라 `testDebugUnitTest` 이고, 모듈 스코프를 빼면 다른 모듈에서
"No tests found" 로 깨진다. 새 의존성은 필요 없다 — `kotlin("test-junit")` 만 쓰므로 `build.gradle.kts` 는
그대로다.
