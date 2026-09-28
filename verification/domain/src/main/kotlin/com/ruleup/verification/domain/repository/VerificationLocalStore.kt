package com.ruleup.verification.domain.repository

/** 자동인증이 단말에 쌓아 둔 로컬 버퍼. */
interface VerificationLocalStore {
    /** 수집 버퍼와 수집 설정을 전부 비운다. */
    suspend fun clearAll()
}
