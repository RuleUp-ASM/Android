package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AgreementType

/**
 * 동의 항목 하나의 현재 상태 (명세: GET /users/me/agreements `agreements[]`).
 *
 * [agreed] 가 false 인데 [version] 이 있으면 **동의했다가 철회한 것**이고, 둘 다 비었으면 한 번도
 * 동의한 적이 없다. 둘을 같은 화면으로 그리면 철회한 사용자에게 "동의한 적 없음"이라고 말하게 된다.
 *
 * [required] 는 **가입 시 필수 여부**다 — 개별 동의 2종은 false 지만 해당 인증 수단을 쓰려면 필수다.
 */
data class AgreementState(
    val type: AgreementType,
    val required: Boolean,
    val agreed: Boolean,
    val version: String?,
    // ISO-8601. 동의·철회 시각
    val agreedAt: String?,
) {
    /** 동의한 적은 있는가 — "철회함"과 "받은 적 없음"을 가르는 값. */
    val everAgreed: Boolean
        get() = version != null
}

/**
 * 동의 현황 (명세: GET /users/me/agreements).
 *
 * [reconsentRequired] 는 **서버가 계산한** 재동의 대상이다 — 클라가 인트로의 `termsVersions` 와
 * 직접 비교하지 않는다. 비교 규칙이 두 곳에 살면 한쪽만 고쳐진다.
 */
data class AgreementStatus(
    val agreements: List<AgreementState>,
    val reconsentRequired: List<AgreementType>,
) {
    fun of(type: AgreementType): AgreementState? = agreements.find { it.type == type }
}

/**
 * 동의 제출·철회 한 건 (명세: POST /users/me/agreements `agreements[]`).
 *
 * [version] 은 **서버의 현재 유효 버전과 같아야 한다** — 구버전을 동의본으로 남기면 입증이 깨지므로
 * 서버가 400 `AGREEMENT_VERSION_MISMATCH` 로 막는다.
 */
data class AgreementSubmission(
    val type: AgreementType,
    val agreed: Boolean,
    val version: String,
)

/** 필수 약관 3종은 철회할 수 없다 — 철회하려면 탈퇴해야 한다(서버 400 `AGREEMENT_REVOKE_FORBIDDEN`). */
class AgreementRevokeForbiddenException : Exception("필수 약관은 철회할 수 없어요. 철회하려면 탈퇴해야 해요.")

/** 동의하려는 버전이 현행이 아니다. 화면을 다시 받아 최신 버전으로 제출해야 한다. */
class AgreementVersionMismatchException : Exception("약관이 개정됐어요. 다시 불러올게요.")
