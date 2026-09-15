package com.ruleup.challenge.presentation.fake

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.repository.AccountRepository

/** 개별 동의 상태만 흉내 낸다. 제출하면 그 항목이 동의 상태로 바뀐다. */
class FakeAccountRepository(
    agreed: Set<AgreementType> = emptySet(),
) : AccountRepository {
    private val agreedTypes = agreed.toMutableSet()
    val submitted = mutableListOf<AgreementSubmission>()

    override suspend fun getAgreements(): AgreementStatus =
        AgreementStatus(
            agreements =
                AgreementType.entries.map { type ->
                    val on = type in agreedTypes
                    AgreementState(type = type, required = false, agreed = on, version = "1.0".takeIf { on }, agreedAt = null)
                },
            reconsentRequired = emptyList(),
        )

    override suspend fun submitAgreements(submissions: List<AgreementSubmission>): AgreementStatus {
        submitted += submissions
        submissions.filter { it.agreed }.forEach { agreedTypes += it.type }
        return getAgreements()
    }

    override suspend fun getSanctions(): SanctionHistory = throw NotImplementedError()
}
