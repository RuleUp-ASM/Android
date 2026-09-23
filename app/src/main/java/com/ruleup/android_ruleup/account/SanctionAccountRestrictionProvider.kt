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

/**
 * 계정 제한을 제재 조회에서 읽는다(`GET /users/me/sanctions`).
 *
 * 이 API 는 **잠금 상태에서도 열리도록** 게이트 화이트리스트에 있는 유일한 계정 API 라, 정지 여부를
 * 물어볼 수 있는 자리가 여기뿐이다. 상태와 제재 종류를 합치는 규칙은 응답이 들고 있다
 * (`SanctionHistory.restriction`) — 여기서 한 번 더 해석하면 해석이 두 벌로 갈라진다.
 *
 * 실패하면 [AccountRestriction.None] 으로 답한다 — 네트워크가 끊겼다고 정상 사용자를 잠금 화면에
 * 가두면 앱을 아예 못 쓴다. 정지 계정은 어차피 다른 API 가 전부 막혀 다음 화면에서 드러난다.
 */
@Singleton
class SanctionAccountRestrictionProvider
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) : AccountRestrictionProvider {
        override suspend fun current(): AccountRestriction =
            runCatching { accountRepository.getSanctions().restriction }
                .getOrDefault(AccountRestriction.None)
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountRestrictionModule {
    @Binds
    @Singleton
    abstract fun bindAccountRestrictionProvider(impl: SanctionAccountRestrictionProvider): AccountRestrictionProvider
}
