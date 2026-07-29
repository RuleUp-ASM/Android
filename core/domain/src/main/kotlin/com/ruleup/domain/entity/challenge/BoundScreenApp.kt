package com.ruleup.domain.entity.challenge

/**
 * 스크린타임 측정 대상 앱 1개(모듈 중립 타입).
 *
 * 대상 앱 설정은 verification 소관(서버 my-screen-apps)이지만 선택 화면은 challenge 에 있어,
 * feature 간 직접 의존 없이 core 를 경유해 주고받기 위한 공용 타입이다([com.ruleup.domain.challenge.ScreenAppBindingPort]).
 */
data class BoundScreenApp(
    val packageName: String,
    val appName: String,
)
