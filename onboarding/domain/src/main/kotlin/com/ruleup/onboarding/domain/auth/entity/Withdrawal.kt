package com.ruleup.onboarding.domain.auth.entity

/** 탈퇴 결과. */
data class Withdrawal(
    val withdrawn: Boolean,
    // 복원 가능 기준 시각 (탈퇴 +1년)
    val archiveExpiresAt: String?,
    val restoreNote: String?,
) {
    companion object {
        /** 서버 검증 문자열. */
        const val CONFIRM_PHRASE = "탈퇴할게요"
    }
}
