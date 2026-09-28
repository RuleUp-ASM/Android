package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.PendingReason

/** 판정 불가 사유 문구. */
internal fun PendingReason.pendingText(): String =
    when (this) {
        PendingReason.PERMISSION_MISSING -> "권한이 꺼져 있어 측정하지 못했어요"
        PendingReason.NO_SIGNAL -> "아직 휴대폰에서 받은 기록이 없어요"
    }
