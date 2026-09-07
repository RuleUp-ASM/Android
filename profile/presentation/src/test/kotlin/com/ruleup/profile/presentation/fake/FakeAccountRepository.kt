package com.ruleup.profile.presentation.fake

import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.repository.AccountRepository

/**
 * 테스트용 [AccountRepository]. 준비하지 않은 메서드는 호출되면 실패한다 —
 * 설정 화면이 의도치 않은 조회를 해도 조용히 지나가지 않게 하려는 것이다.
 */
class FakeAccountRepository(
    private val agreements: (() -> AgreementStatus)? = null,
    private val submit: ((List<AgreementSubmission>) -> AgreementStatus)? = null,
    private val sanctions: (() -> SanctionHistory)? = null,
) : AccountRepository {
    val calls = mutableListOf<String>()

    /** 무엇을 어떤 버전으로 보냈는지. 버전이 어긋나면 서버가 400 이라 인자가 계약이다. */
    val submitted = mutableListOf<List<AgreementSubmission>>()

    override suspend fun getAgreements(): AgreementStatus {
        calls += "getAgreements"
        return requireNotNull(agreements) { "getAgreements 를 준비하지 않았다" }()
    }

    override suspend fun submitAgreements(submissions: List<AgreementSubmission>): AgreementStatus {
        calls += "submitAgreements"
        submitted += submissions
        return requireNotNull(submit) { "submitAgreements 를 준비하지 않았다" }(submissions)
    }

    override suspend fun getSanctions(): SanctionHistory {
        calls += "getSanctions"
        return requireNotNull(sanctions) { "getSanctions 를 준비하지 않았다" }()
    }
}
