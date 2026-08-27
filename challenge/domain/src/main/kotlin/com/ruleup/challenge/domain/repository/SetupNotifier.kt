package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.VerificationConfig

/**
 * 챌린지 생성 직후 셋업 유도 로컬 알림을 띄우는 포트.
 *
 * 자동 인증 챌린지가 아직 셋업을 마치지 않았으면 상황에 맞는 알림을 띄워 상세 화면으로 유도한다.
 * 순서는 **권한 → 방식별 등록**이다 — 권한이 없으면 등록 화면에 들어가도 아무것도 못 한다.
 *
 * 무엇을 등록해야 하는지는 [VerificationConfig.method] 가 결정한다. 서버의
 * [com.ruleup.challenge.domain.entity.ChallengeSetupInfo] 도 같은 기준으로
 * `requiresAnchors`(GPS_PRESENCE·GPS_AVOID) / `requiresTargetPackages`(SCREEN_TIME_MAX·MIN) 를 계산하므로, 방식만 알면
 * 추가 조회 없이 같은 결론에 도달한다.
 *
 * 구현(challenge:data)은 "무슨 알림"만 결정하고, 실제 발송·딥링크는 PushNotificationHelper 에 위임한다.
 */
interface SetupNotifier {
    /**
     * @param verification 생성 응답의 인증 스냅샷. 방식에 따라 필요한 등록이 갈린다.
     * @param personalSetupRequired 서버가 개인 인증 설정이 필요하다고 본 경우에만 true.
     *   false 면 알림을 띄우지 않는다 — 서버 판단이 클라 추론보다 우선한다.
     */
    fun notifyAfterCreate(
        challengeId: String,
        title: String,
        verification: VerificationConfig,
        personalSetupRequired: Boolean,
    )
}
