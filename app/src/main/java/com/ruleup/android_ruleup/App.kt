package com.ruleup.android_ruleup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.ruleup.verification.domain.port.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    Configuration.Provider {
    // WorkManager 는 매니페스트에서 기본 이니셜라이저를 제거(on-demand)하고, 첫 getInstance 시 본 Configuration 으로
    // 초기화된다. @HiltWorker 들을 인스턴스화하는 HiltWorkerFactory 를 등록한다(명세 §3).
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 앱 시작 시 30분 주기 sync 예약을 보장하기 위한 스케줄러.
    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 30분 주기 자동인증 sync 예약(이미 예약돼 있으면 유지). WorkManager 를 여기서 처음 깨운다.
        syncScheduler.ensureScheduled()
    }
}
