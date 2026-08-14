package com.ruleup.onboarding.domain.intro.repository

import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.onboarding.domain.intro.entity.IntroInfo

/**
 * 앱 진입 정보 조회(GET /v1/intro). 로그인 이전에 호출하는 공개 엔드포인트다.
 * `appVersionCode` 와 `platform` 을 헤더로 보내 서버가 강제 업데이트를 판정한다.
 */
interface IntroRepository {
    suspend fun getIntro(): IntroInfo

    /**
     * 마지막 조회에서 받은 약관 버전. 아직 못 받았거나 조회가 실패했으면 빈 버전(각 항목이 폴백으로 떨어진다).
     *
     * 가입 화면이 동의를 기록할 때 필요한데 정작 받는 건 앱 진입 시점이라, 그때 다시 부르면
     * 제출 경로에 네트워크 왕복이 하나 붙는다. 응답을 보관하는 건 저장소의 일이다.
     */
    fun lastTermsVersions(): TermsVersions
}
