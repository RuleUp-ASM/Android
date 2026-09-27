# NOTES

## 층 선택 — 케이스 층

`Challenge.kt` 는 협력자 없는 값 타입 묶음이다. 여기 걸리는 규칙이 깨지면 고칠 파일은 `Challenge.kt`
하나뿐이므로 케이스 층이다(Fake 도, 코루틴도 필요 없다). 이웃 `CreateChallengeCommandTest`·`ExploreTest`·
`JoinBlockReasonTest` 와 같은 자리에 `entity/ChallengeTest.kt` 로 뒀다 — `ExploreTest.kt` 가 이미
한 파일에 `ExploreSortTest`/`ExploreFilterTest` 두 클래스를 담는 선례라 그대로 따랐다.
`kotlin.test` + JUnit4, 백틱 한국어 이름, `assertEquals`/`assertNull` 만 썼다. 새 의존성·프레임워크 없음
(`challenge/domain/build.gradle.kts` 에 `testImplementation(kotlin("test-junit"))` 이 이미 있어 빌드 파일 변경 없음).

## 경로를 열거한 방법

1. `python3 .claude/skills/testing/scripts/coverage_map.py` — "테스트가 없는 대상"에
   `challenge/domain/.../entity/Challenge.kt — 불변식 있는 entity — 케이스 층` 이 그대로 잡혔다.
   (`:challenge:domain` 은 케이스 28 · 모듈 4 로, 이 파일만 구멍이었다.)
2. `Challenge.kt` 의 타입을 훑어 **규칙이 붙어 있는 것**만 남겼다.
   - enum → `entries` · `value` · `fromValue` · 파생 boolean (`isGroup`/`isPrivate`)
   - `require`/`init` → `CreateChallengeCommand` 뿐인데 **이미 `CreateChallengeCommandTest` 가 본다**
     (조합 폭발 규칙: 같은 경로를 두 번 쓰지 않는다) → 손대지 않았다.
   - 나머지는 규칙 없는 값 묶음.
3. 열거의 근거는 코드 타입에 뒀다 — 값 목록은 `entries.map { it.value }` 를 리스트와 통째로 비교하고,
   `ChallengeField` 는 13종 매핑 표 + `entries.size` 를 단언한다. 파생 boolean 도
   `entries.filter { it.isGroup }` 형태라 **enum 값이 늘면 테스트가 자동으로 빨개진다.**
4. `fromValue` 사용처를 `challenge/data/.../dto/*.kt` 에서 확인해, null 이 실제로 어떤 기본값으로
   접히는지(`?: ChallengeMode.SOLO`, `mapNotNull`)를 테스트 주석의 근거로 삼았다.

작성한 테스트 (4 클래스 · 10 케이스):
`ChallengeModeTest`(3) · `ChallengeVisibilityTest`(3) · `ChallengeStatusTest`(2) · `ChallengeFieldTest`(2)

## 일부러 테스트하지 않은 것

- **`CreateChallengeCommand` 의 불변식** — 주간 횟수·모드별 필드 조합·정원 범위는
  `CreateChallengeCommandTest` 가 이미 잡는다. 여기서 다시 훑으면 같은 경로가 두 벌이 된다.
- **data class 들** (`ChallengePeriod`·`ChallengePenalties`·`ChallengeDraft`·`ChallengeConfig`·
  `ChallengeUpdate`·`ChallengeSettings`·`CreatedChallenge`·`RoutineTemplate`·`DeleteResult`·
  `ChallengeUpdateResult`·`ChallengeModeration`) — `init` 도 파생 속성도 없다. `copy`/`equals` 는
  컴파일러가 보장한다.
- **`DraftResult` sealed interface** — 분기별 로직이 없다. "Fallback 을 에러색으로 그리지 않는다"는
  UI 층 규칙이고, 그건 화면이 생길 때 UI 층에서 본다.
- **예외 타입들** (`ChallengeNotEditableException` 외 3종) — 메시지와 필드를 담기만 한다. "429 를
  `ModerationLockedException` 으로 옮긴다" 같은 변환 규칙은 `ChallengeRepositoryImpl` 이라
  **모듈 층 · `:challenge:data`** 몫이다(현재 없음 — coverage_map 도 구멍으로 잡는다).
- **`ModerationState`** — domain 쪽엔 `value`·`fromValue` 도 파생 속성도 없어 고정할 계약이 없다.
  진짜 규칙인 `"EXEMPT", "APPROVED" -> APPROVED` 접기는
  `challenge/data/.../dto/ChallengeCommonDto.kt` 의 `moderationState()` 에 있으므로
  **`:challenge:data` 케이스 층(매퍼 테스트)** 에서 잡아야 한다. 이번 요청 범위(`challenge/domain`) 밖이라 남겨둔다.

## 남은 절차

- `TEST_STRATEGY.md` 는 레포에 아직 **없다**. 스킬 `references/strategy-doc.md` 는 처음 만들 때
  3절(미검증)·4절(인수 스토리)을 사용자와 함께 가려서 채우라고 하므로, 지어내지 않고 별도 작업으로 남긴다.
  이번 작업이 그 문서에 들어갈 항목은 위 "일부러 테스트하지 않은 것"의 마지막 두 줄(`:challenge:data`
  매퍼·RepositoryImpl 구멍)이다.
- 검증 명령(이 평가에서는 실행하지 않음):
  `./gradlew :challenge:domain:testDebugUnitTest --tests "*ChallengeModeTest*"` 등,
  커밋 전 `./gradlew ktlintFormat && ./gradlew test`.
