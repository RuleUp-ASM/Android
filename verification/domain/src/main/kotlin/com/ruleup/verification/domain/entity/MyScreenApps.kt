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

/**
 * PUT /my-screen-apps 접수 결과 (명세 response).
 *
 * 저장하면 그 달의 변경 1회를 소진하므로 [nextChangeAvailableAt] 은 **항상** 내려온다(다음 달 1일
 * 00:00 KST). 화면이 "언제부터 다시 바꿀 수 있는지"를 말할 유일한 원천이다.
 */
data class ScreenAppsUpdate(
    // 접수된 앱 세트
    val apps: List<ScreenApp>,
    // 다음 변경 가능 시각(ISO-8601). 이번 저장으로 월 1회를 소진했다
    val nextChangeAvailableAt: String? = null,
    // 적용 시작 시각(익일 00:00, ISO-8601)
    val appliedFrom: String,
)

/**
 * 제출 단위 대상 앱 세트 (명세 my-screen-apps PUT: 1~10개, packageName 중복 불가).
 *
 * 제약을 타입이 보장한다 — [of] 를 거치지 않고는 만들 수 없어 어떤 경로로 와도 규칙이 빠지지 않는다.
 * 위반은 서버가 400 으로 돌려주는 것과 같은 [InvalidScreenAppException] 으로 알려, 화면이 실패 안내를
 * 한 갈래로 처리하게 한다.
 */
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
