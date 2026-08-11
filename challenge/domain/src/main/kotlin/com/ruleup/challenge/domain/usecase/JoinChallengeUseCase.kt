package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 챌린지 가입 (명세 POST /challenges/{id}/members).
 *
 * **호출 전에 공개 상세의 `verification.requiredPermissions` 를 확보해야 한다** — 서버는 OS 권한을
 * 게이트로 검사하지 않고, 가입 후 권한 거부를 탈퇴로 롤백하는 경로는 폐기됐다(탈퇴 감점·재입장 1주 대기
 * 부작용). 게이트에 막히면 [com.ruleup.challenge.domain.entity.JoinBlockedException] 이 던져진다.
 */
class JoinChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): JoinResult = challengeRepository.join(challengeId)
    }
