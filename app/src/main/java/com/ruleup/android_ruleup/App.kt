package com.ruleup.android_ruleup

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.ruleup.shared.AppConfig
import com.ruleup.shared.di.AndroidAppGraph
import dev.zacsweers.metro.createGraphFactory

class App : Application() {
    // 앱 전역 DI 그래프. onCreate 에서 1회 생성해 MainActivity 등에서 참조한다.
    lateinit var appGraph: AndroidAppGraph
        private set

    override fun onCreate() {
        super.onCreate()

        // BASE_URL 은 local.properties → :shared 의 AppConfig 로 단일 관리(iOS 와 동일 소스).
        appGraph = createGraphFactory<AndroidAppGraph.Factory>().create(this, AppConfig.BASE_URL)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
