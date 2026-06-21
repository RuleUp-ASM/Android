package com.ruleup.android_ruleup

import android.app.Application
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.ruleup.shared.AppConfig
import com.ruleup.shared.createAppGraph
import com.ruleup.shared.di.AppGraph
import com.ruleup.shared.syncSchedulerOf
import com.ruleup.shared.workerFactoryOf

class App :
    Application(),
    Configuration.Provider {
    // 앱 전역 DI 그래프. onCreate 에서 1회 생성해 MainActivity 등에서 참조한다.
    // 그래프 생성은 :shared(createAppGraph)에 위임해 iOS(MainViewController)와 진입 계층을 통일한다.
    lateinit var appGraph: AppGraph
        private set

    // WorkManager 는 기본 이니셜라이저를 매니페스트에서 제거(on-demand 초기화)하고, 첫 getInstance 시
    // 본 Configuration 으로 초기화된다. Metro 주입 WorkerFactory 를 등록한다(명세 §3, Hilt 대체).
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactoryOf(appGraph))
                .build()

    override fun onCreate() {
        super.onCreate()

        // BASE_URL 은 local.properties → :shared 의 AppConfig 로 단일 관리(iOS 와 동일 소스).
        appGraph = createAppGraph(this, AppConfig.BASE_URL)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 30분 주기 자동인증 sync 예약(이미 예약돼 있으면 유지). WorkManager 를 여기서 처음 깨운다.
        syncSchedulerOf(appGraph).ensureScheduled()
    }
}
