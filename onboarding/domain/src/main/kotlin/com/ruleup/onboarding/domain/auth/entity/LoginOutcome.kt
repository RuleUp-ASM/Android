package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.UserAccount

/** 소셜 로그인 후 화면이 가야 할 곳. */
sealed interface LoginOutcome {
    /** 홈으로. */
    data class GoHome(
        val restored: Boolean,
    ) : LoginOutcome

    /** 제한이 걸린 계정. */
    data class Restricted(
        val lockInfo: LockInfo?,
    ) : LoginOutcome

    /** 닉네임 재설정 강제. */
    data class ResetNickname(
        val currentNickname: String,
    ) : LoginOutcome

    /** 신규 가입. */
    data class GoSignup(
        val signupToken: String,
        val expiresInSeconds: Int,
        val profile: OAuthProfile,
    ) : LoginOutcome

    companion object {
        /** 기존 회원은 로그인 경로와 무관하게 계정 상태로 행선지가 갈린다. */
        fun forExistingUser(
            nickname: String,
            account: UserAccount,
            restored: Boolean,
        ): LoginOutcome =
            when {
                account.nicknameStatus == NicknameStatus.CONFLICT -> ResetNickname(nickname)

                // 서버는 정지를 `SUSPENDED` 로 내린다.
                account.accountStatus != AccountStatus.ACTIVE -> Restricted(account.lockInfo)

                else -> GoHome(restored = restored)
            }
    }
}
