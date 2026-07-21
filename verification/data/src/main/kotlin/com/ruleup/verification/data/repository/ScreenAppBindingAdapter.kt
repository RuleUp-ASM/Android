package com.ruleup.verification.data.repository

import com.ruleup.domain.challenge.ScreenAppBindingPort
import com.ruleup.entity.challenge.BoundScreenApp
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * [ScreenAppBindingPort] 구현. 대상 앱 바인딩을 verification 의 my-screen-apps 조회/교체로 위임하고,
 * 모듈 중립 타입([BoundScreenApp]) ↔ 도메인 타입([ScreenApp]) 을 매핑한다.
 */
class ScreenAppBindingAdapter
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
    ) : ScreenAppBindingPort {
        override suspend fun bound(challengeId: String): List<BoundScreenApp>? =
            verificationRepository
                .getMyScreenApps(challengeId)
                ?.apps
                ?.map { BoundScreenApp(packageName = it.packageName, appName = it.appName) }

        override suspend fun bind(
            challengeId: String,
            apps: List<BoundScreenApp>,
        ) {
            verificationRepository.updateMyScreenApps(
                challengeId = challengeId,
                apps = apps.map { ScreenApp(packageName = it.packageName, appName = it.appName) },
            )
        }
    }
