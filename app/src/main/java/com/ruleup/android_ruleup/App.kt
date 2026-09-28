package com.ruleup.android_ruleup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.kakao.vectormap.KakaoMapSdk
import com.ruleup.android_ruleup.logging.CurrentUserHolder
import com.ruleup.android_ruleup.push.PushTokenRegister
import com.ruleup.domain.token.TokenRepository
import com.ruleup.logging.domain.BizLogger
import com.ruleup.observability.data.UserIdentitySync
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.api.w
import com.ruleup.tti.domain.TtiRecorder
import com.ruleup.verification.domain.repository.GeofenceRegister
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.usecase.SubmitDeviceIntroUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    Configuration.Provider {
    // WorkManager 수동 초기화 및 HiltWorkerFactory 등록.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var submitDeviceIntro: SubmitDeviceIntroUseCase

    @Inject
    lateinit var tokenRepository: TokenRepository

    @Inject
    lateinit var geofenceRegister: GeofenceRegister

    @Inject
    lateinit var pushTokenRegister: PushTokenRegister

    // 관측 파이프라인.
    @Inject
    lateinit var observability: Observability

    // 분석 SDK 의 사용자 상태.
    @Inject
    lateinit var userIdentitySync: UserIdentitySync

    // 화면별 TTI 기록기.
    @Inject
    lateinit var ttiRecorder: TtiRecorder

    // 비즈니스 이벤트 기록기.
    @Inject
    lateinit var bizLogger: BizLogger

    // 이벤트에 실리는 사용자 식별자.
    @Inject
    lateinit var currentUserHolder: CurrentUserHolder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        // Logcat 출력과 화면 오버레이는 관측 파이프라인의 싱크가 맡는다.
        if (BuildConfig.DEBUG) {
            // 카카오 콘솔(네이티브 앱키 → Android 플랫폼)에 등록할 키해시.
            observability.i("KakaoMap") { "등록용 키해시 = ${Utility.getKeyHash(this)} / 패키지 = $packageName" }
        }
        // 기록기들을 전면/후면에 맞춰 열고 닫는다.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    ttiRecorder.init()
                    bizLogger.init()
                }

                override fun onStop(owner: LifecycleOwner) {
                    ttiRecorder.destroy()
                    bizLogger.destroy()
                }
            },
        )

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 지도 SDK(v2)는 로그인 SDK 와 별개로 초기화하며 같은 네이티브 앱키를 쓴다.
        runCatching { KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
            .onFailure { observability.w("KakaoMap", it) { "KakaoMapSdk init 실패(미지원 ABI 가능성) — 지도 비활성" } }
        // 30분 주기 자동인증 sync 예약(이미 예약돼 있으면 유지).
        syncScheduler.ensureScheduled()

        // 콜드스타트 지오펜스 reconcile
        appScope.launch {
            runCatching { geofenceRegister.reconcilePersisted() }
                .onFailure { observability.w("GeofenceReconcile", it) { "콜드스타트 지오펜스 reconcile 실패" } }
        }

        // 로그인 상태면 Phase 0 인트로 1회 전송.
        appScope.launch {
            if (tokenRepository.isLoggedIn.first()) {
                runCatching { submitDeviceIntro() }
                    .onFailure { observability.w("VerificationIntro", it) { "Phase 0 인트로 전송 실패" } }
            }
        }

        // FCM 토큰 등록.
        appScope.launch {
            tokenRepository.isLoggedIn
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    syncScheduler.ensureScheduled()
                    pushTokenRegister.registerCurrentToken()
                }
        }

        // isLoggedIn 이 아니라 userId 를 구독한다
        appScope.launch {
            tokenRepository.userId.collect {
                userIdentitySync.setUser(it)
                currentUserHolder.setUser(it)
            }
        }

        installFlushHooks()
    }

    /** 프로세스가 죽기 직전 출구 버퍼를 비운다. */
    private fun installFlushHooks() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { observability.flush() }
            previous?.uncaughtException(thread, throwable)
        }
    }

    // 백그라운드 전환 시에도 비운다.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) runCatching { observability.flush() }
    }
}
