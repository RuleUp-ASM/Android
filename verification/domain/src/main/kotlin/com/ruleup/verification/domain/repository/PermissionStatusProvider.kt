package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.PermissionSnapshot

/** 지금 이 기기의 권한 현황을 묻는 포트(driven adapter). */
fun interface PermissionStatusProvider {
    suspend fun capture(): PermissionSnapshot
}
