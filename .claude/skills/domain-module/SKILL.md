---
name: domain-module
description: RuleUp Android 레포에서 domain 레이어(entity·repository 계약·usecase·navigation Page·observability 이벤트)를 작성하는 절차. 새 도메인 모델이나 값 객체를 만들 때, Repository 인터페이스를 정의할 때, UseCase 를 만들지 말지 판단할 때, 화면 라우트(Page)를 추가할 때, 이벤트 카탈로그를 짤 때, 새 `:<feature>:domain` 모듈을 신설할 때 쓴다. "엔티티 만들어줘", "도메인 모델", "Repository 인터페이스", "유스케이스", "enum 추가", "라우트 추가", "화면 이동", "이벤트 심어줘", "domain 모듈" 같은 말이 나오면 코드를 쓰기 전에 반드시 이 스킬을 먼저 읽는다. 서버 enum 이 늘었을 때 어디로 떨어뜨릴지, 불변식을 어떤 타입으로 가둘지, Repository 와 Store·Provider·Register 를 어떻게 가를지 같은 이 레포 고유의 판단 기준이 여기에만 있고, 모르고 짜면 리뷰에서 통째로 되돌아온다. data·presentation 작업이어도 그 과정에서 새 entity·계약이 필요해지면 이 스킬을 읽고 domain 부터 만든다.
---

# domain 레이어 만들기

domain 은 **앱이 아는 사실과 규칙**을 담는다. 서버 응답 모양(data)도 화면 상태(presentation)도
아니고, 둘이 공유하는 어휘다. 그래서 여기 있는 타입 하나가 잘못 서면 양쪽이 같이 흔들린다.

## 진행 방식: 바로 구현한다

**확인을 기다리지 않는다.** 타입을 정하고 파일까지 쓴 뒤, 무엇을 어디에 뒀고 왜 그렇게 판단했는지를
답변에 요약해 남긴다. 사용자는 그 요약과 코드를 같이 보고 고칠 곳을 짚는다.

빈칸이 있어도 멈추지 않는다. **안전한 쪽으로 가정하고 진행하되, 가정을 KDoc 과 답변 양쪽에 남긴다.**
침묵하는 추측만 금지다 — 드러낸 가정은 사용자가 한 줄로 반박할 수 있다.

멈춰야 하는 경우는 하나뿐이다: **답이 달라지면 타입을 다시 짜야 하는 질문.** 예를 들어 어떤 상태가
서버 enum 인지 클라 파생값인지가 갈리는 경우. 이때도 "물어보고 대기"가 아니라 **한쪽을 골라 구현하고
다른 선택지를 답변에 적는다.**

### 시작 전 확인 (읽기로 끝나는 것들)

- 같은 개념의 entity 가 이미 있는지. 서버 응답이 달라도 **앱이 쓰는 사실이 같으면 타입을 새로 만들지
  않는다** — 필드를 추가하거나 nullable 로 연다.
- `core:domain` 에 이미 있는 공유 타입인지 (`Category`, `Tier`, `User`, `Gender`, `AccountStatus`).
  같은 뜻의 enum 을 feature 에 또 만들면 data 가 두 번 매핑하고 화면이 둘을 헷갈린다.
- 이 값이 서버 계약인지 확인. 서버 enum 이면 `value` 문자열이 계약이고, 클라 개념이면 문자열이 필요 없다.

### 어느 모듈의 domain 인가

| 조건 | 위치 |
|---|---|
| 둘 이상의 feature 가 쓴다 | `core:domain` |
| core 의 계약 시그니처에 등장한다 (`Page`, `NavRoute`, `TokenRepository`) | `core:domain` |
| 그 밖에 전부 | `<feature>:domain` |

소비자가 하나뿐이면 feature 로 내린다. **다른 feature 의 타입이 필요하면 그쪽 `domain` 을 직접
의존한다** — core 에 베껴 포트로 감싸지 않는다. 결합은 그대로 남는데 예외 타입·반환값 같은 표현력만
잃는다(`ScreenAppBindingPort` 가 그 사례였다). `onboarding:domain` 이 `profile:domain` 을 `api` 로
가져가는 게 정상 형태다.

### 배치

```
# 단일 개념 — flat (challenge, profile, verification)
<feature>/domain/src/main/kotlin/com/ruleup/<feature>/domain/
├── entity/
├── repository/          ← 인터페이스만. 구현은 data
├── usecase/             ← 필요할 때만 (아래 기준 참고)
├── navigation/          ← <Name>Page.kt
└── observability/       ← <Feature>Events.kt

# 개념 2개 이상 — 개념 폴더 (onboarding: auth / intro)
<feature>/domain/.../
├── auth/{entity,repository,usecase}/
├── intro/{entity,repository,usecase}/
├── navigation/          ← navigation·observability 는 개념으로 쪼개지 않는다
└── observability/
```

**기존 flat 모듈을 개념 폴더로 재편하지 않는다.** 최소 변경 원칙 — 재편이 낫겠다는 판단만 답변에 한 줄
남긴다. 임의로 옮기면 리뷰 diff 가 본질을 덮는다.

## entity

### 기본형

`data class` + **필드마다 그 값이 무엇인지·비면 무슨 뜻인지** 를 남긴다. 타입 이름과 필드명만으로는
"null 이 0인지 모름인지"가 드러나지 않고, 그걸 모르면 화면이 0%를 그린다.

```kotlin
/**
 * 방 홈 요약 (명세 `summary`).
 *
 * [roomSuccessRate] 는 판정 이력이 없으면 **null 이다 — 0 으로 접지 않는다.** 표본이 없는 것과
 * 0% 는 다른 사실이라, 0% 로 바꾸면 갓 만든 방이 실패한 방처럼 보인다.
 */
data class RoomSummary(
    val title: String,
    // 방 전체 성공률 0~1 = 성공 ÷ (성공+실패)
    val roomSuccessRate: Double?,
    val remainingDays: Int,
    val participantCount: Int,
    val capacity: Int,
)
```

- KDoc 첫 줄에 **근거**를 적는다 — 명세 경로(`명세 GET /challenges/{id}/room`)나 정책 이름.
  나중에 값이 이상할 때 어디를 열어야 하는지가 여기서 갈린다.
- 파일명은 대표 타입 이름(`Room.kt`, `Explore.kt`). 한 개념에 딸린 타입은 **한 파일에 모은다** —
  `Room.kt` 안에 `RoomUser`·`RoomSummary`·`RoomTopRanker`·`ChallengeRoom` 이 같이 산다.
- entity 는 `@Serializable` 을 달지 않는다. 직렬화는 data 의 DTO 몫이다.

### 파생값은 화면이 아니라 타입이 갖는다

화면이 `if (sort == COMPLETION_RATE || sort == SUCCESS_FAIL_RATIO)` 를 쓰기 시작하면 그 조건이
화면마다 복제되고 한쪽만 고쳐진다. **판단을 타입에 올려 두면 이름이 붙어 의도가 드러난다.**

```kotlin
/** 표본 미달 방이 목록에서 빠지는 정렬인지 — 빈 결과 문구를 가르는 기준. */
val excludesLowSample: Boolean
    get() = this == COMPLETION_RATE || this == SUCCESS_FAIL_RATIO
```

`ChallengeMode.isGroup`, `TodayVerificationStatus.isFailure`, `ExploreChallenge.hasMetrics`,
`ExploreFilter.activeCount` 가 같은 패턴이다.

### enum — 서버 값과 폴백

서버 enum 은 `value: String` 을 갖고 `fromValue` 를 companion 에 둔다. **폴백은 필드마다 판단한다:**

| 상황 | 폴백 | 예 |
|---|---|---|
| 모르는 값이 권한·자격을 부풀릴 수 있다 | 가장 **안전한** 값 | `Tier.fromValue → BRONZE` |
| 모르면 표기를 생략하면 그만 | `null` 반환 | `TodayVerificationStatus`, `ChallengeMode` |
| 모르면 그 항목이 조용히 사라진다 | `null` + 별칭 맵 | `Category.LEGACY_ALIASES` |

```kotlin
companion object {
    /** 미지 값은 최하위로 떨어뜨린다 — 서버 enum 확장이 방 입장 판정을 부풀리면 안 된다. */
    fun fromValue(value: String?): Tier = entries.find { it.value == value } ?: BRONZE
}
```

**모르는 값에 예외를 던지지 않는다.** 서버가 enum 하나 추가하면 구버전 앱이 통째로 막힌다.

`value` 가 프로퍼티명과 같아도 문자열을 명시한다 — 난독화·리네임에도 와이어 값이 남는다.
enum 상수 위 주석에는 **그 값이 무슨 상황인지**를 적는다(`// 귀속일은 끝났고 확정 전 — 유예 구간`).

### 불변식은 타입으로 가둔다

범위·형식 규칙이 있으면 `@JvmInline value class` + `private constructor` + `of()` 로 만든다.
**생성자를 막아 두면 어느 화면에서 들어오든 같은 규칙이 걸린다.**

```kotlin
@JvmInline
value class RoutineDescription private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun of(raw: String): RoutineDescription {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "루틴 설명을 입력해 주세요." }
            require(trimmed.length <= MAX_LENGTH) { "루틴 설명은 ${MAX_LENGTH}자까지예요." }
            return RoutineDescription(trimmed)
        }
    }
}
```

범위 상수는 `object` 로 모은다(`ChallengeLimits.CAPACITY_MAX`, `InterestLimits.MAX`).
**화면 위젯과 domain 검증이 같은 상수를 본다** — 숫자가 화면마다 따로 살면 한쪽만 고쳐져도 아무도 모른다.

검증 결과가 통과/불통과가 아니라 **사유별로 갈리면** `of()` 가 예외를 던지는 대신 결과 타입을
돌려준다(`Valid` / `Invalid` / `Underage` 형태의 sealed interface). 규칙이 어디에 사는지는 그대로다 —
검증기를 UseCase 로 빼지 않고 그 값 타입 곁에 둔다.

레이어 규칙: **범위 상수·불변식은 domain 타입, 입력 차단은 화면. ViewModel 에서 clamp 하지 않는다.**

### 결과가 여러 갈래면 sealed interface

성공/실패가 아니라 **성공 안에서 화면이 갈리는 경우**다. `when` 이 exhaustive 라 갈래를 추가하면
호출부가 컴파일 에러로 드러난다.

```kotlin
sealed interface LoginOutcome {
    data class GoHome(val restored: Boolean) : LoginOutcome
    data class GoHomeReadOnly(val lockInfo: LockInfo?) : LoginOutcome
    data class ResetNickname(val currentNickname: String) : LoginOutcome
    data class GoSignup(val signupToken: String, val expiresInSeconds: Int, val profile: OAuthProfile) : LoginOutcome
}
```

갈래 이름은 **화면이 할 일**로 짓는다(`GoHome`, `ResetNickname`) — 서버 상태 코드를 그대로 옮기면
화면이 다시 번역해야 한다. 여기 **없는** 갈래(403 처럼 본문이 안 오는 것)는 KDoc 에 왜 없는지 남긴다.

### 도메인 예외

화면이 분기해야 하는 실패는 domain 어휘로 둔다. presentation 이 `core:network` 의 `ApiException`
코드 문자열을 직접 보게 두면 코드가 바뀔 때마다 화면을 뒤져야 한다.

- 갈래가 적으면 예외 클래스 하나씩 — `class CursorInvalidException : Exception("목록을 다시 불러옵니다.")`
- 갈래가 많으면 enum + 그걸 실은 예외 — `AuthFailure` + `AuthException(failure, message, cause)`

번역은 **data 의 RepositoryImpl** 이 한다. domain 은 받을 타입만 정의한다.

## repository — 계약만 둔다

인터페이스는 domain, 구현은 data(또는 datastore). 시그니처에 **DTO 를 노출하지 않는다** — entity 와
원시 타입만 오간다.

```kotlin
interface ChallengeRepository {
    /**
     * 확인 화면에서 확정한 값으로 챌린지를 생성한다(명세 POST /challenges).
     *
     * [idempotencyKey] 는 확인 화면 진입 시 1회 생성해 재시도까지 계속 쓴다 — 네트워크 타임아웃 후
     * 재시도가 두 번째 방을 만들지 않게 하기 위해서다. 같은 키에 다른 본문을 보내면 서버가 409 로 막는다.
     */
    suspend fun create(
        command: CreateChallengeCommand,
        idempotencyKey: String,
    ): CreatedChallenge
}
```

함수 KDoc 에 남길 것: **명세 엔드포인트 · 실패 시 무엇이 던져지는지 · 호출부가 지켜야 할 것**
(재시도 금지, 키 재사용, 선행 조회 등). 이건 구현을 읽어선 알 수 없고, data 가 그걸 지켜 짜야 한다.

- 인자 **기본값은 인터페이스 시그니처에 둔다.** UseCase 가 같은 기본값을 다시 선언하지 않는다.
- 관찰이 필요할 때만 `Flow` 를 반환한다(`ProgressCacheStore.observe()`). 1회 조회는 `suspend`.
- 메서드가 하나면 `fun interface` (`SyncPolicyStore`).
- 파일 하나에 인터페이스 하나. 파일명 = 인터페이스명.

### 이름을 무엇으로 끝낼까

전부 `repository/` 폴더에 살지만 이름이 성격을 말한다. **`*RepositoryImpl` 이라는 이름은
아키텍처 테스트가 data/datastore 모듈로 강제**하므로, 네트워크가 아닌 것에 `Repository` 를 붙이면
구현 위치가 어색해진다.

| 접미사 | 무엇인가 | 예 |
|---|---|---|
| `Repository` | 서버 리소스 조회·변경 | `ChallengeRepository`, `AuthRepository` |
| `Store` | 로컬 보관(메모리·DataStore) | `MyChallengeStore`, `SyncPolicyStore` |
| `Provider` | 지금 값을 물어보는 읽기 전용 | `DeviceIntroProvider`, `SyncScopeProvider` |
| `Collector` | OS 신호를 긁어 버퍼에 넣는다 | `SignalCollector` |
| `Register` | OS 에 등록·해제한다 | `GeofenceRegister` |
| `Scheduler` | 나중에 실행되도록 예약한다 | `SyncScheduler` |
| `Notifier` | 앱 밖으로 알린다 | `SetupNotifier` |

뒤의 다섯은 **driven adapter 포트**다 — domain 이 Android 를 모른 채 OS 능력을 쓰기 위한 구멍이고,
KDoc 에 그 사실을 적어 둔다(`OS 신호 수집 포트(driven adapter, 명세 §1·§2)`).

## usecase — 기본은 만들지 않는다

UseCase 는 **여러 협력자를 엮는 조립**을 위한 것이다. 만드는 경우는 둘뿐이다:

| 기준 | 예 |
|---|---|
| repository·포트를 둘 이상 엮는다 | `SocialLoginUseCase` (Auth + DeviceIdentity + Token) |
| 호출에 부수효과가 따라붙는다 | `CreateChallengeUseCase` (생성 → 셋업 알림) |

만들지 않는 경우:

- **단일 repository 위임.** 인자를 그대로 넘기거나 기본값만 지정하는 경우도 포함한다.
  ViewModel 이 Repository 인터페이스를 직접 주입받는다.
- **협력자 없이 성립하는 비즈니스 규칙·검증·정규화.** 이건 **entity 소관**이다. 규칙이 값에
  붙어 있어야 그 값을 만드는 모든 경로에 규칙이 걸린다 — UseCase 로 빼면 그 UseCase 를
  거치지 않은 경로가 규칙을 통과해 버리고, 화면이 검증기를 따로 주입받아야 한다.

  나이 제한 같은 것도 `ValidateBirthDateUseCase` 가 아니라 생일 값 타입이 갖는다
  (`RoutineDescription.of()` 가 길이 규칙을 가두는 것과 같은 형태다). 현재 코드의
  `ValidateBirthDateUseCase` 는 이 기준으로 보면 entity 로 내려가야 하는 사례다.

나중에 협력자가 늘면 그때 UseCase 로 올린다. 미리 만들어 두지 않는다.

```kotlin
class CreateChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val setupNotifier: SetupNotifier,
    ) {
        suspend operator fun invoke(
            command: CreateChallengeCommand,
            idempotencyKey: String,
        ): CreatedChallenge {
            val created = challengeRepository.create(command, idempotencyKey)
            // 자동 인증인데 셋업이 미완료면 로컬 알림으로 상세 진입을 유도한다(생성의 부수효과).
            setupNotifier.notifyAfterCreate(...)
            return created
        }
    }
```

- `class` + `@Inject constructor` + `suspend operator fun invoke`. 이름은 `<동사><대상>UseCase`.
- 반환은 **화면 어휘**로 정규화한다 — 응답을 그대로 흘리지 말고 `LoginOutcome` 처럼 갈 곳을 준다.
- 순서가 규칙인 경우(세션 저장 → 분기) 왜 그 순서인지 KDoc 에 적는다. 순서만 봐선 안 드러난다.
- `*UseCase` 는 domain 에만 둘 수 있다 — Konsist 아키텍처 테스트가 막는다.

## navigation · observability

domain 에 살지만 규칙이 따로 있다. 필요할 때 읽는다:

- 화면 라우트(`navigation/<Name>Page.kt`) 추가 → **`references/navigation.md`**
- 이벤트 카탈로그(`observability/<Feature>Events.kt`) 추가 → **`references/observability.md`**
- 새 `:<feature>:domain` 모듈 신설 → **`references/new-module.md`**

## 테스트 — domain 이 테스트의 본거지다

data 는 매핑, presentation 은 UI 라 검증이 비싸다. **규칙을 domain 타입에 올려 뒀으면 여기서
싸게 검증된다.** 그래서 새 규칙을 넣었으면 테스트도 같이 쓴다.

무엇을 쓰나:

| 대상 | 무엇을 고정하나 |
|---|---|
| enum | 값 목록과 순서, 모르는 값의 폴백 (`ExploreSortTest`) |
| 파생 프로퍼티 | 경계 조건 (`excludesLowSample`, `activeCount`) |
| value class | `of()` 가 막는 입력 (`RoutineDescriptionTest`) |
| UseCase | 분기별 결과와 **호출부에 그대로 전달됐는지** (`CreateChallengeUseCaseTest`) |
| Events | 이벤트 이름·키·값 타입 골든 (`ChallengeEventsTest`) |

형태:

```kotlin
class ExploreSortTest {
    @Test
    fun `명세의 6종만 정의돼 있고 기본은 인기순이다`() {
        // 구 TEMPLATE_USAGE·TRENDING 이 남아 있으면 서버가 400 INVALID_SORT_TYPE 으로 막는다.
        assertEquals(listOf("POPULAR", "PARTICIPANTS", ...), ExploreSort.entries.map { it.value })
    }
}
```

- `kotlin.test` (`assertEquals`/`assertTrue`/`assertNull`), 코루틴은 `runBlocking`. mock 라이브러리는 쓰지 않는다.
- 테스트 이름은 **백틱 안 한국어 서술문**. 주석에는 결과가 아니라 **이게 깨지면 무엇이 망가지는지**를 적는다.
- Fake 는 `src/test/.../fake/` 에 둔다. **검증 대상 메서드만 값을 돌려주고 나머지는
  `throw NotImplementedError()`** — 의도치 않은 호출이 조용히 지나가지 않는다.
- 픽스처 팩토리(`command()`, `createdChallenge()`)를 같은 `fake/` 에 두고 기본값을 채워 둔다.
  테스트 본문에는 **그 테스트가 신경 쓰는 값만** 인자로 준다.

## 마무리

1. `./gradlew ktlintFormat` — 후행 콤마·import 정렬. 안 돌리면 CI 가 막는다.
2. 테스트 실행 — 모듈 스코프를 반드시 지정한다:
   ```bash
   ./gradlew :challenge:domain:testDebugUnitTest --tests "*ExploreTest*"
   ./gradlew :core:domain:test --tests "*CategoryTest*"   # 순수 JVM 모듈은 variant 가 없다
   ```
3. 아키텍처 테스트가 막는 것들을 자기 검열한다 (`./gradlew :app:test`):
   - `android.*` / `androidx.*` import 금지. 날짜·URI·로그가 필요하면 **포트로 뺀다**.
   - `com.ruleup.*.data.*` / `*.presentation.*` import 금지.
   - `*UseCase` 는 domain 에만, `*RepositoryImpl` 은 data/datastore 에만.
   - 쓸 수 있는 외부 의존은 `javax.inject`(어노테이션만) · `kotlinx.coroutines.core`(Flow 쓸 때) ·
     `kotlinx.serialization`(core:domain 한정) 정도다. Hilt 런타임은 넣지 않는다.

### 작업 후 답변에 남길 요약

배치는 파일 목록만 봐도 알지만, **폴백·불변식 판단은 코드를 정독해야 드러나므로** 표로 뽑아 준다 —
사용자가 반박할 지점이 바로 여기다:

```markdown
## 배치
challenge/domain/entity/Ranking.kt       (신규) — RankingEntry, RankingPage, RankingScope
challenge/domain/repository/RankingRepository.kt (신규)

## 판단
| 대상 | 결정 | 근거 |
|---|---|---|
| RankingScope 모르는 값 | null | 범위 표기를 생략하면 그만 — 잘못된 범위로 그리는 것보다 낫다 |
| myRank | Int? 유지 | 10회 미만은 등재 안 됨. 0 으로 접으면 1등처럼 보인다 |
| UseCase | 만들지 않음 | 단일 repository 위임 — ViewModel 이 직접 주입받는다 |

## 가정한 것 (틀리면 알려주세요)
- 커서 페이징으로 가정. 서버가 page/size 면 RankingPage 필드가 바뀐다
```
