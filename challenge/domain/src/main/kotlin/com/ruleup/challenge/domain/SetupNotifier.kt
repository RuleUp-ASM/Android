package com.ruleup.challenge.domain

/**
 * 챌린지 생성 직후 셋업 유도 로컬 알림을 띄우는 포트.
 *
 * 자동 인증 챌린지가 아직 셋업(권한 허용 → 대상 앱 등록)을 마치지 않았으면, 상황에 맞는 로컬 알림을
 * 띄워 상세 화면으로 유도한다. 권한 미허용이 우선이고, 권한이 확보됐지만 대상 앱이 미등록이면 앱 등록을
 * 안내한다. 둘 다 완료됐거나 수동 인증이면 알림을 띄우지 않는다.
 *
 * 권한 여부는 OS 런타임 권한으로, 대상 앱 등록 여부는 로컬 저장([TargetAppStore])으로 판별한다.
 * 구현은 플랫폼(NotificationManager) 의존이라 app 계층이 채운다.
 */
interface SetupNotifier {
    fun notifyAfterCreate(
        challengeId: String,
        title: String,
        requiredPermissions: List<String>,
        isAuto: Boolean,
    )
}
