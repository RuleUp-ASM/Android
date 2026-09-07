package com.ruleup.domain.entity.user

/**
 * 동의 항목. 개인정보보호법상 "누가 어떤 버전에 동의했는지"를 추적해야 해서 **항목별로 버전을 함께**
 * 기록한다.
 *
 * 서버가 두 어휘를 쓴다 — `agreements` **객체의 키**([key])와 동의 제출·조회 API 의
 * **enum 값**([apiType])이다. 하나로 합치면 어느 한쪽 호출이 조용히 400 을 받는다.
 *
 * [LOCATION_INFO]·[HEALTH_INFO] 는 **가입 때 받지 않는다** — 위치·건강 인증 수단을 처음 쓰는
 * 시점에 받는 법정 개별 동의라, 가입 요청에 실으면 서버가 모르는 항목으로 400 을 준다.
 * 그래서 가입에 실을 6종은 [SIGNUP] 이 따로 갖는다.
 *
 * @property key `agreements` 객체의 필드명 (가입 · GET /users/me).
 * @property apiType 동의 조회·제출 API 의 `type` 값. 폐기된 항목은 null 이다.
 */
enum class AgreementType(
    val key: String,
    val apiType: String?,
    val required: Boolean,
) {
    TERMS_OF_SERVICE("termsOfService", apiType = "TOS", required = true),
    PRIVACY_POLICY("privacyPolicy", apiType = "PRIVACY", required = true),
    LOCATION_SERVICE("locationService", apiType = "LOCATION", required = true),
    MARKETING("marketing", apiType = "MARKETING", required = false),
    EVENT("event", apiType = "EVENT", required = false),

    // 폐기 — 가입 요청 계약에는 남아 있어 항목 자체는 유지하되 동의 API 로는 보내지 않는다
    NIGHT_PUSH("nightPush", apiType = null, required = false),

    // 위치 기반 인증을 처음 쓸 때 받는 법정 개별 동의
    LOCATION_INFO("locationInfo", apiType = "LOCATION_INFO", required = false),

    // 건강 데이터 인증을 처음 쓸 때 받는 법정 개별 동의
    HEALTH_INFO("healthInfo", apiType = "HEALTH_INFO", required = false),
    ;

    /** 가입 요청에 실리는 항목인가. 개별 동의 2종은 가입이 아니라 인증 수단 첫 사용 시 받는다. */
    val inSignup: Boolean
        get() = this != LOCATION_INFO && this != HEALTH_INFO

    companion object {
        val REQUIRED: List<AgreementType> = entries.filter { it.required }
        val OPTIONAL: List<AgreementType> = entries.filter { !it.required }

        /** 가입 요청에 실어야 하는 6종. 하나라도 빠지면 서버가 동의 이력을 남기지 못한다. */
        val SIGNUP: List<AgreementType> = entries.filter { it.inSignup }

        fun fromApiType(value: String?): AgreementType? = entries.find { it.apiType != null && it.apiType == value }

        fun fromKey(value: String?): AgreementType? = entries.find { it.key == value }
    }
}

/** 항목별 동의 여부와 동의한 약관 버전. 버전은 `GET /intro` 의 `termsVersions` 에서 온다. */
data class AgreementConsent(
    val agreed: Boolean,
    val version: String,
)

/**
 * 가입 요청에 실리는 약관 6종 동의.
 *
 * 6종이 모두 있어야 한다 — 선택 약관도 "동의 안 함"을 버전과 함께 기록해야, 나중에 약관이 개정됐을
 * 때 재동의 판정을 할 수 있다. 개별 동의 2종은 여기 없다([AgreementType.SIGNUP] 참고).
 */
data class AgreementConsents(
    val consents: Map<AgreementType, AgreementConsent>,
) {
    /** 필수 3종에 모두 동의했는지. 하나라도 빠지면 서버가 `REQUIRED_AGREEMENT_MISSING` 을 준다. */
    val requiredSatisfied: Boolean
        get() = AgreementType.REQUIRED.all { consents[it]?.agreed == true }

    companion object {
        /**
         * 체크 상태와 현행 버전으로 6종을 만든다. [checked] 에 없는 항목은 미동의로 채운다 —
         * 선택 약관을 통째로 빠뜨리면 서버가 동의 이력을 남기지 못한다.
         */
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

/**
 * 현행 약관 버전. 가입 시 동의 기록에 그대로 실어 보낸다.
 *
 * 클라가 버전을 하드코딩하지 않게 하려고 서버가 내려준다. 값이 비어 오면 [FALLBACK_VERSION] 으로
 * 채운다 — 버전을 몰라 가입을 막는 것보다, 기록을 남기고 서버가 재검증하는 편이 낫다.
 */
data class TermsVersions(
    val versions: Map<AgreementType, String>,
) {
    fun of(type: AgreementType): String = versions[type] ?: FALLBACK_VERSION

    companion object {
        const val FALLBACK_VERSION = "1.0"
    }
}
