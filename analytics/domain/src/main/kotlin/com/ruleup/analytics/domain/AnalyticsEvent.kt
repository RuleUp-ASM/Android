package com.ruleup.analytics.domain

/**
 * 추적할 비즈니스 이벤트를 타입 안전하게 정의한다.
 *
 * 이벤트 이름·파라미터 키를 이 한곳에 모아 분류 체계(taxonomy)를 코드로 강제한다.
 * Firebase 제약을 지킨다: [name] 은 snake_case 40자 이하, 파라미터는 이벤트당 25개 이하,
 * 파라미터 키 40자/문자열 값 100자 이하. 개인정보(PII)는 절대 담지 않는다.
 *
 * 새 이벤트는 여기에 data class 로 추가한다. 샘플 3종은 패턴 예시이며 실제 지표에 맞게 교체·확장한다.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
) {
    /** 화면 진입. presentation 계층에서 호출. */
    data class ScreenView(
        val screen: String,
    ) : AnalyticsEvent(
            name = "screen_view",
            params = mapOf("screen_name" to screen),
        )

    /** 소셜 로그인 완료. provider 는 식별자가 아닌 분류값(google/kakao 등). */
    data class Login(
        val provider: String,
    ) : AnalyticsEvent(
            name = "login",
            params = mapOf("method" to provider),
        )

    /** 챌린지 생성 — 핵심 비즈니스 행동. UseCase 계층에서 호출. */
    data class ChallengeCreated(
        val challengeId: String,
        val durationDays: Int,
    ) : AnalyticsEvent(
            name = "challenge_created",
            params =
                mapOf(
                    "challenge_id" to challengeId,
                    "duration_days" to durationDays,
                ),
        )

    /** 자동인증 sync 성공(명세 §3). 30분 배치 전송·평가 완료. */
    data class VerificationSynced(
        val updatedCount: Int,
        val ignoredCount: Int,
    ) : AnalyticsEvent(
            name = "verification_synced",
            params =
                mapOf(
                    "updated_count" to updatedCount,
                    "ignored_count" to ignoredCount,
                ),
        )

    /** 자동인증 sync 실패(명세 §3.3). [reason] 은 식별자가 아닌 분류값(429/400/permission 등). */
    data class VerificationSyncFailed(
        val reason: String,
    ) : AnalyticsEvent(
            name = "verification_sync_failed",
            params = mapOf("reason" to reason),
        )

    /**
     * 생성 후 셋업 퍼널 단계 완료. [step] 은 `permission`/`target_apps`/`anchor` 중 하나로,
     * 어느 단계에서 이탈하는지 본다(권한 허용 → 앱 등록 → 앵커 등록).
     */
    data class SetupStepCompleted(
        val step: String,
        val challengeId: String,
    ) : AnalyticsEvent(
            name = "setup_step_completed",
            params =
                mapOf(
                    "step" to step,
                    "challenge_id" to challengeId,
                ),
        ) {
        companion object {
            const val STEP_PERMISSION = "permission"
            const val STEP_TARGET_APPS = "target_apps"
            const val STEP_ANCHOR = "anchor"
        }
    }
}
