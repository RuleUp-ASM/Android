package com.ruleup.verification.domain.repository

/**
 * 자동인증이 단말에 쌓아 둔 로컬 버퍼.
 *
 * 수집한 신호는 전송될 때까지 기기에 남는다 — 지오펜스 목표·통과 기록, 위치 표본, 앱 사용 이벤트와
 * 커서, 건강 측정값, 진행률 캐시. **전부 계정에 귀속된 데이터라 계정이 바뀌면 남아 있으면 안 된다.**
 */
interface VerificationLocalStore {
    /**
     * 수집 버퍼와 수집 설정을 전부 비운다. 로그아웃·탈퇴에서 부른다.
     *
     * 지오펜스 OS 등록 해제는 여기 포함되지 않는다 — [GeofenceRegister.clear] 가 따로 한다.
     */
    suspend fun clearAll()
}
