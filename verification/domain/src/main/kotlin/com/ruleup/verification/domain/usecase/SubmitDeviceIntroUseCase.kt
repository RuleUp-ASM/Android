package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.repository.DeviceIntroProvider
import com.ruleup.verification.domain.repository.SyncPolicyStore
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Phase 0 인트로 유스케이스(전송 스펙 §0.3). 로그인 직후 1회: 정적 프로필 + 최초 권한 스냅샷을 보내고
 * 서버 정책을 받아 보관 → 주기 flush 간격에 즉시 적용한다.
 *
 * 네트워크 실패는 호출자가 무시(다음 앱 시작/주기 sync 응답이 정책을 보정). 멱등하다.
 */
class SubmitDeviceIntroUseCase
    @Inject
    constructor(
        private val deviceIntroProvider: DeviceIntroProvider,
        private val verificationRepository: VerificationRepository,
        private val syncPolicyStore: SyncPolicyStore,
        private val syncScheduler: SyncScheduler,
    ) {
        suspend operator fun invoke() {
            val intro = deviceIntroProvider.capture()
            val policy = verificationRepository.submitIntro(intro)
            syncPolicyStore.save(policy)
            syncScheduler.reschedule(policy.flushIntervalSec)
        }
    }
