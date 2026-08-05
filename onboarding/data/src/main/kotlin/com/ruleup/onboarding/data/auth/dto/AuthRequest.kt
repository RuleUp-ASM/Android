package com.ruleup.onboarding.data.auth.dto

import com.ruleup.onboarding.domain.entity.AgreementConsents
import com.ruleup.onboarding.domain.entity.PermissionSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginAuthRequest(
    @SerialName("code")
    val code: String? = null,
    @SerialName("codeVerifier")
    val codeVerifier: String? = null,
    // 카카오톡 간편 로그인은 SDK 가 내부 처리해 값이 없다. 구글은 필수(콘솔 등록값과 정확히 일치).
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
    // 참고용 초기 권한 스냅샷. 서버가 신뢰하지 않으므로 없어도 된다.
    @SerialName("permissions")
    val permissions: PermissionsRequest? = null,
)

/**
 * 공통 기기 정보 객체. 로그인·가입 양쪽에 동반한다.
 *
 * [platform] 은 기본값을 두지 않는다 — `Json(encodeDefaults=false)` 에서 값이 기본값과 같으면
 * 직렬화에서 빠져 "ANDROID" 가 누락된다.
 */
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

/** 약관 항목 하나의 동의 여부와 버전. */
@Serializable
data class AgreementConsentRequest(
    @SerialName("agreed")
    val agreed: Boolean,
    @SerialName("version")
    val version: String,
)

@Serializable
data class SignUpRequest(
    @SerialName("signupToken")
    val signupToken: String,
    @SerialName("nickname")
    val nickname: String,
    // 0~6개. 건너뛰면 빈 배열.
    @SerialName("interestCategories")
    val interestCategories: List<String>,
    // YYYY-MM-DD. 만 14세 미만은 서버가 400 으로 막는다.
    @SerialName("birthDate")
    val birthDate: String,
    // MALE / FEMALE / NON_BINARY. 필수 필드라 UI 건너뛰기 시 NON_BINARY 를 보낸다.
    @SerialName("gender")
    val gender: String,
    // 6종 전부. 키는 AgreementType.key 와 같다.
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
