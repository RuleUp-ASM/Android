package com.ruleup.android_ruleup

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.kakao.vectormap.KakaoMapSdk
import com.ruleup.android_ruleup.push.PushTokenRegistrar
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.data.UserIdentitySync
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.api.w
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.usecase.SubmitDeviceIntroUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    lateinit var geofenceRegistrar: GeofenceRegistrar

    // FCM 토큰 서버 등록(기기 1대 = 토큰 1개 upsert). 앱 시작 + onNewToken 경로가 공유한다.
    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    // 관측 파이프라인. 여기서 주입해야 앱 시작 시점에 그래프가 만들어진다.
    @Inject
    lateinit var observability: Observability

    // 분석 SDK 의 사용자 상태. 이벤트에 실리는 값이 아니라 SDK 가 들고 있는 상태다.
    @Inject
    lateinit var userIdentitySync: UserIdentitySync

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        // Logcat 출력과 화면 오버레이는 관측 파이프라인의 싱크가 맡는다(LogcatSink · 인스펙터).
        if (BuildConfig.DEBUG) {
            // 카카오 콘솔(네이티브 앱키 → Android 플랫폼)에 등록할 키해시. 등록 안 되면 지도 인증 실패로 빈 화면.
            observability.i("KakaoMap") { "등록용 키해시 = ${Utility.getKeyHash(this)} / 패키지 = $packageName" }
        }
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        // 지도 SDK(v2)는 로그인 SDK 와 별개로 초기화한다. 같은 네이티브 앱키를 쓴다(:core:map 이 MapView 사용).
        // 카카오 지도 네이티브 라이브러리(libK3fAndroid.so)는 arm64-v8a/armeabi-v7a 만 제공 → x86_64 에뮬레이터에선
        // init 이 MissingLibraryException 을 던진다. 앱 전체가 죽지 않도록 방어한다(지도 화면만 비활성, 나머지 정상).
        runCatching { KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
            .onFailure { observability.w("KakaoMap", it) { "KakaoMapSdk init 실패(미지원 ABI 가능성) — 지도 비활성" } }
        // 30분 주기 자동인증 sync 예약(이미 예약돼 있으면 유지). WorkManager 를 여기서 처음 깨운다.
        syncScheduler.ensureScheduled()

        // 콜드스타트 지오펜스 reconcile — OS 등록 실패/휘발분을 앱 시작마다 재등록한다. 실패는 다음 시작이 보정.
        appScope.launch {
            runCatching { geofenceRegistrar.reconcilePersisted() }
                .onFailure { observability.w("GeofenceReconcile", it) { "콜드스타트 지오펜스 reconcile 실패" } }
        }

        // 로그인 상태면 Phase 0 인트로 1회 전송(전송 스펙 §0.3). 실패는 무시 — 다음 시작/주기 sync 가 정책을 보정.
        appScope.launch {
            if (tokenRepository.isLoggedIn.first()) {
                runCatching { submitDeviceIntro() }
                    .onFailure { observability.w("VerificationIntro", it) { "Phase 0 인트로 전송 실패" } }
            }
        }

        // FCM 토큰 등록(로그인 상태 upsert — PushTokenRegistrar 내부에서 판단). 실패는 다음 시작/onNewToken 이 보정.
        appScope.launch {
            pushTokenRegistrar.registerCurrentToken()
        }

        // 사용자 식별자를 분석 SDK 에 반영한다. isLoggedIn 이 아니라 userId 를 구독하는 이유는
        // 갱신 응답이 userId 를 안 주는 서버 배포본에서 로그인 상태여도 이 값이 비어 있을 수 있기
        // 때문이다(TokenRepository.userId KDoc 참고).
        appScope.launch {
            tokenRepository.userId.collect { userIdentitySync.setUser(it) }
        }

        installFlushHooks()
    }

    /**
     * 프로세스가 죽기 직전 출구 버퍼를 비운다.
     *
     * **기존 핸들러를 교체하지 않고 체이닝한다.** Crashlytics 가 `FirebaseInitProvider`(ContentProvider)로
     * [onCreate] 이전에 자기 핸들러를 심으므로, 여기서 잡히는 [previous] 가 그것이다. 갈아치우면
     * 크래시 수집이 통째로 죽는다.
     */
    private fun installFlushHooks() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { observability.flush() }
            previous?.uncaughtException(thread, throwable)
        }
    }

    // 백그라운드 전환 시에도 비운다. 프로세스가 조용히 회수되는 경로가 크래시보다 흔하다.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) runCatching { observability.flush() }
    }
}
