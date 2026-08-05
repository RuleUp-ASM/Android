package com.ruleup.onboarding.domain.entity

/**
 * 로그인 시 함께 보내는 초기 권한 스냅샷. **선택 필드이고 참고용이다.**
 *
 * 서버는 이 값을 신뢰하지 않는다 — OS 설정에서 언제든 바뀌므로 저장해도 곧 사실과 달라진다.
 * 앱도 권한 상태를 저장하지 않고 필요할 때마다 OS 에 다시 묻는다.
 */
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

    /** 아직 물어본 적 없음. */
    NOT_DETERMINED("NOT_DETERMINED"),
}
