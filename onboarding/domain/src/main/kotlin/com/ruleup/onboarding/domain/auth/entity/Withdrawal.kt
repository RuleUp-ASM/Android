package com.ruleup.onboarding.domain.auth.entity

/**
 * 탈퇴 결과 (명세: DELETE /users/me).
 *
 * 상태 기반 소프트 탈퇴다 — 1년 안에 같은 소셜 계정으로 돌아오면 기록이 복원된다.
 * [restoreNote] 는 **서버가 관리하는 문구**라 클라가 다시 쓰지 않는다.
 */
data class Withdrawal(
    val withdrawn: Boolean,
    // 복원 가능 기준 시각 (탈퇴 +1년)
    val archiveExpiresAt: String?,
    val restoreNote: String?,
) {
    companion object {
        /** 서버 검증 문자열. 화면 문구가 아니라 계약이라 여기서만 정의한다. */
        const val CONFIRM_PHRASE = "탈퇴할게요"
    }
}
