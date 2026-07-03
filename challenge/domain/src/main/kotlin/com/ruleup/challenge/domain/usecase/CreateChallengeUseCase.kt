package com.ruleup.challenge.domain.usecase

import com.ruleup.analytics.domain.AnalyticsEvent
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.SetupNotifier
import javax.inject.Inject

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
        private val analyticsLogger: AnalyticsLogger,
        private val setupNotifier: SetupNotifier,
    ) {
        suspend operator fun invoke(form: ChallengeForm): Challenge {
            val challenge = challengeRepository.create(form)
            // 생성 성공(예외 미발생) 시에만 비즈니스 이벤트를 기록한다.
            analyticsLogger.log(
                AnalyticsEvent.ChallengeCreated(
                    challengeId = challenge.challengeId,
                    durationDays = challenge.durationDays,
                ),
            )
            // 자동 인증인데 셋업(권한/대상 앱)이 미완료면 로컬 알림으로 상세 진입을 유도한다(생성의 부수효과).
            setupNotifier.notifyAfterCreate(
                challengeId = challenge.challengeId,
                title = challenge.title,
                requiredPermissions = challenge.verification?.requiredPermissions.orEmpty(),
                isAuto = challenge.verification?.selectedMethod == SelectedMethod.AUTO,
            )
            return challenge
        }
    }
