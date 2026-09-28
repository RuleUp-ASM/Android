package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AgreementType

/** 동의 항목 하나의 현재 상태. */
data class AgreementState(
    val type: AgreementType,
    val required: Boolean,
    val agreed: Boolean,
    val version: String?,
    // ISO-8601.
    val agreedAt: String?,
) {
    /** 동의한 적은 있는가 */
    val everAgreed: Boolean
        get() = version != null
}

/** 동의 현황. */
data class AgreementStatus(
    val agreements: List<AgreementState>,
    val reconsentRequired: List<AgreementType>,
) {
    fun of(type: AgreementType): AgreementState? = agreements.find { it.type == type }
}

/** 동의 제출·철회 한 건. */
data class AgreementSubmission(
    val type: AgreementType,
    val agreed: Boolean,
    val version: String,
)

/** 필수 약관 3종은 철회할 수 없다 */
class AgreementRevokeForbiddenException : Exception("필수 약관은 철회할 수 없어요. 철회하려면 탈퇴해야 해요.")

/** 동의하려는 버전이 현행이 아니다. */
class AgreementVersionMismatchException : Exception("약관이 개정됐어요. 다시 불러올게요.")
