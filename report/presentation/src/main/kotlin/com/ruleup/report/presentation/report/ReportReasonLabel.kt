package com.ruleup.report.presentation.report

import com.ruleup.report.domain.entity.ReportReason

/**
 * 신고 사유 표기 (Figma `1466:96`).
 *
 * 방 상세의 사유 시트(`:challenge:presentation`)에 같은 문구가 따로 있다. feature 끼리 presentation
 * 을 가로질러 의존하지 않는 규칙이라 공유하지 못하고 복제한다 — **문구를 고칠 땐 두 곳을 함께 고친다.**
 * 진입점이 더 늘면 그때 `:report:domain` 으로 올린다.
 */
fun ReportReason.label(): String =
    when (this) {
        ReportReason.CHEATING_SUSPECT -> "부정한 방법으로 인증한 것 같아요"
        ReportReason.INAPPROPRIATE -> "부적절한 내용이에요"
        ReportReason.SPAM_AD -> "광고 · 스팸이에요"
        ReportReason.ETC -> "그 외"
    }
