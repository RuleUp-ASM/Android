package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.SetupAnchors
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 셋업(앵커·대상앱 바인딩) 제출(명세 setup). 지도에서 누적한 앵커를 백엔드로 보내 평가 대상 진입 여부를 받는다.
 * 전송 직전 앵커 반경을 500~5000m 로 클램프하고 최대 10개로 자른다(명세 setup, INVALID_ANCHOR 예방).
 */
class SubmitChallengeSetupUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            anchors: List<LocationPin>,
            targetPackages: List<String> = emptyList(),
        ): ChallengeSetupResult {
            val normalized =
                anchors
                    .take(SetupAnchors.MAX_COUNT)
                    .map { it.copy(radiusM = it.radiusM.coerceIn(SetupAnchors.MIN_RADIUS_M, SetupAnchors.MAX_RADIUS_M)) }
            return verificationRepository.setupChallenge(
                challengeId = challengeId,
                anchors = normalized,
                targetPackages = targetPackages,
            )
        }
    }
