package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.SetupNotifier
import javax.inject.Inject

/** 챌린지 생성 유스케이스. */
class CreateChallengeUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val setupNotifier: SetupNotifier,
    ) {
        suspend operator fun invoke(
            command: CreateChallengeCommand,
            idempotencyKey: String,
        ): CreatedChallenge {
            val created = challengeRepository.create(command, idempotencyKey)
            // 자동 인증인데 셋업(권한/대상 앱)이 미완료면 로컬 알림으로 상세 진입을 유도한다(생성의 부수효과).
            setupNotifier.notifyAfterCreate(
                challengeId = created.challengeId,
                // 생성 응답은 슬림해 제목이 없다
                title = command.title,
                verification = created.verification,
                personalSetupRequired = created.personalSetupRequired,
            )
            return created
        }
    }
