package com.ruleup.onboarding.data.intro.dto

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.intro.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.entity.IntroInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /v1/intro` 응답. 누락 필드는 안전한 기본값으로 떨어진다.
 *
 * `recommendAppVersion` 은 없다 — 업데이트 정책이 예외 없이 강제라 권장 버전 개념 자체가 빠졌다.
 */
@Serializable
data class IntroResponse(
    @SerialName("forceUpdate") val forceUpdate: Boolean = false,
    @SerialName("devTestMsg") val devTestMsg: String? = null,
    @SerialName("minAppVersion") val minAppVersion: String? = null,
    @SerialName("termsVersions") val termsVersions: TermsVersionsResponse? = null,
)

/** 현행 약관 버전 6종. 키는 [AgreementType.key] 와 같다. */
@Serializable
data class TermsVersionsResponse(
    @SerialName("termsOfService") val termsOfService: String? = null,
    @SerialName("privacyPolicy") val privacyPolicy: String? = null,
    @SerialName("locationService") val locationService: String? = null,
    @SerialName("marketing") val marketing: String? = null,
    @SerialName("event") val event: String? = null,
    @SerialName("nightPush") val nightPush: String? = null,
)

fun IntroResponse.toDomain(): IntroInfo =
    IntroInfo(
        versionGate =
            AppVersionGate(
                forceUpdate = forceUpdate,
                devTestMsg = devTestMsg,
                minAppVersion = minAppVersion,
            ),
        termsVersions = termsVersions.toDomain(),
    )

/** 비어 오는 항목은 담지 않는다 — 조회 시 [TermsVersions.FALLBACK_VERSION] 으로 떨어진다. */
internal fun TermsVersionsResponse?.toDomain(): TermsVersions =
    TermsVersions(
        buildMap {
            this@toDomain?.termsOfService?.let { put(AgreementType.TERMS_OF_SERVICE, it) }
            this@toDomain?.privacyPolicy?.let { put(AgreementType.PRIVACY_POLICY, it) }
            this@toDomain?.locationService?.let { put(AgreementType.LOCATION_SERVICE, it) }
            this@toDomain?.marketing?.let { put(AgreementType.MARKETING, it) }
            this@toDomain?.event?.let { put(AgreementType.EVENT, it) }
            this@toDomain?.nightPush?.let { put(AgreementType.NIGHT_PUSH, it) }
        },
    )
