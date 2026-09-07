# NOTES

## 어느 층을 골랐나

**domain 유닛 테스트(`:challenge:domain` 의 `src/test`) 한 층만** 썼다. 대상인 `Challenge.kt` 는 전부
협력자 없는 값 타입(enum·data class·예외)이라 판정에 필요한 입력이 전부 인자로 들어온다. 리포지토리도
코루틴도 안드로이드 프레임워크도 끼지 않으므로 fake·Robolectric·`runTest` 가 필요 없고, `kotlin.test` 의
순수 함수 호출로 끝난다. 같은 규칙을 ViewModel 이나 data 매퍼에서 확인하면 조합이 폭발하고,
정작 규칙이 깨졌을 때 실패 지점이 규칙에서 멀어진다.

이웃 테스트(`JoinBlockReasonTest`, `ExploreTest`, `RoutineDescriptionTest`)와 같은 형식을 따랐다 —
`kotlin.test`, 한글 백틱 테스트명, 파일 하나에 타입별 테스트 클래스 여러 개, "왜 이게 중요한가"만 주석.

## 경로를 어떻게 열거했나

`Challenge.kt` 의 공개 선언을 위에서 아래로 훑고, 각각 **분기 또는 계약이 있는지**로 걸렀다.

1. `fromValue` 를 가진 enum 4종(`ChallengeMode`·`ChallengeVisibility`·`ChallengeStatus`·`ChallengeField`)
   → 알려진 값 전부 매핑 / 모르는 값 / `null` / 대소문자 어긋남. 여기에 `entries.size` 를 함께 못 박아
   enum 이 늘거나 줄면 테스트가 깨지게 했다.
2. 파생 프로퍼티 2종(`ChallengeMode.isGroup`, `ChallengeVisibility.isPrivate`) → true/false 양쪽.
3. 기본값이 계약인 타입 → `ChallengeUpdate`(모든 필드 null = 미변경, `removeImage=false`),
   `ChallengeNotEditableException(editableFields = emptySet())`, `ModerationLockedException(retryAfterSeconds = null)`.
4. `init` 검증이 있는 `CreateChallengeCommand` → 이미 `CreateChallengeCommandTest` 가 거부 경로를 덮고
   있었다. 빠져 있던 **경계 통과**(weeklyCount 1·7, capacity 1·10,000)만 그 파일에 한 테스트로 보탰다.
   거부만 있으면 범위를 한 칸 좁혀도 테스트가 통과한다.

각 `fromValue` 가 실제로 어떻게 쓰이는지도 확인했다 — `challenge/data` 의 응답 매핑에서
`?: 기본값` 과 `mapNotNull` 로 소비된다. 그래서 "모르는 값 → null" 이 장식이 아니라, 서버가 값을 늘렸을 때
앱이 죽지 않고 기본값/필드 누락으로 떨어지는 실제 계약이다. 테스트 주석에 그 이유를 적었다.

## 일부러 테스트하지 않은 것

- **동작 없는 값 홀더**: `ChallengePeriod`, `ChallengePenalties`, `ChallengeModeration`, `ChallengeDraft`,
  `RoutineTemplate`, `CreatedChallenge`, `ChallengeConfig`, `ChallengeSettings`, `ChallengeUpdateResult`,
  `DeleteResult`, `DraftResult`. 분기도 불변식도 없어 테스트하면 생성자 인자를 그대로 읽는 동어반복이 된다.
  (`ChallengeUpdate` 만 예외로 넣은 이유는 "null = 미변경" 이라는 **기본값 자체가 서버 계약**이기 때문이다.)
- **`ModerationState`**: `fromValue` 가 없고 EXEMPT→APPROVED 합치기를 data 가 한다. 그 매핑 규칙은
  `challenge/data` 의 DTO 테스트에서 볼 일이지 domain 엔티티 테스트의 몫이 아니다.
- **`ChallengeLimits` 상수 자체**: 상수를 상수와 비교하는 테스트는 값을 고칠 때 같이 고쳐질 뿐 아무것도
  막지 못한다. 대신 경계 통과/거부 테스트가 상수를 **사용해서** 범위를 고정한다.
- **화면 쪽 입력 차단**: 같은 범위를 화면도 막지만 그건 UX 라 presentation 층 소관이다(레포 규칙: 범위값은
  domain, 입력 차단은 view).
- **실행/검증**: 지시대로 gradle 을 돌리지 않았다. 컴파일·ktlint 검증은 하지 않은 상태다.

## 산출물

- `challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/ChallengeTest.kt` (신규)
- `challenge/domain/src/test/kotlin/com/ruleup/challenge/domain/entity/CreateChallengeCommandTest.kt`
  (기존 파일 + 경계 통과 테스트 1개)
