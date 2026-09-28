package com.ruleup.android_ruleup.account

import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.onboarding.domain.account.AccountRestrictionProvider
import com.ruleup.profile.domain.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** 계정 제한을 제재 조회에서 읽는다(`GET /users/me/sanctions`). */
@Singleton
class SanctionAccountRestrictionProvider
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) : AccountRestrictionProvider {
        override suspend fun current(): AccountRestriction = accountRepository.getSanctions().restriction
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountRestrictionModule {
    @Binds
    @Singleton
    abstract fun bindAccountRestrictionProvider(impl: SanctionAccountRestrictionProvider): AccountRestrictionProvider
}
