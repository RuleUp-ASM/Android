package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.repository.ChallengeRepository
import javax.inject.Inject

/**
 * 방장 전용 설정 조회 (명세 GET /challenges/{id}/settings).
 *
 * 수정 화면 진입 시 호출해 현재 설정·`editableFields`·`version` 을 받는다. 409 `VERSION_CONFLICT` 를
 * 받은 뒤 다시 그릴 때도 이 유스케이스로 재조회한다.
 */
class GetChallengeSettingsUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeSettings = challengeRepository.getSettings(challengeId)
    }
