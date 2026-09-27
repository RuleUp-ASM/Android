# 케이스 · 모듈 층

JVM 에서 도는 두 층. 이 레포 테스트의 대부분이고 실행 기반은 이미 있다.

## 목차
- [케이스 층](#케이스-층)
- [모듈 층](#모듈-층)
- [Fake 작성법](#fake-작성법)
- [모듈별 의존성](#모듈별-의존성)

---

## 케이스 층

규칙 하나가 값에 붙어 있는지 본다. 협력자가 없으므로 테스트도 협력자가 없어야 한다 — Fake 가 필요해졌다면 대상이 케이스 층이 아니다.

### entity · 값 객체

`init`/`require` 의 불변식마다 하나씩. `assertFailsWith` 로 "만들 수 없다"를 고정한다.

```kotlin
class CreateChallengeCommandTest {
    @Test
    fun `주간 횟수가 1~7 을 벗어나면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { command().copy(weeklyCount = 0) }
        assertFailsWith<IllegalArgumentException> { command().copy(weeklyCount = 8) }
    }
}
```

유효한 기본값은 파일 하단이나 `fake/` 의 팩토리 하나로 만들고 `copy` 로 한 축만 흔든다. 흔든 축이 곧 테스트가 말하는 규칙이라 무엇을 보는지가 코드에서 바로 읽힌다.

경계값은 **안쪽 끝과 바깥쪽 바로 옆**만. 상수가 domain 에 있으면 리터럴 대신 상수를 쓴다(`InterestLimits.MAX + 1`) — 상수가 바뀌면 테스트가 따라 움직여야 하는지 아닌지가 그 자리에서 드러난다.

### enum

값이 늘어날 때 자동으로 걸리게 만든다.

```kotlin
@Test
fun `모든 카테고리는 서버 코드를 하나씩 갖는다`() {
    assertEquals(Category.entries.size, Category.entries.map { it.code }.toSet().size)
}
```

값마다 기대치를 손으로 나열해야 한다면 개수도 함께 단언해 "지금 몇 개인지 안다"를 못 박는다.

### DTO ↔ entity 매퍼

data 층의 매핑 함수. **서버가 안 줄 수 있는 필드**가 핵심이다.

- 정상 응답이 entity 로 옳게 옮겨지는가 (필드 전부가 아니라, 옮기다 틀리기 쉬운 것 — 단위 변환·시간대·중첩 구조)
- nullable 필드가 없을 때 무엇이 되는가. 기본값을 넣고 있다면 그게 의도인지 이름으로 못 박는다
- enum 문자열이 모르는 값일 때. 던지는지 UNKNOWN 으로 접는지가 정책이다
- 직렬화 왕복이 필요하면 통합 층으로 (→ `integration.md`)

### Composable 에서 뽑아낸 순수 함수

화면 문구·포맷·표시 여부를 정하는 함수는 Composable 밖에 두면 케이스 층에서 검증된다. 기존 `AppealSheetTest`·`TodayVerificationCopyTest` 가 이 방식이다. **문구를 결정하는 로직은 UI 테스트로 올리지 말고 여기서 잡는 게 훨씬 싸다** — UI 테스트는 그 문구가 화면에 실제로 나오는지만 본다.

---

## 모듈 층

여러 협력자를 엮는 조립이 맞게 도는지 본다. 아래층이 이미 잡은 규칙은 다시 보지 않는다.

### UseCase

`RunSyncUseCaseTest` 가 기준 형태다.

```kotlin
@Test
fun `활성 챌린지도 보낼 것도 없으면 전송하지 않고 null 을 반환한다`() =
    runBlocking {
        val signalRepo = FakeSignalRepository(drain = null)
        val verificationRepo = FakeVerificationRepository()
        val useCase = RunSyncUseCase(FakeSignalCollector(), signalRepo, FakeEnvelopeMetadataProvider(), verificationRepo)

        val result = useCase(scope, collectedAt)

        assertNull(result)
        assertFalse(verificationRepo.syncCalled)   // 안 한 일도 계약이다
        assertFalse(signalRepo.markSyncedCalled)
    }
```

무엇을 보는가:

- **분기마다 하나** — 조기 반환, 정상 경로, 부분 실패
- **호출했는가/안 했는가** — 부수효과가 딸린 조립이면 "안 보냈다"가 종종 더 중요한 계약이다
- **협력자가 던진 예외를 어떻게 다루는가** — 그대로 올리는지, 도메인 예외로 바꾸는지, 삼키고 기본값을 주는지. 셋 다 정책이라 이름으로 못 박는다
- **넘긴 인자** — Fake 에 `lastCommand` 처럼 마지막 인자를 남겨두고 단언한다(`FakeChallengeRepository` 참고)

단일 repository 위임은 UseCase 가 아니다(`CLAUDE.md`). 그런 건 Repository 계약 자체를 테스트할 게 없으므로 케이스 층 매퍼 테스트나 ViewModel 테스트로 커버된다.

### RepositoryImpl

DTO ↔ entity 매핑과 예외 변환이 본체다. Retrofit api 인터페이스를 Fake 로 구현해 응답을 꽂는다.

- 성공 응답 → entity
- HTTP 오류 → 도메인 예외. 화면 동작이 갈리는 코드만(409 중복·429 쿨다운·401 만료)
- 페이지네이션·빈 목록

네트워크 스택 자체(OkHttp·Retrofit 변환)는 보지 않는다. 그건 라이브러리 몫이고, 진짜로 계약을 확인하려면 인수 층이 실서버로 본다.

---

## Fake 작성법

목킹 라이브러리를 쓰지 않는 이유는 **검증 대상이 아닌 호출이 조용히 지나가지 않게** 하기 위해서다. `FakeChallengeRepository` 의 형태를 따른다.

```kotlin
class FakeChallengeRepository(
    private val created: CreatedChallenge? = null,
) : ChallengeRepository {
    var lastCommand: CreateChallengeCommand? = null
        private set

    // 이 테스트가 안 쓰는 메서드는 호출되면 터뜨린다 — 의도치 않은 호출이 드러난다
    override suspend fun getRoutineTemplates() = throw NotImplementedError()

    override suspend fun create(command: CreateChallengeCommand, idempotencyKey: String): CreatedChallenge {
        lastCommand = command
        return requireNotNull(created)
    }
}
```

- 안 쓰는 메서드는 `throw NotImplementedError()`. 기본값을 돌려주면 잘못된 호출이 초록불로 지나간다
- 응답은 생성자로 주입, 호출 기록은 `var … private set`
- 실패를 재현할 땐 `failWith: Throwable?` 필드를 두고 던진다(`RecordingSink` 참고)
- 여러 테스트가 쓰는 Fake 는 `<module>/src/test/kotlin/.../fake/` 에. 한 테스트만 쓰면 그 파일 안에 private 으로 두는 게 낫다

**관측(Observability)은 이미 공용 fixture 가 있다.** 새로 만들지 마라.

```kotlin
testImplementation(testFixtures(project(":observability:domain")))
```
```kotlin
val sink = RecordingSink()
val observability = testObservability(sink = sink)
// …
assertEquals("challenge_create_start", sink.single.payload.name)
```

---

## 모듈별 의존성

대부분의 모듈은 이미 갖춰져 있다.

```kotlin
testImplementation(kotlin("test-junit"))                              // 기본
testImplementation(libs.kotlinx.coroutines.test)                      // runTest 가 필요하면
testImplementation(testFixtures(project(":observability:domain")))    // 이벤트를 검증하면
testImplementation(libs.kotlinx.serialization.json)                   // DTO 직렬화를 검증하면
```

`:core:domain`·`:observability:domain` 은 순수 JVM 이라 variant 가 없다 → `./gradlew :core:domain:test`.
나머지 android library 모듈은 → `./gradlew :challenge:domain:testDebugUnitTest`.
