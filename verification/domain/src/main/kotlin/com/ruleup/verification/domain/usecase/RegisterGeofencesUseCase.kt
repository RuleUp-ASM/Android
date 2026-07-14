package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.repository.GeofenceRegistrar
import javax.inject.Inject

/**
 * 로컬에 보존된 활성 챌린지 GPS 좌표를 OS 지오펜스로 재조정 등록(명세 §2.1, Phase 1).
 * BOOT_COMPLETED·콜드스타트·권한 복구 트리거에서 호출한다.
 */
class RegisterGeofencesUseCase
    @Inject
    constructor(
        private val geofenceRegistrar: GeofenceRegistrar,
    ) {
        suspend operator fun invoke() = geofenceRegistrar.reconcilePersisted()
    }
