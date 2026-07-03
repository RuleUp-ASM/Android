package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 셋업 요구사항 조회(명세: GET /challenges/{id}/setup). 상세 화면이 진입/재개 시 호출해
 * 지도(앵커)·대상 앱 등록 중 실제로 필요한 것만 유도하도록 판단 근거를 얻는다.
 */
class GetChallengeSetupUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeSetupInfo = challengeRepository.getSetupInfo(challengeId)
    }
