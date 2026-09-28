package com.ruleup.verification.domain.entity

import com.ruleup.domain.entity.user.AgreementType

/** 서버 requiredPermissions에 대한 기기 권한과 정보 수집 동의를 각각 유지한다. */
data class VerificationAccess(
    val permissions: PermissionSnapshot,
    val missingPermissions: List<String>,
    val missingConsents: List<AgreementType>,
)
