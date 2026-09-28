package com.ruleup.onboarding.domain.account

import com.ruleup.domain.entity.user.AccountRestriction

/** 지금 계정에 어떤 제한이 걸려 있는지 알려주는 포트. */
fun interface AccountRestrictionProvider {
    suspend fun current(): AccountRestriction
}
