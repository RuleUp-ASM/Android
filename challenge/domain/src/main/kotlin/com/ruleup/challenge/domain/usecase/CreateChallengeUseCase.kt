package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.SetupNotifier
import javax.inject.Inject

/**
 * 챌린지 생성 유스케이스 (명세 POST /challenges).
 *
 * 확인 화면에서 확정한 값으로 생성한다. [idempotencyKey] 는 **확인 화면 진입 시 1회 생성해** 재시도까지
 * 같은 값을 넘겨야 한다 — 여기서 만들면 재시도마다 새 키가 생겨 중복 생성 방지가 무의미해진다.
 *
 * 실패는 예외([com.ruleup.network.dto.ApiException] 등)로 전파된다.
 */
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
                // 생성 응답은 슬림해 제목이 없다 — 방금 보낸 값이 곧 최종값이라 그대로 쓴다.
                title = command.title,
                requiredPermissions = created.verification.requiredPermissions,
                isAuto = created.verification.type == VerificationType.AUTO,
            )
            return created
        }
    }
