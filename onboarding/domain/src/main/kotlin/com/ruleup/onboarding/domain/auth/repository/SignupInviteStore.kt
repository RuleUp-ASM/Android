package com.ruleup.onboarding.domain.auth.repository

/** 가입 요청에 함께 보낼 초대 링크. */
interface SignupInviteStore {
    fun capture(link: String)

    suspend fun currentLink(): String?

    fun clear()
}
