package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.DeviceIntro
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Phase 0 인트로 요청 (전송 스펙 §0.3) ----------

/** 정적 디바이스 프로필. appVersion 은 요청 최상위로 보낸다(§0.3 예시). */
@Serializable
data class DeviceProfileRequest(
    @SerialName("sdkInt")
    val sdkInt: Int,
    @SerialName("model")
    val model: String,
    @SerialName("lowRam")
    val lowRam: Boolean,
)

/** 인트로 요청(§0.3). deviceProfile + appVersion + 최초 권한 스냅샷. */
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
