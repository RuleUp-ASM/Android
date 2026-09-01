# NOTES

## 고른 층: 케이스

`Challenge.kt` 안의 규칙은 전부 **값 하나에 붙어 있고 협력자가 없다** — enum ↔ 서버 문자열 매핑,
파생 불리언(`isGroup`·`isPrivate`), `CreateChallengeCommand` 의 `init` 불변식.
"이 규칙이 깨지면 어느 파일을 고치나"로 물으면 답이 전부 `Challenge.kt` 자신이므로 케이스 층이다.
Fake 가 필요해지는 지점이 하나도 없다는 점이 층 선택의 근거이기도 하다.

실행: `./gradlew :challenge:domain:testDebugUnitTest --tests "*ChallengeModeTest*"` 등 (android library 모듈).
`challenge/domain/build.gradle.kts` 에 `testImplementation(kotlin("test-junit"))` 이 이미 있어 **의존성 변경 없음**.

## 경로를 어떻게 열거했나

`Challenge.kt` 의 선언을 위에서 아래로 훑어 타입 종류별로 분류했다.

| 선언 | 분기 출처 | 처리 |
|---|---|---|
| `ChallengeMode` | enum 2종 + `fromValue` + `isGroup` | 전 값 매핑 · 미상 값 · `entries.filter { it.isGroup }` |
| `ChallengeVisibility` | enum 2종 + `fromValue` + `isPrivate` | 위와 동일 |
| `ChallengeStatus` | enum 3종 + `fromValue` | 전 값 매핑 · 미상 값 |
| `ChallengeField` | enum 13종 + `fromValue` | 전 값 매핑 · 미상 값 |
| `CreateChallengeCommand` | `init` 의 `require` 5개 · 수치 범위 2개 | 기존 테스트 + **안쪽 경계** 보강 |
| 나머지 data class / sealed | 동작 없음 | 테스트하지 않음 (아래) |

열거가 끝났다는 근거는 **`entries.size` 단언**과 **`entries.filter { … }` 순회**에 뒀다 —
서버가 값을 늘려 enum 에 상수가 추가되면 기대 목록과 개수가 어긋나 그 자리에서 빨개진다.
(`JoinBlockReasonTest` 가 쓰는 방식과 같다. 이웃 테스트를 먼저 읽고 그 형태에 맞췄다.)

미상 값 케이스는 `fromValue(null)` 과 `fromValue("모르는 값")` 두 갈래 모두 넣었다 —
솔로 챌린지의 `visibility` 처럼 **null 이 정상 입력**인 필드가 있어 "없음"과 "모름"이 같은 결과여야 한다.

## 만든 것

- `challenge/domain/src/test/.../entity/ChallengeTest.kt` (신규)
  `ChallengeModeTest`(3) · `ChallengeVisibilityTest`(3) · `ChallengeStatusTest`(2) · `ChallengeFieldTest`(2) = **10개**.
  파일명·다중 클래스 배치는 기존 `ExploreTest.kt`(안에 `ExploreSortTest`·`ExploreFilterTest`) 관례를 따랐다.
- `challenge/domain/src/test/.../entity/CreateChallengeCommandTest.kt` (기존 파일 수정)
  **안쪽 경계 2개 추가**: `주간 횟수 1회와 7회는 만들 수 있다`, `그룹 정원 1명과 10,000명은 만들 수 있다`.
  기존 테스트가 바깥쪽(0·8·0·10,001)만 막고 있어, 범위가 `2..6` 으로 좁아져도 아무것도 깨지지 않는 구멍이 있었다.
  리터럴 대신 `ChallengeLimits` 상수를 참조해 상수가 바뀌면 테스트가 따라 움직이도록 했다.
  *범위를 신규 파일로만 제한하고 싶다면 이 파일은 빼도 나머지는 그대로 성립한다.*

## 일부러 테스트하지 않은 것

- **`ChallengeLimits` 의 상수 값 자체** · **data class 의 `copy`·`equals`** — 컴파일러/언어가 이미 보장한다.
- **`ChallengeModeration`·`ChallengePeriod`·`ChallengePenalties`·`ChallengeDraft`·`ChallengeConfig`·
  `RoutineTemplate`·`CreatedChallenge`·`ChallengeSettings`·`ChallengeUpdateResult`** — 동작이 없는 운반 타입.
  검증할 규칙이 생기는 지점은 data 층 매퍼다(아래).
- **`DraftResult`(sealed)** — domain 에 이 타입을 소비하는 함수가 없다. 분기를 가르는 코드는 presentation 에 있으므로
  `when` exhaustive 를 걸 자리가 여기엔 없다.
- **`ModerationState` 문자열 매핑** — `EXEMPT`/`APPROVED` 합치기와 미상 값 → `NONE` 접기는
  `challenge/data/.../dto/ChallengeCommonDto.kt` 의 `private fun moderationState()` 에 있다.
  같은 케이스 층이지만 **모듈이 다르고**(`:challenge:data` 는 현재 테스트 소스가 0개), private 이라 노출 조정이 필요하다.
  이 작업 범위 밖 — 별도 작업 단위로 남긴다.
- **`ChallengeUpdate` 의 "null=미변경 / `removeImage`=명시 삭제"** — domain 타입에는 이 의미를 강제하는 코드가 없고
  실제 규칙은 직렬화(data)에서 갈린다. domain 에서 테스트하면 규칙이 없는 곳을 테스트하는 셈이다.
- **`ChallengePenalties` 의 `score`·`groupShare` 를 서버가 강제한다는 정책** — 주석으로만 존재하고 domain 에 걸린
  불변식이 없다. 테스트로 못 박으려면 먼저 규칙을 타입에 넣어야 한다(설계 변경이라 별건).
- **`ChallengeMode.isGroup` 이 화면 문구를 가르는 것**(`HomeChallengeUi`) — 위층 몫이고, 여기서는
  "그룹만 isGroup 이다"까지만 본다. 경로는 갈라지는 층에서 한 번만 검증한다.

## 남은 것

`TEST_STRATEGY.md` 가 **레포에 아직 없다**. 스킬 절차 7번은 이 문서 갱신을 요구하지만, 문서를 처음부터
세우는 건 이 요청(엔티티 테스트)보다 훨씬 넓은 작업이라 임의로 만들지 않았다.
만들 때 3절 미검증 목록에 올릴 항목은 위 "일부러 테스트하지 않은 것" 중
**`:challenge:data` 매퍼(모듈 전체 테스트 0개)** 와 **`ChallengeUpdate` 부분 수정 직렬화** 둘이다.
