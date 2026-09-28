package com.ruleup.verification.domain.usecase

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.repository.AccountRepository
import javax.inject.Inject

class AgreeVerificationConsentUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val introRepository: IntroRepository,
    ) {
        suspend operator fun invoke(types: List<AgreementType>) {
            require(types.all { it == AgreementType.LOCATION_INFO || it == AgreementType.HEALTH_INFO })
            if (types.isEmpty()) return
            val agreements = accountRepository.getAgreements()
            val submissions =
                types.distinct().map { type ->
                    val version = agreements.of(type)?.version ?: introRepository.lastTermsVersions().of(type)
                    AgreementSubmission(type = type, agreed = true, version = version)
                }
            accountRepository.submitAgreements(submissions)
        }
    }
