package com.ruleup.domain.entity.user

/** 동의 항목. */
enum class AgreementType(
    val key: String,
    val apiType: String,
    val required: Boolean,
) {
    TERMS_OF_SERVICE("termsOfService", apiType = "TOS", required = true),
    PRIVACY_POLICY("privacyPolicy", apiType = "PRIVACY", required = true),
    LOCATION_SERVICE("locationService", apiType = "LOCATION", required = true),
    MARKETING("marketing", apiType = "MARKETING", required = false),
    EVENT("event", apiType = "EVENT", required = false),

    // 위치 기반 인증을 처음 쓸 때 받는 법정 개별 동의
    LOCATION_INFO("locationInfo", apiType = "LOCATION_INFO", required = false),

    // 건강 데이터 인증을 처음 쓸 때 받는 법정 개별 동의
    HEALTH_INFO("healthInfo", apiType = "HEALTH_INFO", required = false),
    ;

    /** 가입 요청에 실리는 항목인가. */
    val inSignup: Boolean
        get() = this != LOCATION_INFO && this != HEALTH_INFO

    companion object {
        val REQUIRED: List<AgreementType> = entries.filter { it.required }
        val OPTIONAL: List<AgreementType> = entries.filter { !it.required }

        /** 가입 요청에 실어야 하는 5종(필수3·선택2). */
        val SIGNUP: List<AgreementType> = entries.filter { it.inSignup }

        fun fromApiType(value: String?): AgreementType? = entries.find { it.apiType == value }

        fun fromKey(value: String?): AgreementType? = entries.find { it.key == value }
    }
}

/** 항목별 동의 여부와 동의한 약관 버전. */
data class AgreementConsent(
    val agreed: Boolean,
    val version: String,
)

/** 가입 요청에 실리는 약관 5종 동의. */
data class AgreementConsents(
    val consents: Map<AgreementType, AgreementConsent>,
) {
    /** 필수 3종에 모두 동의했는지. */
    val requiredSatisfied: Boolean
        get() = AgreementType.REQUIRED.all { consents[it]?.agreed == true }

    companion object {
        /** 체크 상태와 현행 버전으로 5종을 만든다. */
        fun of(
            checked: Set<AgreementType>,
            versions: TermsVersions,
        ): AgreementConsents =
            AgreementConsents(
                AgreementType.SIGNUP.associateWith { type ->
                    AgreementConsent(agreed = type in checked, version = versions.of(type))
                },
            )
    }
}

/** 현행 약관 버전. */
data class TermsVersions(
    val versions: Map<AgreementType, String>,
) {
    fun of(type: AgreementType): String = versions[type] ?: FALLBACK_VERSION

    companion object {
        const val FALLBACK_VERSION = "1.0"
    }
}
