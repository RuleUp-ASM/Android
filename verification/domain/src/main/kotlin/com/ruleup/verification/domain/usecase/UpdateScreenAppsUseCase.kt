package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.InvalidScreenAppException
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * 스크린타임 대상 앱 세트 교체(명세: PUT /my-screen-apps). 항상 익일 00:00 부터 적용된다.
 * 전송 직전 packageName 중복을 제거하고 최대 [ScreenAppSet.MAX_COUNT] 개로 자른다(INVALID_APP 예방).
 * 선택이 하나도 없으면 [InvalidScreenAppException] 로 분기한다(최소 1개).
 */
class UpdateScreenAppsUseCase
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            apps: List<ScreenApp>,
        ): ScreenAppsUpdate {
            val normalized =
                apps
                    .distinctBy { it.packageName }
                    .take(ScreenAppSet.MAX_COUNT)
            if (normalized.size < ScreenAppSet.MIN_COUNT) throw InvalidScreenAppException()
            return verificationRepository.updateMyScreenApps(challengeId, normalized)
        }
    }
