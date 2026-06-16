package com.ruleup.challenge.presentation.create.component

import androidx.compose.runtime.Composable

/**
 * AUTO 인증에 필요한 런타임 권한을 OS 다이얼로그로 요청하는 추상화.
 *
 * 서버가 내려준 권한 토큰(예: "LOCATION", "CAMERA")을 받아 플랫폼 권한으로 매핑·요청하고,
 * 허용된 토큰 집합을 돌려준다. 토큰→플랫폼 권한 매핑은 각 플랫폼 actual 이 소유한다.
 * 매핑되지 않는(요청 불가) 토큰은 낙관적으로 허용 처리하며, 최종 판단은 서버가 한다.
 */
interface PermissionRequester {
    suspend fun request(tokens: List<String>): Set<String>
}

/** 현재 플랫폼의 [PermissionRequester] 를 생성한다. */
@Composable
expect fun rememberPermissionRequester(): PermissionRequester
