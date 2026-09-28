package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.VerificationConfig

/** 챌린지 생성 직후 셋업 유도 로컬 알림을 띄우는 포트. */
interface SetupNotifier {
    /** 생성 응답 기반 셋업 안내. */
    fun notifyAfterCreate(
        challengeId: String,
        title: String,
        verification: VerificationConfig,
        personalSetupRequired: Boolean,
    )
}
