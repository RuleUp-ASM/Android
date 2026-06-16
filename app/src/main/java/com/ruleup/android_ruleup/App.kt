package com.ruleup.android_ruleup

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.ruleup.shared.AppConfig
import com.ruleup.shared.createAppGraph
import com.ruleup.shared.di.AppGraph

class App : Application() {
    // 앱 전역 DI 그래프. onCreate 에서 1회 생성해 MainActivity 등에서 참조한다.
    // 그래프 생성은 :shared(createAppGraph)에 위임해 iOS(MainViewController)와 진입 계층을 통일한다.
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()

        // BASE_URL 은 local.properties → :shared 의 AppConfig 로 단일 관리(iOS 와 동일 소스).
        appGraph = createAppGraph(this, AppConfig.BASE_URL)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
