package com.ruleup.profile.presentation.common

import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangeReason

// 점수 변동 한 줄의 표기.

/** 「아침 6:30 기상 · 사이클 성공」. */
val ScoreChange.titleLabel: String
    get() = challengeTitle?.let { "$it · ${reason.label}" } ?: reason.label

/** 사유 라벨. */
val ScoreChangeReason?.label: String
    get() =
        when (this) {
            ScoreChangeReason.CYCLE_SUCCESS -> "사이클 성공"
            ScoreChangeReason.CYCLE_FAIL -> "사이클 실패"
            ScoreChangeReason.LEAVE -> "중도 탈퇴"
            ScoreChangeReason.KICK_FAIL -> "연속 실패로 강퇴"
            ScoreChangeReason.KICK_PERMISSION -> "권한 미허용으로 강퇴"
            ScoreChangeReason.CHEAT -> "부정행위 검출"
            ScoreChangeReason.APPEAL_RESTORE -> "이의 인용으로 복원"
            // 모르는 사유는 증감폭만 남긴다
            null -> "점수 변동"
        }

/** +8 / −5. */
val ScoreChange.deltaLabel: String
    get() = if (delta < 0) "−${-delta}" else "+$delta"
