package com.ruleup.domain.challenge

import com.ruleup.domain.entity.challenge.BoundScreenApp

/**
 * 스크린타임 대상 앱 바인딩 포트(feature 간 공유용 core 경계).
 *
 * 대상 앱 선택 화면(challenge)이 서버 my-screen-apps(verification)를 직접 호출할 수 없으므로,
 * verification:data 가 본 포트를 구현하고 challenge:presentation 이 이를 소비한다.
 */
interface ScreenAppBindingPort {
    /**
     * 현재 바인딩된 대상 앱 목록(셋업/수정 재진입 복원용). 미설정이면 null.
     * 실패(권한/네트워크 등)는 예외로 전파된다.
     */
    suspend fun bound(challengeId: String): List<BoundScreenApp>?

    /**
     * 대상 앱 세트를 교체한다(익일 적용). 쿨다운·형식 위반 등 실패는 예외로 전파된다.
     */
    suspend fun bind(
        challengeId: String,
        apps: List<BoundScreenApp>,
    )
}
