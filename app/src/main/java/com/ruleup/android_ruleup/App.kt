package com.ruleup.android_ruleup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.kakao.vectormap.KakaoMapSdk
import com.ruleup.android_ruleup.debug.DebugLogTree
import com.ruleup.domain.token.TokenRepository
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.usecase.RegisterGeofencesUseCase
import com.ruleup.verification.domain.usecase.SubmitDeviceIntroUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    // Phase 0 인트로(전송 스펙 §0.3): 로그인 상태면 정적 프로필+권한 스냅샷을 보내고 서버 정책을 받는다.
    @Inject
    lateinit var submitDeviceIntro: SubmitDeviceIntroUseCase

    @Inject
    lateinit var tokenRepository: TokenRepository

    // 콜드스타트 지오펜스 reconcile(명세 §2.3): 로컬 보존 목표를 OS 에 재등록해 등록 실패·휘발을 보정한다.
    @Inject
    lateinit var registerGeofences: RegisterGeofencesUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            // 카카오 콘솔(네이티브 앱키 → Android 플랫폼)에 등록할 키해시. 등록 안 되면 지도 인증 실패로 빈 화면.
            Timber.tag("KakaoMap").i("등록용 키해시 = %s / 패키지 = %s", Utility.getKeyHash(this), packageName)
        }
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 지도 SDK(v2)는 로그인 SDK 와 별개로 초기화한다. 같은 네이티브 앱키를 쓴다(:core:map 이 MapView 사용).
        // 카카오 지도 네이티브 라이브러리(libK3fAndroid.so)는 arm64-v8a/armeabi-v7a 만 제공 → x86_64 에뮬레이터에선
        // init 이 MissingLibraryException 을 던진다. 앱 전체가 죽지 않도록 방어한다(지도 화면만 비활성, 나머지 정상).
        runCatching { KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
            .onFailure { Timber.w(it, "KakaoMapSdk init 실패(미지원 ABI 가능성) — 지도 비활성") }
        // 30분 주기 자동인증 sync 예약(이미 예약돼 있으면 유지). WorkManager 를 여기서 처음 깨운다.
        syncScheduler.ensureScheduled()

        // 콜드스타트 지오펜스 reconcile — OS 등록 실패/휘발분을 앱 시작마다 재등록한다. 실패는 다음 시작이 보정.
        appScope.launch {
            runCatching { registerGeofences() }
                .onFailure { Timber.tag("GeofenceReconcile").w(it, "콜드스타트 지오펜스 reconcile 실패") }
        }

        // 로그인 상태면 Phase 0 인트로 1회 전송(전송 스펙 §0.3). 실패는 무시 — 다음 시작/주기 sync 가 정책을 보정.
        appScope.launch {
            if (tokenRepository.isLoggedIn.first()) {
                runCatching { submitDeviceIntro() }
                    .onFailure { Timber.tag("VerificationIntro").w(it, "Phase 0 인트로 전송 실패") }
            }
        }
    }
}
