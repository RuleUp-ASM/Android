package com.ruleup.android_ruleup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.ruleup.android_ruleup.debug.DebugLogTree
import com.ruleup.verification.domain.port.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
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
        // Timber Tree 를 심지 않으면 모든 로그가 버려진다(HttpClient 태그 포함). 디버그 빌드에서만
        // Logcat 출력 + 화면 우측 상단 오버레이([DebugLogOverlay]) 동시 적재.
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugLogTree())
        }
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 지도 SDK(v2)는 로그인 SDK 와 별개로 초기화한다. 같은 네이티브 앱키를 쓴다(:core:map 이 MapView 사용).
        // 카카오 지도 네이티브 라이브러리(libK3fAndroid.so)는 arm64-v8a/armeabi-v7a 만 제공 → x86_64 에뮬레이터에선
        // init 이 MissingLibraryException 을 던진다. 앱 전체가 죽지 않도록 방어한다(지도 화면만 비활성, 나머지 정상).
        runCatching { KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
            .onFailure { Timber.w(it, "KakaoMapSdk init 실패(미지원 ABI 가능성) — 지도 비활성") }
        // 30분 주기 자동인증 sync 예약(이미 예약돼 있으면 유지). WorkManager 를 여기서 처음 깨운다.
        syncScheduler.ensureScheduled()
    }
}
