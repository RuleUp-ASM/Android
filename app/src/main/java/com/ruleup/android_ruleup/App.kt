package com.ruleup.android_ruleup

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.ruleup.android_ruleup.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

class App : Application() {
    // 앱 전역 DI 그래프. onCreate 에서 1회 생성해 MainActivity 등에서 참조한다.
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()

        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
