package com.ruleup.onboarding.data.intro.dto

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.intro.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.entity.IntroInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /v1/intro` 응답. */
@Serializable
data class IntroResponse(
    @SerialName("forceUpdate") val forceUpdate: Boolean? = null,
    @SerialName("devTestMsg") val devTestMsg: String? = null,
    @SerialName("minAppVersion") val minAppVersion: String? = null,
    @SerialName("termsVersions") val termsVersions: TermsVersionsResponse? = null,
)

/** 현행 약관 버전 5종. */
@Serializable
data class TermsVersionsResponse(
    @SerialName("termsOfService") val termsOfService: String? = null,
    @SerialName("privacyPolicy") val privacyPolicy: String? = null,
    @SerialName("locationService") val locationService: String? = null,
    @SerialName("marketing") val marketing: String? = null,
    @SerialName("event") val event: String? = null,
)

internal fun IntroResponse.toDomain(): IntroInfo =
    IntroInfo(
        versionGate =
            AppVersionGate(
                // 필드가 비어 오면 강제하지 않는다
                forceUpdate = forceUpdate ?: false,
                devTestMsg = devTestMsg,
                minAppVersion = minAppVersion,
            ),
        termsVersions = termsVersions.toDomain(),
    )

/** 비어 오는 항목은 담지 않는다 */
internal fun TermsVersionsResponse?.toDomain(): TermsVersions =
    TermsVersions(
        buildMap {
            this@toDomain?.termsOfService?.takeIf { it.isNotBlank() }?.let { put(AgreementType.TERMS_OF_SERVICE, it) }
            this@toDomain?.privacyPolicy?.takeIf { it.isNotBlank() }?.let { put(AgreementType.PRIVACY_POLICY, it) }
            this@toDomain?.locationService?.takeIf { it.isNotBlank() }?.let { put(AgreementType.LOCATION_SERVICE, it) }
            this@toDomain?.marketing?.takeIf { it.isNotBlank() }?.let { put(AgreementType.MARKETING, it) }
            this@toDomain?.event?.takeIf { it.isNotBlank() }?.let { put(AgreementType.EVENT, it) }
        },
    )
