package com.ruleup.domain.entity.user

/**
 * 약관 6종. 개인정보보호법상 "누가 어떤 버전에 동의했는지"를 추적해야 해서 **항목별로 버전을 함께**
 * 기록한다.
 *
 * 선택 3종([MARKETING]·[EVENT]·[NIGHT_PUSH])은 가입 후 알림 설정의 초기값이 된다.
 *
 * @property key 서버 직렬화 키(`agreements` 객체의 필드명).
 */
enum class AgreementType(
    val key: String,
    val required: Boolean,
) {
    TERMS_OF_SERVICE("termsOfService", required = true),
    PRIVACY_POLICY("privacyPolicy", required = true),
    LOCATION_SERVICE("locationService", required = true),
    MARKETING("marketing", required = false),
    EVENT("event", required = false),
    NIGHT_PUSH("nightPush", required = false),
    ;

    companion object {
        val REQUIRED: List<AgreementType> = entries.filter { it.required }
        val OPTIONAL: List<AgreementType> = entries.filter { !it.required }
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
 * 때 재동의 판정을 할 수 있다.
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
                AgreementType.entries.associateWith { type ->
                    AgreementConsent(agreed = type in checked, version = versions.of(type))
                },
            )
    }
}

/**
 * 현행 약관 버전 6종. 가입 시 동의 기록에 그대로 실어 보낸다.
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
