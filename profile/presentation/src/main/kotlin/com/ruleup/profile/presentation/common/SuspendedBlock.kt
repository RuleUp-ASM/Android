package com.ruleup.profile.presentation.common

/**
 * 제재로 기능이 막힌 상태. 진입점을 숨기지 않고 눌렀을 때 시트로 알린다(제재 정책 §5.1).
 *
 * [until] 은 기간 제재만 있다 — 영구 정지에는 해제일이 없으므로 null 이고, 화면이 이 null 을
 * "곧 풀림"으로 접으면 안 된다.
 */
data class SuspendedBlock(
    val until: String?,
)
