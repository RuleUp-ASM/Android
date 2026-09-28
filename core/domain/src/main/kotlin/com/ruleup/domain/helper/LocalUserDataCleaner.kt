package com.ruleup.domain.helper

/** 계정에 귀속된 단말 저장 데이터를 지우는 포트. */
fun interface LocalUserDataCleaner {
    /** 전부 지운다. */
    suspend fun clear()
}
