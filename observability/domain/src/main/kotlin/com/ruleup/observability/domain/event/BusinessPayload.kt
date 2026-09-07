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
     * feature 고유 이벤트. 이름을 **값으로** 받는다 — sealed 케이스로 두면 도메인 지식이 관측 모듈로
     * 새고, 애초에 sealed 는 모듈 경계를 넘지 못해 feature 가 자기 케이스를 정의할 수도 없다.
     *
     * 분류 체계와 스키마는 그 도메인의 팩토리 함수가 소유한다(예: `ChallengeEvents`).
     *
     * @param name 분석 백엔드에 기록될 이벤트 이름. snake_case 를 쓴다.
     */
    data class Custom(
        val name: String,
        override val attrs: Attributes = Attributes.EMPTY,
    ) : BusinessPayload
}
