package com.ruleup.android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.ruleup.data.common.activity.ActivityProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        val activityProvider =
            EntryPointAccessors
                .fromApplication(this, AppEntryPoint::class.java)
                .activityProvider()
        registerActivityLifecycleCallbacks(activityProvider)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    fun interface AppEntryPoint {
        fun activityProvider(): ActivityProvider
    }
}
