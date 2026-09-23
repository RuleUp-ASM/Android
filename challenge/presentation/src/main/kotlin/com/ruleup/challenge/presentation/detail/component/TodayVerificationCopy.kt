package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.PendingReason

/** 판정 불가 사유 문구. 할 일이 갈린다 — 권한은 켜야 하고, 신호는 기다리거나 앱을 열어야 한다. */
internal fun PendingReason.pendingText(): String =
    when (this) {
        PendingReason.PERMISSION_MISSING -> "권한이 꺼져 있어 측정하지 못했어요"
        PendingReason.NO_SIGNAL -> "아직 휴대폰에서 받은 기록이 없어요"
    }
