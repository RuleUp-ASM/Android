package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.port.VerificationRepository
import dev.zacsweers.metro.Inject

/**
 * 홈/리스트 진입 시 내 챌린지 진행률 일괄 조회(명세 3.2, VF-02).
 * 백그라운드 sync 가 이미 갱신한 값을 1회 조회한다.
 */
class ObserveProgressUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(filter: ProgressFilter = ProgressFilter.ACTIVE): ProgressSnapshot =
            verificationRepository.getProgress(filter)
    }
