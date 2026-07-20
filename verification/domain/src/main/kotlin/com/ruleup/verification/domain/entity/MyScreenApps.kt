package com.ruleup.verification.domain.entity

/** 스크린타임 측정 대상 앱 1개 (명세 my-screen-apps). */
data class ScreenApp(
    // Android 패키지명(측정 키)
    val packageName: String,
    // 바인딩 시점의 앱 이름 스냅샷(앱 삭제 후에도 표시용)
    val appName: String,
)

/** 익일 적용 대기 세트 (명세 my-screen-apps GET pending). */
data class PendingScreenApps(
    val apps: List<ScreenApp>,
    // 적용 시작 시각(익일 00:00, ISO-8601)
    val effectiveFrom: String,
)

/**
 * 내 스크린타임 대상 앱 (명세: GET /my-screen-apps). 셋업/수정 재진입 시 복원용.
 * 변경은 익일 적용이라 현재 적용 중인 세트([apps])와 익일부터 적용될 대기 세트([pending])를 함께 준다.
 */
data class MyScreenApps(
    // 현재 적용 중인 앱 목록(1개 이상)
    val apps: List<ScreenApp>,
    // 현재 세트 적용 시작 시각(ISO-8601), 이력 없으면 null
    val appliedFrom: String?,
    // 익일 적용 대기 변경(없으면 null)
    val pending: PendingScreenApps?,
)

/** PUT /my-screen-apps 접수 결과 (명세 response). */
data class ScreenAppsUpdate(
    // 접수된 앱 세트
    val apps: List<ScreenApp>,
    // 적용 시작 시각(익일 00:00, ISO-8601)
    val appliedFrom: String,
)

/** 대상 앱 세트 제약 (명세 my-screen-apps PUT: 1~10개, packageName 중복 불가). */
object ScreenAppSet {
    const val MAX_COUNT = 10
    const val MIN_COUNT = 1
}
