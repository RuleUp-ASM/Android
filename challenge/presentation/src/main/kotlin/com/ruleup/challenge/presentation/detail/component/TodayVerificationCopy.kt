package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.FailureReason

/**
 * 실패 사유 문구 (프론트엔드 테크스펙 4-7).
 *
 * **사용자를 거짓말하는 사람으로 대하는 문구를 쓰지 않는다.** 측정이 안 된 사실과 다음에 할 수 있는
 * 것만 말한다 — 자동 인증은 신호가 비는 것만으로도 실패하므로, 사유 문구가 곧 "왜 억울하지 않은지"를
 * 설명하는 자리다.
 */
internal fun FailureReason.failureText(): String =
    when (this) {
        FailureReason.OUT_OF_GEOFENCE -> "등록한 장소에서 머문 기록이 없어요"
        FailureReason.INSUFFICIENT_DWELL -> "머문 시간이 목표에 못 미쳤어요"
        FailureReason.ENTERED_AVOID_ZONE -> "피하기로 한 장소에 들어간 기록이 있어요"
        FailureReason.INSUFFICIENT_DISTANCE -> "이동 거리가 목표에 못 미쳤어요"
        FailureReason.INSUFFICIENT_STEPS -> "걸음 수가 목표에 못 미쳤어요"
        // 부정행위로 단정하지 않는다 — 손으로 적은 기록은 인정 대상이 아닐 뿐이다.
        FailureReason.UNTRUSTED_HEALTH_SOURCE -> "직접 입력한 기록은 인정되지 않아요"
        FailureReason.INSUFFICIENT_USAGE -> "앱 사용 시간이 목표에 못 미쳤어요"
        FailureReason.USAGE_EXCEEDED -> "앱 사용 시간이 목표를 넘었어요"
        FailureReason.WOKE_UP_LATE -> "목표 시각까지 잠금 해제 기록이 없어요"
        FailureReason.PHONE_USED_IN_BLOCK_WINDOW -> "사용하지 않기로 한 시간에 사용 기록이 있어요"
        FailureReason.SLEPT_LATE -> "목표 취침 시각을 넘겨 잠들었어요"
        FailureReason.INSUFFICIENT_SLEEP -> "수면 시간이 목표에 못 미쳤어요"
        FailureReason.PERIOD_QUOTA_MISSED -> "이번 기간의 목표 횟수를 채우지 못했어요"
        // 권한과 다른 문구를 쓴다 — 사용자가 할 수 있는 조치가 다르다(전송 재개 vs 권한 허용).
        FailureReason.NO_SIGNAL_RECEIVED -> "휴대폰에서 기록이 도착하지 않았어요"
        FailureReason.PERMISSION_MISSING -> "인증에 필요한 권한이 꺼져 있어요"
        FailureReason.GEOFENCE_NOT_CONFIGURED -> "인증 장소가 아직 등록되지 않았어요"
        FailureReason.METHOD_NOT_SUPPORTED_ON_PLATFORM -> "이 기기에서는 이 방식을 측정할 수 없어요"
        FailureReason.MANUAL_NOT_SUBMITTED -> "오늘 체크가 없었어요"
        // 폐기된 예비 폴백 잔재. 서버가 아직 보낼 수 있어 문구는 남겨 둔다.
        FailureReason.FALLBACK_LIMIT_EXCEEDED -> "이번 주 수동 인증을 모두 사용했어요"
        // 모르는 사유를 지어내지 않는다 — 이의로 갈 수 있다는 것만 말한다.
        FailureReason.UNKNOWN -> "인증 조건을 채우지 못했어요"
    }
