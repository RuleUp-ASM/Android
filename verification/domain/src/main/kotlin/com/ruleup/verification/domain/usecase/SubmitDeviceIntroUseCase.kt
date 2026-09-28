package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.repository.DeviceIntroProvider
import com.ruleup.verification.domain.repository.SyncPolicyStore
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

/** Phase 0 인트로 유스케이스. */
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
