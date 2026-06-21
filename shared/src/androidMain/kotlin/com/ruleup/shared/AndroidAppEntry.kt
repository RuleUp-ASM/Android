package com.ruleup.shared

import android.content.Context
import androidx.work.WorkerFactory
import com.ruleup.shared.di.AndroidAppGraph
import com.ruleup.shared.di.AppGraph
import com.ruleup.verification.domain.port.SyncScheduler
import dev.zacsweers.metro.createGraphFactory

/**
 * Android 앱 전역 DI 그래프를 생성한다. iOS 의 [MainViewController] 와 대칭으로 컴포지션 루트(그래프 생성)를
 * :shared 에 두어, :app 은 생명주기 호스트(Application/Activity)·플랫폼 초기화만 담당하는 얇은 셸로 남긴다.
 *
 * [Context] 는 datastore·이미지 업로드 등 Android 바인딩에 필요하므로 Application.onCreate 에서 1회 호출한다.
 */
fun createAppGraph(
    context: Context,
    baseUrl: String,
): AppGraph = createGraphFactory<AndroidAppGraph.Factory>().create(context, baseUrl)

/**
 * WorkManager 통합 접근자. [AndroidAppGraph] 캐스팅을 :shared 안에서 수행해, :app 이 그래프의 전체
 * Metro 기여 supertype 을 컴파일 클래스패스에 두지 않아도 되게 한다(:app 은 얇은 셸 유지).
 */
fun workerFactoryOf(graph: AppGraph): WorkerFactory = (graph as AndroidAppGraph).verificationWorkerFactory

fun syncSchedulerOf(graph: AppGraph): SyncScheduler = (graph as AndroidAppGraph).syncScheduler
