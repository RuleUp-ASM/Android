package com.ruleup.shared.di

import android.content.Context
import androidx.work.WorkerFactory
import com.ruleup.network.di.BaseUrl
import com.ruleup.verification.domain.port.SyncScheduler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/**
 * Android 앱 전역 DI 그래프. 각 모듈이 @ContributesBinding/@ContributesTo/@ContributesIntoMap 로
 * AppScope 에 기여한 바인딩을 한곳에 모은다. Application Context 를 그래프에 주입한다
 * (datastore/messageHelper/이미지 업로드 등에서 사용).
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {
    /** WorkManager Configuration 에 등록할 Metro 주입 WorkerFactory(자동인증 sync). */
    val verificationWorkerFactory: WorkerFactory

    /** 앱 시작 시 30분 주기 sync 예약을 보장하기 위한 스케줄러. */
    val syncScheduler: SyncScheduler

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
            @Provides @BaseUrl baseUrl: String,
        ): AndroidAppGraph
    }
}
