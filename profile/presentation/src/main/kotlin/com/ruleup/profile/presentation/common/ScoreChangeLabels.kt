package com.ruleup.profile.presentation.common

import com.ruleup.profile.domain.entity.ScoreChange
import com.ruleup.profile.domain.entity.ScoreChangeReason

// 점수 변동 한 줄의 표기. 내 티어의 「최근 변동」과 히스토리의 전체 목록이 같은 규칙으로 그린다 —
// 명세가 두 API 를 같은 항목 구조로 정의했으므로, 표기가 갈리면 같은 사건이 두 화면에서 달라 보인다.

/**
 * 「아침 6:30 기상 · 사이클 성공」.
 *
 * [ScoreChange.challengeTitle] 이 없으면 **사유만** 남긴다. 완료된 방은 원본이 하드 삭제되어
 * 서버도 이름을 못 채우는 것이 정상 경로다 — 빈 자리에 「알 수 없는 챌린지」 같은 말을 지어
 * 넣으면 삭제된 방과 계정 단위 변동(가입 10점)이 같아 보인다.
 */
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
            // 모르는 사유는 증감폭만 남긴다 — 아무 라벨에나 접으면 그 행에 대해 거짓말이 된다.
            null -> "점수 변동"
        }

/** +8 / −5. 음수 기호는 하이픈이 아니라 U+2212 를 써야 숫자와 같은 높이로 붙는다. */
val ScoreChange.deltaLabel: String
    get() = if (delta < 0) "−${-delta}" else "+$delta"
