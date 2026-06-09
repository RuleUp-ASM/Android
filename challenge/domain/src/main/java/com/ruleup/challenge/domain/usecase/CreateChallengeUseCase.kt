package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.ChallengeRepository
import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeForm
import dev.zacsweers.metro.Inject

/**
 * 챌린지 생성 유스케이스 (명세 3.2).
 *
 * 추천을 수정·확정한 최종값([ChallengeForm])으로 챌린지를 생성한다.
 * 실패는 예외([com.ruleup.network.dto.ApiException] 등)로 전파된다.
 */
class CreateChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(form: ChallengeForm): Challenge = challengeRepository.create(form)
    }
