---
name: testing
description: RuleUp Android 레포에서 테스트를 설계·작성·감사하고 TEST_STRATEGY.md 를 갱신하는 절차. 케이스(엔티티·값객체·매퍼) · 모듈(UseCase·Repository 구현·ViewModel) · UI(Robolectric Compose) · 통합(모듈 경계·네비게이션·직렬화) · 인수(실서버 사용자 스토리) 다섯 층의 경계, 이름 규칙, 상태 경로와 에러 요인을 빠짐없이 열거하는 방법이 여기에만 있다. "테스트 짜줘", "테스트 케이스", "커버리지", "이거 테스트 어떻게 해", "UI 테스트", "통합 테스트", "인수 테스트", "빠진 테스트 찾아줘", "테스트 전략" 같은 말이 나오면 테스트 파일을 열기 전에 반드시 이 스킬을 먼저 읽는다. 명시적 요청이 아니어도 마찬가지다 — 새 엔티티·UseCase·ViewModel·화면을 만들거나 버그를 고치는 작업은 테스트도 같이 쓰게 되므로 코드를 쓰기 전에 읽고, PR 리뷰에서 남의 테스트를 평가할 때도 이 기준으로 판정한다. 층을 잘못 고르면 조합 폭발을 만들거나 리뷰에서 통째로 되돌아온다.
---

# 테스트

## 먼저 알아야 할 것

이 레포에는 이미 유닛 테스트가 있고 **그 컨벤션이 기준이다**. 새 프레임워크를 들여오지 않는다.

- `kotlin.test` + JUnit4. 목킹 라이브러리는 쓰지 않는다 — Fake 를 손으로 쓴다.
- 테스트 이름은 백틱으로 감싼 한국어 문장.
- `assertFailsWith`, `assertEquals`, `assertTrue` 정도만 쓴다.
- 코루틴은 `runBlocking`(domain) 또는 `runTest`(coroutines-test 가 있는 모듈).

기존 파일이 답이다. 새 테스트를 쓰기 전에 같은 층의 이웃을 하나 읽어라:
`challenge/domain/src/test/.../entity/CreateChallengeCommandTest.kt`(케이스),
`verification/domain/src/test/.../usecase/RunSyncUseCaseTest.kt`(모듈),
`challenge/domain/src/test/.../fake/FakeChallengeRepository.kt`(Fake).

## 다섯 층

| 층 | 무엇을 지키는가 | 대상 | 실행 |
|---|---|---|---|
| **케이스** | 규칙 하나 | entity·값 객체·enum·DTO 매퍼·순수 함수 | `./gradlew test` |
| **모듈** | 케이스를 엮은 한 모듈의 계약 | UseCase·RepositoryImpl·**ViewModel** | `./gradlew test` |
| **UI** | 상태가 화면에 그려지고 조작이 의도로 올라간다 (기대값의 출처는 **Figma**) | Composable (Robolectric) | `./gradlew test` |
| **통합** | 모듈 경계를 건너는 결합 | 네비게이션 레지스트리·직렬화 왕복·아키텍처 규칙 | `./gradlew test` |
| **인수** | 사용자 스토리가 실제로 된다 | 실서버 관통 | 수동/야간 전용 |

앞의 네 층은 전부 JVM 에서 돌아 CI(`test.yml`)가 그대로 커버한다. 인수만 밖에 있다 — 실서버 상태를 바꾸므로 PR 마다 돌리면 데이터가 쌓이고 CI 가 남의 네트워크 사정에 인질이 된다.

### 층은 이렇게 고른다

> **이 규칙이 깨지면 어느 파일을 고칠 것인가. 그 파일이 사는 층에 테스트를 둔다.**

주간 횟수 1~7 이 깨지면 `CreateChallengeCommand.kt` 를 고친다 → 케이스.
sync 가 보낼 게 없는데도 전송하면 `RunSyncUseCase.kt` 를 고친다 → 모듈.
PENDING 을 실패처럼 그리면 Composable 을 고친다 → UI.

이름이나 파일 위치로 고르지 마라. `ViewModel` 테스트라고 다 모듈 층이 아니고, `presentation` 안에 있다고 다 UI 층이 아니다 — 실제로 `challenge/presentation` 의 기존 테스트들은 Composable 에서 뽑아낸 순수 함수를 검증하므로 **케이스** 층이다.

## 조합 폭발을 막는 규칙

"모든 상태 경로 × 모든 에러 요인"을 층마다 되풀이하면 테스트 수가 곱으로 늘고, 그러면 아무도 리팩터링을 못 한다. 그런 테스트 뭉치는 품질을 지키는 게 아니라 코드를 얼린다.

> **경로는 그것이 갈라지는 층에서 한 번만 검증한다. 위층은 아래층을 옳게 엮었는지만 본다.**

`CreateChallengeCommand` 가 잘못된 정원을 거부하는지는 케이스 층이 끝냈다. `CreateChallengeUseCase` 테스트는 정원을 다시 훑지 않고 **"UseCase 가 command 를 만들어 repository 에 넘기고 실패를 그대로 올린다"** 만 본다. UI 테스트는 "정원 초과일 때 버튼이 잠긴다" 하나면 되고, 몇이 초과인지는 다시 세지 않는다.

이러면 전체 테스트 수가 곱이 아니라 합이 된다. 층을 건너뛴 검증이 보이면 그건 아래층에 테스트가 없다는 신호다 — 위층에서 때우지 말고 아래층에 내려라.

## 이름 규칙

테스트 이름은 **CI 가 빨개진 순간**에 읽힌다. 그걸 쓰지 않은 사람이 이름만 보고 "코드가 틀렸나, 테스트가 낡았나"를 판단해야 한다. 그래서 이름은 **조건 → 잃는 것**을 도메인 언어로 말한다.

```
✅ `주간 횟수가 1~7 을 벗어나면 만들 수 없다`
✅ `활성 챌린지도 보낼 것도 없으면 전송하지 않고 null 을 반환한다`
✅ `마감 시각을 모르면 날짜를 지어내지 않는다`

❌ `testWeeklyCount`                              — 무엇을 지키는지 없다
❌ `weeklyCount 가 0 이면 IllegalArgumentException 을 던진다`
                                                  — 예외 타입은 수단이다. 이름엔 규칙을, 단언에 수단을
❌ `create 를 호출하면 create 가 호출된다`          — 코드를 되뇔 뿐 규칙이 없다
❌ `정상 케이스`, `엣지 케이스 2`                   — 깨져도 뭘 잃었는지 모른다
❌ `명세의 2종을 모두 매핑한다`                     — 무엇의 2종인지가 없다. 형제 클래스와 그대로 부딪힌다
```

네 가지만 지키면 된다.

1. **조건과 결과가 둘 다 있다.** "…면 …한다" 형태. 조건이 없으면 언제 성립하는 규칙인지 모른다.
2. **구현 어휘가 아니라 도메인 어휘를 쓴다.** `IllegalArgumentException`·`null`·메서드 이름은 단언이 말하게 두고, 이름은 사용자·기획이 아는 말로 쓴다. 다만 `null 을 반환한다`처럼 **반환값 자체가 계약**이면 이름에 있어도 된다.
3. **부정형을 아끼지 않는다.** "지어내지 않는다", "만들 수 없다", "전송하지 않는다" — 막는 게 목적인 테스트는 막는다고 써야 의도가 산다.
4. **주어를 이름에 담는다 — 한 파일 안에서 이름이 겹치면 안 된다.** 이 레포는 관련 타입의 테스트 클래스를 한 파일에 모으는 일이 잦은데(`ChallengeModeTest`·`ChallengeVisibilityTest` 를 `ChallengeTest.kt` 하나에), 검사의 *모양*만 적으면 형제 클래스와 똑같은 이름이 나온다. 실제로 `명세의 2종을 모두 매핑한다` 가 두 클래스에 중복된 적이 있다. 리포트에는 클래스가 아니라 **이름이 뜨므로**, 겹치면 CI 가 빨개졌을 때 어느 규칙이 깨졌는지 특정할 수 없다 — 테스트 이름이 존재하는 이유가 그 순간인데 바로 그때 쓸모를 잃는다.

   고치는 법은 번호를 붙이는 게 아니라 **주어를 넣는 것**이다: `참여 형태는 SOLO·GROUP 두 종이고 서버 값과 이름이 같다` · `공개 범위는 PUBLIC·PRIVATE 두 종이고 서버 값과 이름이 같다`. 파일을 다 쓴 뒤 이름 목록을 훑어 겹치는 게 없는지 확인한다.

클래스 이름은 대상 타입 + `Test` 로 기존 관례를 따른다(`CreateChallengeCommandTest`). 파일 상단 KDoc 에는 **이 테스트가 존재하는 이유**를 한두 줄 남긴다 — `CreateChallengeCommandTest` 의 "화면도 같은 범위로 입력을 막지만 그건 UX 이고, 여기 걸리는 건 화면을 거치지 않는 경로에서 규칙이 빠졌다는 뜻이다"가 좋은 예다. 주석 기준은 `comments` 스킬을 따른다.

## 경로를 빠짐없이 열거하는 법

"모든"을 사람의 기억으로 보장하면 다음에 분기가 하나 늘 때 조용히 거짓이 된다. **열거의 출처를 코드 타입에 둬라.** 그러면 타입이 늘 때 컴파일러가 알려준다.

### 해피 경로: 타입에서 읽어낸다

대상의 시그니처를 훑어 분기를 뽑는다.

- **sealed class / interface** → 하위 타입 전부. `when` 을 `else` 없이 써서 exhaustive 하게 받으면 새 분기가 생길 때 컴파일이 깨진다.
- **enum** → `entries` 를 순회하는 테스트를 하나 두면 값이 늘어도 자동으로 걸린다. 값마다 기대치를 손으로 나열해야 하면 `entries.size` 를 단언해 "몇 개인지 안다"를 고정한다.
- **nullable 파라미터·필드** → 있음/없음 두 갈래. `null` 일 때의 기대 동작이 명세에 없으면 그건 명세 구멍이다. 물어보고 넘어가라.
- **수치 범위** → 경계 안쪽 최솟값·최댓값, 바깥쪽 바로 옆. 가운데 값은 굳이 안 본다.
- **컬렉션** → 비었을 때·하나일 때·상한일 때. 상한이 없으면 그것도 구멍이다.
- **불리언 조합** → 서로 독립이면 각각 하나씩, 상호작용이 규칙이면 그 조합만. 2ⁿ 을 다 쓰지 않는다.

### 에러 요인: 실패할 수 있는 지점에서 읽어낸다

- **`require`/`init` 블록** → 조건마다 하나. `CreateChallengeCommand` 처럼 불변식이 여럿이면 각각 별도 `assertFailsWith`.
- **`suspend` 호출** → 협력자가 던질 수 있는 예외 전부. domain 이 정의한 예외 타입(`DraftExpiredException`·`SyncTooFrequentException`·`InvalidSignalPayloadException`…)이 그 목록이다.
- **HTTP 상태** → data 층이 상태 코드를 도메인 예외로 옮긴다면 옮기는 지점에서 코드마다 하나. 409·429·401 처럼 화면 동작이 갈리는 것만.
- **응답 nullable** → 서버가 안 줄 수 있는 필드마다 "없으면 어떻게 되는가". 매퍼가 조용히 기본값을 넣고 있으면 그게 맞는 결정인지 테스트 이름으로 못 박아라.
- **시간·순서** → 만료·쿨다운·재시도가 있으면 경계 직전/직후.

### 열거를 끝냈다는 근거를 남긴다

셋 중 하나로 남긴다. 주석으로 "모든 경우를 다뤘다"고 쓰는 건 근거가 아니다.

- `when` 을 exhaustive 하게 쓴 테스트 (타입이 늘면 컴파일 실패)
- `Enum.entries` / `sealedSubclasses` 순회
- 못 다룬 게 있으면 파일 상단 KDoc 에 **왜 안 했는지**와 함께 남기고, `TEST_STRATEGY.md` 의 미검증 목록에도 올린다

## 무엇을 테스트하지 않는가

- **컴파일러가 이미 보장하는 것** — data class 의 `copy`·`equals`, 상수 값 자체.
- **다른 층이 이미 잡은 경로** — 위의 조합 폭발 규칙.
- **DI 배선 그 자체** — Hilt 가 못 엮으면 빌드가 깨진다. 다만 "어떤 구현이 바인딩됐는가"가 정책이면(예: 릴리스에서 debug sink 가 빠진다) 그건 검증할 값이 있다.
- **Composable 의 픽셀·색·간격** — 디자인 시스템 토큰을 쓴다는 건 코드가 이미 말한다. UI 테스트는 **무엇이 보이고 무엇이 눌리는가**만 본다.
- **서버 응답 스키마 전체** — 앱이 읽는 필드만. 안 쓰는 필드까지 고정하면 서버가 못 움직인다.

## 절차

새 테스트 작업이 오면 이 순서로 간다.

1. **대상과 층을 정한다.** "이 규칙이 깨지면 어느 파일을 고치나"로 층을 고른다. 여러 층에 걸치면 각 층이 무엇만 볼지 먼저 적고 시작한다.
2. **감사부터 한다.** 이미 있는 걸 또 쓰지 않도록 그 층의 이웃 테스트를 읽는다. 넓게 볼 때는 `scripts/coverage_map.py` 로 격자와 빈 구멍을 뽑는다.
3. **인프라가 없으면 먼저 세운다.** ViewModel·UI 테스트는 이 레포에 아직 실행 기반이 없다 → `references/viewmodel.md`, `references/ui.md`. 의존성은 반드시 `gradle/libs.versions.toml` 을 거친다.
4. **UI 층이면 Figma 부터 연다.** 기대 문구·상태별 노출은 구현이 아니라 디자인에서 가져온다 — 구현에서 베낀 기대값은 동어반복이라 "처음부터 틀렸다"를 영영 못 잡는다. 현행 페이지는 `1134:2` 하나뿐이고 문서에 적힌 옛 노드 ID 는 믿지 않는다 → `references/ui.md`.
5. **경로를 열거하고 이름부터 쓴다.** 본문 없이 테스트 이름 목록을 먼저 만들어 사용자에게 보여라. 이름이 규칙 목록으로 읽히지 않으면 그건 층을 잘못 골랐거나 대상이 너무 크다는 뜻이고, 그때 고치는 게 가장 싸다.
6. **작성한다.** 층별 상세는 참조 파일로.
7. **돌린다.** 모듈 스코프를 반드시 지정한다 — 안 하면 다른 모듈에서 "No tests found" 로 깨진다.
   ```bash
   ./gradlew :challenge:domain:testDebugUnitTest --tests "*CreateChallengeCommandTest*"   # android library
   ./gradlew :core:domain:test --tests "*CategoryTest*"                                    # 순수 JVM
   ./gradlew ktlintFormat && ./gradlew test                                                # 커밋 전
   ```
8. **`TEST_STRATEGY.md` 를 갱신한다.** 테스트를 늘렸는데 문서가 그대로면 문서는 품질 지표가 아니라 거짓말이 된다 → `references/strategy-doc.md`.

작업 워크플로우(이슈 → `feat/<번호>` 브랜치 → 작업 단위 PR)는 `CLAUDE.md` 를 따른다.

## 참조

필요할 때만 읽는다.

| 파일 | 언제 |
|---|---|
| `references/unit.md` | 케이스·모듈 층 — entity·매퍼·UseCase·RepositoryImpl, Fake 작성법 |
| `references/viewmodel.md` | MVI ViewModel 테스트 — 실행 기반 신설, Main 디스패처·effect 채널 함정 |
| `references/ui.md` | Robolectric Compose UI 테스트 — **Figma 기준 잡기**, 실행 기반 신설, 전역 클릭 가드·테마 함정 |
| `references/integration.md` | 모듈 경계·네비게이션·직렬화 왕복·아키텍처 규칙 |
| `references/acceptance.md` | 실서버 인수 테스트 — 테스트 토큰·격리·야간 실행 |
| `references/strategy-doc.md` | `TEST_STRATEGY.md` 형식과 갱신 절차 |
| `scripts/coverage_map.py` | 층×모듈 커버리지 격자와 빈 구멍 추출 |
