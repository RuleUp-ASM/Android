package com.ruleup.observability.domain.event

import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.ElementKey
import com.ruleup.observability.domain.model.Referrer
import com.ruleup.observability.domain.model.ScreenKey

sealed interface BusinessPayload : ObsPayload {
    override val channel: Channel get() = Channel.BUSINESS

    data class ScreenView(
        val screen: ScreenKey,
        val referrer: Referrer?,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : BusinessPayload

    data class UserAction(
        val screen: ScreenKey,
        val element: ElementKey,
        val elementState: Attributes,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : BusinessPayload

    /**
     * feature 고유 이벤트.
     *
     * [ScreenView] · [UserAction] 은 모든 feature 에 있는 횡단 개념이라 타입으로 두지만,
     * `challenge_created` 처럼 **한 도메인에만 있는 이벤트를 여기 케이스로 추가하면** 그 도메인
     * 지식이 관측 모듈로 새어 들어온다. feature 가 늘 때마다 이 파일을 고쳐야 하고, 애초에
     * Kotlin 의 sealed 는 모듈 경계를 넘지 못해 feature 가 자기 케이스를 정의할 수도 없다.
     *
     * 그래서 이름을 값으로 받는다. 분류 체계는 **그 도메인을 아는 모듈의 팩토리 함수**가 소유한다:
     *
     * ```kotlin
     * // :challenge:domain
     * fun challengeCreated(challengeId: String, durationDays: Int) =
     *     BusinessPayload.Custom("challenge_created", attributes {
     *         put("challenge_id", challengeId)
     *         put("duration_days", durationDays)
     *     })
     * ```
     *
     * 팩토리 시그니처가 곧 스키마다 — `durationDays: Int` 인 이상 다른 타입이 들어갈 수 없다.
     * 별도 스키마 선언을 두지 않는 이유이며, 검증은 팩토리의 출력을 그대로 고정하는 단위 테스트가
     * 더 엄격하게 해준다.
     *
     * @param name 분석 백엔드에 기록될 이벤트 이름. snake_case 를 쓴다.
     */
    data class Custom(
        val name: String,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : BusinessPayload
}
