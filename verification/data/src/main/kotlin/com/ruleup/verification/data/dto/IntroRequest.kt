package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.DeviceIntro
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Phase 0 인트로 요청

/** 정적 디바이스 프로필. */
@Serializable
data class DeviceProfileRequest(
    @SerialName("sdkInt")
    val sdkInt: Int,
    @SerialName("model")
    val model: String,
    @SerialName("lowRam")
    val lowRam: Boolean,
)

/** 인트로 요청. */
@Serializable
data class IntroRequest(
    @SerialName("deviceProfile")
    val deviceProfile: DeviceProfileRequest,
    @SerialName("appVersion")
    val appVersion: String,
    @SerialName("permissions")
    val permissions: PermissionsRequest,
)

internal fun DeviceIntro.toRequest(): IntroRequest =
    IntroRequest(
        deviceProfile =
            DeviceProfileRequest(
                sdkInt = profile.sdkInt,
                model = profile.model,
                lowRam = profile.lowRam,
            ),
        appVersion = profile.appVersion,
        permissions = permissions.toDto(),
    )
