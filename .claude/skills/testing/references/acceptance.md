# 인수 층 — 실서버 사용자 스토리

**아직 없다.** 아래는 세우는 절차이고, 세우기 전에 [BE 에 요청할 것](#be-에-요청할-것) 한 건이 선행한다.

인수 테스트는 "사용자가 하려던 일이 실제로 되는가"를 **진짜 서버에 대고** 확인한다. 아래 네 층이 전부 초록이어도 서버가 필드 이름을 바꿨거나 정책이 달라지면 앱은 망가지는데, 그걸 잡는 유일한 층이다.

대신 두 가지를 대가로 치른다: **느리고, 서버 상태를 바꾼다.** 그래서 기본 CI 에서 빼고 수동/야간으로만 돌린다.

## 목차
- [BE 에 요청할 것](#be-에-요청할-것)
- [어디에 두고 어떻게 도는가](#어디에-두고-어떻게-도는가)
- [작성 형태](#작성-형태)
- [데이터 격리와 정리](#데이터-격리와-정리)
- [무엇을 인수 시나리오로 삼는가](#무엇을-인수-시나리오로-삼는가)

---

## BE 에 요청할 것

앱의 로그인 진입점은 카카오·구글 OAuth 뿐이라 자동화된 테스트가 동의 화면을 통과할 수 없다. **테스트 전용 토큰 발급 경로가 없으면 인수 테스트는 첫 화면에서 막힌다.**

BE 에 이렇게 요청한다:

> 테스트 계정용 액세스 토큰 발급 엔드포인트. 운영 환경에서는 비활성이고, 사전 공유한 시크릿으로만 호출되며, 지정된 테스트 계정의 토큰만 내준다. 인수 테스트가 매 실행마다 이 경로로 토큰을 받아 시작한다.

이게 생기기 전까지는 **수동 획득 토큰을 환경변수로 주입**해 같은 테스트를 돌린다. 코드는 토큰을 어디서 얻었는지 모르게 짜 두면(아래 `acceptanceToken()`) 나중에 발급 경로가 생겨도 테스트는 안 고친다.

미결 상태는 `TEST_STRATEGY.md` 의 미결/차단 항목에 올려 둔다.

---

## 어디에 두고 어떻게 도는가

새 모듈을 만들지 않는다. `:app` 은 이미 전 모듈을 모으고 있어서 Retrofit api 인터페이스와 DTO 를 그대로 재사용할 수 있다 — **앱이 실제로 쓰는 계약 그대로** 서버를 두드려야 의미가 있다.

```
app/src/test/java/com/ruleup/android_ruleup/acceptance/
├── AcceptanceGate.kt          공통 게이트 · 토큰 · Retrofit 조립
└── <Story>AcceptanceTest.kt
```

기본 `./gradlew test` 에서는 **가정(assumption) 으로 건너뛴다.** 실패가 아니라 skip 이라 CI 는 초록이고, 리포트에는 "건너뜀"으로 남아 존재가 드러난다.

```kotlin
package com.ruleup.android_ruleup.acceptance

import org.junit.Assume.assumeTrue

/** 인수 테스트는 실서버 상태를 바꾸므로 명시적으로 켤 때만 돈다. */
fun requireAcceptanceEnabled() {
    assumeTrue(
        "인수 테스트는 RULEUP_ACCEPTANCE=1 일 때만 돈다",
        System.getenv("RULEUP_ACCEPTANCE") == "1",
    )
}

/** 지금은 수동 획득 토큰. BE 발급 경로가 생기면 이 함수 안만 바꾼다. */
fun acceptanceToken(): String =
    requireNotNull(System.getenv("RULEUP_ACCEPTANCE_TOKEN")) {
        "RULEUP_ACCEPTANCE_TOKEN 이 없다. 앱으로 로그인해 얻은 액세스 토큰을 넣어라."
    }

fun acceptanceBaseUrl(): String =
    System.getenv("RULEUP_ACCEPTANCE_BASE_URL") ?: BuildConfig.BASE_URL
```

돌리는 법:

```bash
RULEUP_ACCEPTANCE=1 RULEUP_ACCEPTANCE_TOKEN=... \
  ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"
```

`:app/build.gradle.kts` 에 필요한 것:

```kotlin
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.retrofit)               // 이미 core:network 가 쓰는 것과 같은 버전
testImplementation(libs.okhttp)
testImplementation(libs.kotlinx.serialization.json)
```

야간 워크플로는 이 층이 안정된 뒤에 붙인다. 먼저 수동으로 여러 번 돌려 재현되는 걸 확인하지 않으면, 새벽마다 아무도 안 보는 빨간 알림만 쌓인다.

---

## 작성 형태

스토리 하나가 테스트 하나다. 이름은 **사용자가 하려던 일**로 쓴다.

```kotlin
class ChallengeCreationAcceptanceTest {
    @Before
    fun gate() = requireAcceptanceEnabled()

    @Test
    fun `설명을 적어 챌린지를 만들면 내 챌린지 목록에서 보인다`() =
        runBlocking {
            val api = acceptanceApi<ChallengeApi>()
            val title = uniqueTitle("생성")

            val created = api.create(createRequest(title), idempotencyKey = UUID.randomUUID().toString())
            try {
                val mine = api.myChallenges()
                assertTrue(mine.items.any { it.challengeId == created.challengeId })
            } finally {
                cleanUp(api, created.challengeId)
            }
        }
}
```

- **관통해서 본다.** 한 엔드포인트의 응답 필드를 훑는 건 인수가 아니다 — 만들고, 조회하고, 보이는지까지 가야 스토리다
- **DTO 를 손으로 파싱하지 않는다.** 앱의 `*Api` 인터페이스와 DTO 를 그대로 쓴다. 서버가 필드를 바꾸면 역직렬화에서 터지는 게 목적이다
- **단언은 사용자가 아는 사실로.** 응답 스키마 전체를 고정하면 서버가 못 움직인다
- **`try/finally` 로 정리.** 아래 참고

---

## 데이터 격리와 정리

실서버를 쓰면 테스트가 남긴 쓰레기가 계정에 쌓이고, 쌓인 쓰레기가 다음 실행을 깨뜨린다. 세 가지를 지킨다.

- **매 실행 고유 이름** — `uniqueTitle()` 이 실행 시각·랜덤을 섞어 붙인다. "내가 만든 그것"을 이름으로 찾을 수 있어야 한다
- **`finally` 로 지운다** — 단언이 실패해도 정리는 돈다. 삭제 API 가 없으면 그것도 BE 에 요청할 목록이고, 그때까지는 `TEST_STRATEGY.md` 에 "이 테스트는 데이터를 남긴다"고 적어 둔다
- **다른 테스트가 만든 것에 기대지 않는다** — 실행 순서를 보장할 수 없고, 절반만 돌리는 경우가 흔하다. 각 테스트가 자기 전제를 스스로 만든다

전제가 너무 비싸서(예: 7일간 인증 기록) 매번 못 만드는 스토리는 **전용 고정 계정**을 쓰고, 그 계정이 무엇을 전제하는지 문서에 적는다.

---

## 무엇을 인수 시나리오로 삼는가

전부를 인수로 올리면 야간 실행이 한 시간짜리가 되고 아무도 안 본다. **깨졌을 때 사용자가 앱을 못 쓰는 것**만 올린다.

- 로그인 → 첫 화면 진입
- 챌린지 생성 → 내 목록에 보임
- 초대 링크로 참여 → 방 진입
- 인증 제출 → 오늘 상태가 바뀜
- 인증 실패 → 이의 제기 → 상태가 바뀜

각 시나리오는 `TEST_STRATEGY.md` 에서 **어떤 하위 테스트들이 이걸 미리 잡아주는지** 함께 적는다. 인수가 깨졌을 때 하위 층이 다 초록이었다면, 그건 하위 층에 구멍이 있다는 뜻이고 그 구멍을 메우는 게 다음 작업이다.
