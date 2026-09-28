package com.ruleup.onboarding.data.auth.dto

import com.ruleup.domain.device.DeviceInfo
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.onboarding.domain.auth.entity.PermissionSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginAuthRequest(
    @SerialName("code")
    val code: String? = null,
    @SerialName("codeVerifier")
    val codeVerifier: String? = null,
    // 카카오톡 간편 로그인은 SDK 가 내부 처리해 값이 없다.
    @SerialName("redirectUri")
    val redirectUri: String? = null,
    // 단일 활성 기기 판정 키.
    @SerialName("deviceId")
    val deviceId: String,
    // 동일 설치 다계정 차단 판정 키.
    @SerialName("installationId")
    val installationId: String,
    @SerialName("deviceInfo")
    val deviceInfo: DeviceInfoRequest,
    // 참고용 초기 권한 스냅샷.
    @SerialName("permissions")
    val permissions: PermissionsRequest? = null,
)

/** 로그인·가입에 동반하는 기기 정보. */
@Serializable
data class DeviceInfoRequest(
    @SerialName("platform")
    val platform: String,
    @SerialName("osVersion")
    val osVersion: String? = null,
    @SerialName("sdkInt")
    val sdkInt: Int? = null,
    @SerialName("deviceModel")
    val deviceModel: String? = null,
    @SerialName("manufacturer")
    val manufacturer: String? = null,
    @SerialName("lowRam")
    val lowRam: Boolean? = null,
    @SerialName("versionName")
    val versionName: String? = null,
    @SerialName("versionCode")
    val versionCode: Int? = null,
)

@Serializable
data class PermissionsRequest(
    @SerialName("postNotifications")
    val postNotifications: String? = null,
    @SerialName("location")
    val location: String? = null,
    @SerialName("camera")
    val camera: String? = null,
    @SerialName("screenTime")
    val screenTime: String? = null,
)

@Serializable
data class AgreementConsentRequest(
    @SerialName("agreed")
    val agreed: Boolean,
    @SerialName("version")
    val version: String,
)

@Serializable
data class SignUpRequest(
    @SerialName("inviteLink")
    val inviteLink: String? = null,
    @SerialName("signupToken")
    val signupToken: String,
    @SerialName("nickname")
    val nickname: String,
    // 0~6개.
    @SerialName("interestCategories")
    val interestCategories: List<String>,
    // YYYY-MM-DD.
    @SerialName("birthDate")
    val birthDate: String,
    // MALE / FEMALE.
    @SerialName("gender")
    val gender: String,
    // 5종 전부.
    @SerialName("agreements")
    val agreements: Map<String, AgreementConsentRequest>,
    @SerialName("deviceId")
    val deviceId: String,
    @SerialName("installationId")
    val installationId: String,
    @SerialName("deviceInfo")
    val deviceInfo: DeviceInfoRequest,
)

@Serializable
data class TokenRefreshRequest(
    @SerialName("refreshToken")
    val refreshToken: String? = null,
)

@Serializable
data class LogoutRequest(
    @SerialName("refreshToken")
    val refreshToken: String? = null,
)

internal fun DeviceInfo.toRequest(): DeviceInfoRequest =
    DeviceInfoRequest(
        platform = platform,
        osVersion = osVersion,
        sdkInt = sdkInt,
        deviceModel = deviceModel,
        manufacturer = manufacturer,
        lowRam = lowRam,
        versionName = versionName,
        versionCode = versionCode,
    )

internal fun PermissionSnapshot.toRequest(): PermissionsRequest =
    PermissionsRequest(
        postNotifications = postNotifications.value,
        location = location.value,
        camera = camera.value,
        screenTime = screenTime.value,
    )

internal fun AgreementConsents.toRequest(): Map<String, AgreementConsentRequest> =
    consents.entries.associate { (type, consent) ->
        type.key to AgreementConsentRequest(agreed = consent.agreed, version = consent.version)
    }

/** 회원 탈퇴. */
@Serializable
data class WithdrawRequest(
    @SerialName("confirmPhrase")
    val confirmPhrase: String,
)
