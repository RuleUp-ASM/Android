package com.ruleup.profile.domain.repository

import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.SanctionHistory

/**
 * 계정 설정 계층 — 동의 현황과 제재 이력.
 *
 * 프로필 편집(`ProfileRepository`)과 갈라 두는 이유는 **잠금 상태에서의 접근 규칙이 다르기**
 * 때문이다. 제재 이력은 잠금 계정도 열어야 하고(잠금 사유를 볼 유일한 경로다), 동의 제출은
 * 잠금이면 403 이다.
 */
interface AccountRepository {
    /** 동의 현황 조회(명세: GET /users/me/agreements). 잠금 상태에서도 조회는 열려 있다. */
    suspend fun getAgreements(): AgreementStatus

    /**
     * 동의 제출·철회(명세: POST /users/me/agreements). 여러 항목을 한 트랜잭션으로 처리하므로
     * 하나라도 실패하면 전부 롤백된다.
     *
     * 필수 3종 철회는 [com.ruleup.profile.domain.entity.AgreementRevokeForbiddenException],
     * 구버전 제출은 [com.ruleup.profile.domain.entity.AgreementVersionMismatchException] 이 던져진다.
     */
    suspend fun submitAgreements(submissions: List<AgreementSubmission>): AgreementStatus

    /**
     * 제재 통지·이력 조회(명세: GET /users/me/sanctions).
     *
     * 본인 것만 조회하며 userId 를 받지 않는다 — 타인 조회 경로를 만들지 않는 것이 권한 검사보다
     * 확실한 방어다.
     */
    suspend fun getSanctions(): SanctionHistory
}
