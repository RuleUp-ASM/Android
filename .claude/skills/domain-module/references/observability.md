# 이벤트 카탈로그(`<Feature>Events.kt`) 작성하기

이벤트는 **그 도메인을 아는 모듈이 소유한다.** `observability:domain` 에는 `challenge_created` 같은
케이스가 없고 앞으로도 없다 — sealed 는 모듈 경계를 넘지 못해 feature 가 자기 케이스를 정의할 수도
없고, feature 가 늘 때마다 관측 모듈을 고쳐야 한다. 그래서 이름을 값으로 받는
`BusinessPayload.Custom` 하나만 두고, **분류 체계는 feature domain 의 팩토리 함수가 갖는다.**

위치: `<feature>/domain/observability/<Feature>Events.kt` (개념 폴더가 있어도 여기는 쪼개지 않는다).

## 형태

`object` 안에 팩토리 함수를 모은다. **팩토리 시그니처가 곧 스키마다** — 파라미터 타입이 값 타입을
고정하므로 별도 스키마 선언을 두지 않는다.

```kotlin
object ChallengeEvents {
    // ---------- 탐색 ----------

    /** 탐색 홈 진입. 전환율의 분모다. */
    fun exploreHomeView(hasTrending: Boolean) =
        BusinessPayload.Custom(
            "explore_home_view",
            attributes { put("has_trending", hasTrending) },
        )

    /** 카드 클릭. 상세 진입률의 분자다. */
    fun challengeCardClick(
        challengeId: String,
        position: Int,
        source: ChallengeCardSource,
        sort: ExploreSort?,
    ) = BusinessPayload.Custom(
        "challenge_card_click",
        attributes {
            put("challenge_id", challengeId)
            put("position", position)
            put("source", source.value)
            // 인기 섹션은 정렬 개념이 없다 — 키를 비워두는 대신 아예 넣지 않는다.
            sort?.let { put("sort", it.value) }
        },
    )
}
```

- 이벤트 이름과 속성 키는 **snake_case**. 이름은 `<대상>_<행위>` (`challenge_card_click`).
- `attributes { put(...) }` 는 값 타입별 오버로드다(`String`/`Int`/`Long`/`Double`/`Boolean`).
  `Map<String, Any>` 와 달리 **넣는 순간 타입이 고정**되므로 대시보드에서 타입이 흔들리지 않는다.
- **KDoc 에 "이 값으로 무엇을 계산하는지"를 적는다** — "전환율의 분모다", "빈 결과율을 낸다".
  이게 없으면 나중에 아무도 그 이벤트를 지워도 되는지 판단하지 못한다.
- 구획 주석(`// ---------- 탐색 ----------`)으로 퍼널 단계를 나눈다.

### null 속성은 키를 넣지 않는다

빈 문자열을 넣으면 집계에 **가짜 분류가 하나 생긴다**. 없으면 없는 채로 둔다.

```kotlin
// 성공이면 키를 아예 넣지 않는다 — 빈 문자열은 집계에 가짜 분류를 하나 만든다.
errorCode?.let { put("error_code", it) }
```

반대로 "아무것도 안 걸렸음"이 **의미 있는 값**이면 명시적 문자열을 쓴다 —
`ExploreFilter.describe()` 는 필터가 비면 `"none"` 을 돌려준다. 빈 문자열이면 결측과 구분되지 않는다.

### 속성 값 enum 은 같은 파일 아래에

카테고리형 속성은 문자열 리터럴 대신 `value` 를 가진 enum 으로 둔다. 오타가 컴파일에 걸리고,
가능한 값의 목록이 한눈에 보인다.

```kotlin
/** 카드를 어디서 눌렀는지. 인기 섹션과 목록의 전환력을 나눠 본다. */
enum class ChallengeCardSource(
    val value: String,
) {
    TRENDING("trending"),
    LIST("list"),
}
```

여러 값을 한 속성으로 접는 헬퍼는 `private fun` 으로 파일 맨 아래 둔다(`ExploreFilter.describe()`).

## 무엇을 이벤트로 만드나

퍼널 계산이 **끊기지 않도록** 짝을 맞춘다. 분모 없는 분자는 아무것도 계산하지 못한다.

- 진입(분모) ↔ 행동(분자): `explore_home_view` → `challenge_card_click` → `challenge_detail_view`
  → `challenge_join_attempt` → `challenge_join_result`
- **식별자를 끝까지 같은 값으로 흘린다.** `challenge_id` 가 노출→클릭→상세→참여까지 이어져야
  전환율이 계산된다. 중간에 한 단계라도 빠지면 그 구간이 통째로 안 보인다.
- 실패도 남긴다. 성공만 세면 "왜 안 됐는지"를 영영 모른다(`owner_claim` 은 실패에 `error_code` 를 싣는다).
- 서버가 담당하기로 한 이벤트는 만들지 않는다 — 다만 **클라이언트에서만 만들 수 있는 연결값**
  (노출→참여의 `challenge_id` 일관성)이 걸리면 중복을 감수하고 남긴다. 그 판단 근거를 KDoc 에 적는다.

## 호출

ViewModel 이 `Observability` 를 주입받아 호출한다. domain 의 Events 는 **페이로드를 만들기만** 하고
발송하지 않는다.

```kotlin
observability.log(Channel.BUSINESS, Severity.INFO, TAG) { ChallengeEvents.exploreHomeView(hasTrending) }
```

람다인 이유가 있다 — 게이트가 **페이로드 생성 전에** 돌기 때문에 버려질 이벤트는 객체 할당조차
일어나지 않는다. 팩토리를 미리 호출해 변수에 담아 넘기지 않는다.

화면 진입·클릭처럼 모든 feature 에 공통인 것은 `Custom` 이 아니라 `BusinessPayload.ScreenView` ·
`UserAction` 을 쓴다. 진단 로그는 `observability.w(TAG) { "..." }` 계열 단축 함수.

## 골든 테스트를 같이 쓴다

이벤트 이름은 **분석 백엔드와의 계약**이다. 이름 하나가 바뀌면 대시보드가 조용히 비는데 컴파일은
그대로 통과한다. 그래서 팩토리 출력을 그대로 박아 두는 테스트를 함께 쓴다.

```kotlin
class ChallengeEventsTest {
    @Test
    fun `기능 스펙 9번의 탐색 이벤트 이름을 전부 고정한다`() {
        // 14종 중 하나라도 이름이 바뀌면 전환율 계산이 끊긴다.
        val names = listOf(
            ChallengeEvents.exploreHomeView(true),
            ChallengeEvents.challengeCardClick("c1", 0, ChallengeCardSource.LIST, ExploreSort.POPULAR),
            // ...
        ).map { it.name }

        assertEquals(listOf("explore_home_view", "challenge_card_click", ...), names)
    }
}
```

속성까지 고정하려면 `attrs` 를 `attributes { ... }` 로 만든 기대값과 비교한다 —
`Attributes` 는 삽입 순서를 보존하고 맵 동등성을 따르므로 그대로 비교된다.

## 자주 빠뜨리는 것

- **분모를 안 만든다** → 클릭 수만 쌓이고 전환율이 안 나온다.
- **`challenge_id` 를 한 단계에서 뺀다** → 퍼널이 그 지점에서 끊긴다.
- **빈 문자열을 넣는다** → 집계에 결측과 구분 안 되는 가짜 분류가 생긴다.
- **팩토리를 람다 밖에서 호출한다** → 게이트가 막을 이벤트도 객체를 만든다.
- 노출(impression) 이벤트에 중복 방지 규칙을 안 적는다 → 스크롤마다 재전송돼 노출 수가 부풀려진다.
  규칙(뷰포트 50% · 1초 이상 · 세션 내 1회)은 KDoc 에 적고 호출부가 지킨다.
