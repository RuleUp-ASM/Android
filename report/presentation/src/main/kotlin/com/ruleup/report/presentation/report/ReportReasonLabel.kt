package com.ruleup.report.presentation.report

import com.ruleup.report.domain.entity.ReportReason

/** 신고 사유 표기. */
fun ReportReason.label(): String =
    when (this) {
        ReportReason.CHEATING_SUSPECT -> "부정한 방법으로 인증한 것 같아요"
        ReportReason.INAPPROPRIATE -> "부적절한 내용이에요"
        ReportReason.SPAM_AD -> "광고 · 스팸이에요"
        ReportReason.ETC -> "그 외"
    }
