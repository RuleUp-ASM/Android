package com.ruleup.verification.domain.entity

/** 스크린타임 측정 대상 앱 1개. */
data class ScreenApp(
    val packageName: String,
    // 바인딩 시점의 앱 이름 스냅샷(앱 삭제 후에도 표시용)
    val appName: String,
)

/** 익일 적용 대기 세트. */
data class PendingScreenApps(
    val apps: List<ScreenApp>,
    // 적용 시작 시각(익일 00:00, ISO-8601)
    val effectiveFrom: String,
)

/** 내 스크린타임 대상 앱. */
data class MyScreenApps(
    // 현재 적용 중인 앱 목록(1개 이상)
    val apps: List<ScreenApp>,
    // 현재 세트 적용 시작 시각(ISO-8601), 이력 없으면 null
    val appliedFrom: String?,
    // 익일 적용 대기 변경(없으면 null)
    val pending: PendingScreenApps?,
)

/** PUT /my-screen-apps 접수 결과. */
data class ScreenAppsUpdate(
    val apps: List<ScreenApp>,
    // 다음 변경 가능 시각(ISO-8601).
    val nextChangeAvailableAt: String? = null,
    // 적용 시작 시각(익일 00:00, ISO-8601)
    val appliedFrom: String,
)

/** 제출 단위 대상 앱 세트. */
class ScreenAppSet private constructor(
    val apps: List<ScreenApp>,
) {
    companion object {
        const val MAX_COUNT = 10
        const val MIN_COUNT = 1

        fun of(apps: List<ScreenApp>): ScreenAppSet {
            if (apps.size !in MIN_COUNT..MAX_COUNT) throw InvalidScreenAppException()
            if (apps.distinctBy { it.packageName }.size != apps.size) throw InvalidScreenAppException()
            return ScreenAppSet(apps)
        }
    }
}
