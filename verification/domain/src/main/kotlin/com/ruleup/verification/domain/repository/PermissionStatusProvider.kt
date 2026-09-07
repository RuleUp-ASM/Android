package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.PermissionSnapshot

/**
 * 지금 이 기기의 권한 현황을 묻는 포트(driven adapter).
 *
 * 권한 상태는 **저장하지 않고 매번 OS 에 다시 묻는다**(프론트엔드 테크스펙 4-5) — 사용자가 설정에서
 * 끄고 돌아오는 경로가 있어 캐시가 곧 거짓이 된다.
 *
 * sync envelope 가 쓰는 것과 같은 스냅샷이다. 화면이 참여 전 권한을 검사할 때도 이 값을 쓴다 —
 * 검사 기준이 둘로 갈리면 "참여는 됐는데 신호는 안 올라가는" 상태가 생긴다.
 */
fun interface PermissionStatusProvider {
    suspend fun capture(): PermissionSnapshot
}
