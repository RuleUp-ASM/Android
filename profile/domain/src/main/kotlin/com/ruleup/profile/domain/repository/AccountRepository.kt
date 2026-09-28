package com.ruleup.profile.domain.repository

import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.SanctionHistory

/** 계정 설정 계층 */
interface AccountRepository {
    /** 동의 현황 조회. */
    suspend fun getAgreements(): AgreementStatus

    /** 동의 제출·철회. */
    suspend fun submitAgreements(submissions: List<AgreementSubmission>): AgreementStatus

    /** 제재 통지·이력 조회. */
    suspend fun getSanctions(): SanctionHistory
}
