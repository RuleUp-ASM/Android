package com.ruleup.verification.presentation.render

import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.OverallStatus
import com.ruleup.verification.domain.entity.TodayStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** failureReason CTA 라우팅 대상(명세 §6.4). */
enum class CtaTarget {
    NONE,

    // 신호 미수신/권한 누락 → 권한 설정으로
    OPEN_PERMISSION_SETTINGS,

    // 장소 미설정 → 지도 핀 설정으로
    OPEN_LOCATION_PIN,
}

/**
 * 오늘 상태 라벨(명세 §6.4). ⚠️ PENDING 은 "진행 중"이지 실패가 아니다 — MAX·부재·시간창 루틴은
 * 창이 닫혀야 SUCCESS 확정이라 그 전엔 계속 PENDING(절대 실패로 표시 금지).
 */
fun todayStatusLabel(status: TodayStatus): String =
    when (status) {
        TodayStatus.SUCCESS -> "인증 완료"
        TodayStatus.PENDING -> "진행 중"
        TodayStatus.FAILED -> "인증 실패"
        TodayStatus.NOT_TARGET -> "오늘은 대상일 아님"
    }

/** 오늘 상태가 실패로 렌더되어야 하는가(PENDING/NOT_TARGET 은 실패 아님). */
fun isFailureState(status: TodayStatus): Boolean = status == TodayStatus.FAILED

fun overallStatusLabel(status: OverallStatus): String =
    when (status) {
        OverallStatus.ON_TRACK -> "순항 중"
        OverallStatus.AT_RISK -> "주의"
        OverallStatus.FAILED -> "실패"
        OverallStatus.COMPLETED -> "완료"
    }

/** failureReason → 한글 카피(명세 §6.4). */
fun failureReasonCopy(reason: FailureReason): String =
    when (reason) {
        FailureReason.OUT_OF_GEOFENCE -> "등록 장소에서 인증되지 않았어요"
        FailureReason.INSUFFICIENT_DWELL -> "체류 시간이 부족해요"
        FailureReason.ENTERED_AVOID_ZONE -> "회피해야 할 장소에 머물렀어요"
        FailureReason.INSUFFICIENT_DISTANCE -> "이동 거리가 부족해요"
        FailureReason.INSUFFICIENT_STEPS -> "걸음 수가 부족해요"
        FailureReason.UNTRUSTED_HEALTH_SOURCE -> "신뢰할 수 없는 건강 데이터 출처예요"
        FailureReason.INSUFFICIENT_USAGE -> "사용 시간이 부족해요"
        FailureReason.USAGE_EXCEEDED -> "사용 시간을 초과했어요"
        FailureReason.WOKE_UP_LATE -> "기상 시각을 초과했어요"
        FailureReason.PHONE_USED_IN_BLOCK_WINDOW -> "금지 시간대에 폰을 사용했어요"
        FailureReason.SLEPT_LATE -> "취침 시각을 초과했어요"
        FailureReason.INSUFFICIENT_SLEEP -> "수면 시간이 부족해요"
        FailureReason.PERIOD_QUOTA_MISSED -> "이번 주기 목표 횟수를 못 채웠어요"
        FailureReason.NO_SIGNAL_RECEIVED -> "신호가 수신되지 않았어요 — 권한을 확인해주세요"
        FailureReason.PERMISSION_MISSING -> "권한이 없어 인증할 수 없어요"
        FailureReason.GEOFENCE_NOT_CONFIGURED -> "장소가 설정되지 않았어요"
        FailureReason.METHOD_NOT_SUPPORTED_ON_PLATFORM -> "이 기기에서는 지원되지 않는 인증 방식이에요"
        FailureReason.MANUAL_NOT_SUBMITTED -> "오늘 인증을 제출하지 않았어요"
        FailureReason.FALLBACK_LIMIT_EXCEEDED -> "이번 주 수동 인증을 모두 사용했어요"
        FailureReason.UNKNOWN -> "인증에 실패했어요"
    }

/** failureReason → CTA 라우팅 대상(명세 §6.4). */
fun failureReasonCta(reason: FailureReason): CtaTarget =
    when (reason) {
        FailureReason.NO_SIGNAL_RECEIVED, FailureReason.PERMISSION_MISSING -> CtaTarget.OPEN_PERMISSION_SETTINGS
        FailureReason.GEOFENCE_NOT_CONFIGURED -> CtaTarget.OPEN_LOCATION_PIN
        else -> CtaTarget.NONE
    }

/** CTA 버튼 라벨(없으면 null). */
fun ctaLabel(target: CtaTarget): String? =
    when (target) {
        CtaTarget.OPEN_PERMISSION_SETTINGS -> "권한 설정으로"
        CtaTarget.OPEN_LOCATION_PIN -> "지도 핀 설정으로"
        CtaTarget.NONE -> null
    }

/** 마지막 sync 가 오래(>2h)되면 "신호 미수신" 경고 배지(명세 §6.1). [lastSyncedAt] 이 null 이어도 경고. */
fun isStaleSync(
    lastSyncedEpochMillis: Long?,
    nowEpochMillis: Long,
    thresholdMillis: Long = 2L * 60 * 60 * 1000,
): Boolean = lastSyncedEpochMillis == null || (nowEpochMillis - lastSyncedEpochMillis) > thresholdMillis

/** ISO datetime → epoch millis(파싱 실패/그 null 이면 null → 신호 미수신으로 취급). */
@OptIn(ExperimentalTime::class)
fun parseIsoToEpochMillis(iso: String?): Long? = iso?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
