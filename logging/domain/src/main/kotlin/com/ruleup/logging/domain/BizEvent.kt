package com.ruleup.logging.domain

/**
 * 남길 수 있는 비즈니스 이벤트 하나.
 *
 * sealed 로 두지 않고 이름을 **값으로** 받는다 — 분류 체계는 그 도메인이 소유해야 하고
 * (`ChallengeEvents`·`OnboardingEvents`), sealed 는 모듈 경계를 넘지 못해 feature 가 자기 케이스를
 * 정의할 수도 없다. 대신 이름을 부르는 자리에서 짓지 않는다는 규칙은 팩토리가 지킨다 —
 * feature domain 의 `<Feature>Events` 만 이 타입을 만들고, 화면은 그 함수를 부른다.
 *
 * [name] 은 수집 쪽과 맞춘 값이라 한번 정하면 바꾸지 않는다 — 바꾸면 그 시점을 기준으로 지표가
 * 둘로 갈린다. 코드에서 부르는 팩토리 이름만 바꾼다.
 *
 * @param name 분석 백엔드에 기록될 이벤트 이름. snake_case 를 쓴다.
 */
data class BizEvent(
    val name: String,
    val attrs: BizAttributes = BizAttributes.EMPTY,
)

/**
 * 어느 feature 에도 속하지 않는 공통 이벤트.
 *
 * 화면 진입은 네비게이션을 쥔 `:app` 이 남기므로 여기 둔다 — feature 가 각자 남기면
 * 진입 경로(탭·딥링크·뒤로 가기)마다 빠뜨린다.
 */
object CommonBizEvents {
    /**
     * 화면에 들어왔다.
     *
     * @param from 직전 화면. 앱을 열자마자 들어온 첫 화면이면 null 이라 키를 넣지 않는다 —
     *   빈 문자열을 넣으면 집계에 "빈 값"이라는 가짜 분류가 하나 생긴다.
     */
    fun screenView(
        screen: String,
        from: String? = null,
    ) = BizEvent(
        "screen_view",
        bizAttributes {
            put("screen_name", screen)
            from?.let { put("from_screen", it) }
        },
    )
}
