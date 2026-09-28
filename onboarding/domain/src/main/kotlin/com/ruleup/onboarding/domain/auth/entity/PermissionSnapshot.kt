package com.ruleup.onboarding.domain.auth.entity

/** 로그인 시 함께 보내는 초기 권한 스냅샷. */
data class PermissionSnapshot(
    val postNotifications: PermissionState,
    val location: PermissionState,
    val camera: PermissionState,
    val screenTime: PermissionState,
)

enum class PermissionState(
    val value: String,
) {
    GRANTED("GRANTED"),
    DENIED("DENIED"),

    NOT_DETERMINED("NOT_DETERMINED"),
}
