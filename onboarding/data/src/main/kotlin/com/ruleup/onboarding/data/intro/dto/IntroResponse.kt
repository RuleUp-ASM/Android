package com.ruleup.onboarding.data.intro.dto

import com.ruleup.onboarding.domain.entity.AppVersionGate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /v1/intro 응답 data. 누락 필드는 안전한 기본값으로 떨어진다. */
@Serializable
data class IntroResponse(
    @SerialName("forceUpdate") val forceUpdate: Boolean = false,
    @SerialName("devTestMsg") val devTestMsg: String? = null,
    @SerialName("minAppVersion") val minAppVersion: String? = null,
    @SerialName("recommendAppVersion") val recommendAppVersion: String? = null,
)

fun IntroResponse.toDomain(): AppVersionGate =
    AppVersionGate(
        forceUpdate = forceUpdate,
        devTestMsg = devTestMsg,
        minAppVersion = minAppVersion,
        recommendAppVersion = recommendAppVersion,
    )
