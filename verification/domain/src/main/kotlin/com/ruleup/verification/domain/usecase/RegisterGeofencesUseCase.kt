package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import javax.inject.Inject

/**
 * 활성 챌린지 GPS 좌표를 OS 지오펜스로 재조정 등록(명세 §2.1, Phase 1).
 * BOOT_COMPLETED·콜드스타트·참여/탈퇴 트리거에서 호출한다.
 */
class RegisterGeofencesUseCase
    @Inject
    constructor(
        private val geofenceRegistrar: GeofenceRegistrar,
    ) {
        suspend operator fun invoke(targets: List<GeofenceTarget>) = geofenceRegistrar.reconcile(targets)
    }
