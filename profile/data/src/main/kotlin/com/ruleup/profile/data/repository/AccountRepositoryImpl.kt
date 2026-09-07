package com.ruleup.profile.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.profile.data.api.AccountApi
import com.ruleup.profile.data.dto.toAgreementFailure
import com.ruleup.profile.data.dto.toDomain
import com.ruleup.profile.data.dto.toRequest
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.repository.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl
    @Inject
    constructor(
        private val api: AccountApi,
    ) : AccountRepository {
        override suspend fun getAgreements(): AgreementStatus =
            api
                .getAgreements()
                .getOrThrow()
                .toDomain()

        override suspend fun submitAgreements(submissions: List<AgreementSubmission>): AgreementStatus =
            try {
                api
                    .submitAgreements(submissions.toRequest())
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 화면이 "탈퇴 안내"와 "다시 불러오기"로 갈리므로 코드 문자열을 밖으로 흘리지 않는다.
                throw e.toAgreementFailure()
            }

        override suspend fun getSanctions(): SanctionHistory =
            api
                .getSanctions()
                .getOrThrow()
                .toDomain()
    }
