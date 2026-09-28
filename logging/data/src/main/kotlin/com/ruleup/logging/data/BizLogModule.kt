package com.ruleup.logging.data

import android.content.Context
import com.ruleup.logging.data.sink.AmplitudeBizShooter
import com.ruleup.logging.data.sink.BizShooterFailureReporter
import com.ruleup.logging.data.sink.CompositeBizShooter
import com.ruleup.logging.data.sink.FirebaseBizShooter
import com.ruleup.logging.data.sink.LogcatBizShooter
import com.ruleup.logging.domain.BizLogClock
import com.ruleup.logging.domain.BizLogConfig
import com.ruleup.logging.domain.BizLogShooter
import com.ruleup.logging.domain.BizLogger
import com.ruleup.logging.domain.BizScreenSource
import com.ruleup.logging.domain.BizUserSource
import com.ruleup.logging.domain.SystemBizLogClock
import com.ruleup.logging.domain.createBizLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/** 비즈니스 이벤트 전송 배선. */
@Module
@InstallIn(SingletonComponent::class)
object BizLogModule {
    @Provides
    @Singleton
    fun bizLogClock(): BizLogClock = SystemBizLogClock()

    @Provides
    @Singleton
    fun bizLogShooter(
        @ApplicationContext context: Context,
        config: BizLogConfig,
        failureReporter: BizShooterFailureReporter,
    ): BizLogShooter {
        val children =
            buildList {
                add(FirebaseBizShooter(context))
                if (config.isAmplitudeConfigured) add(AmplitudeBizShooter(context, config))
                if (config.debuggable) add(LogcatBizShooter())
            }
        return CompositeBizShooter(children, failureReporter)
    }

    /** 기록기는 도메인이 만든다. */
    @Provides
    @Singleton
    fun bizLogger(
        shooter: BizLogShooter,
        clock: BizLogClock,
        screenSource: BizScreenSource,
        userSource: BizUserSource,
    ): BizLogger =
        createBizLogger(
            shooter = shooter,
            clock = clock,
            screenSource = screenSource,
            userSource = userSource,
            ioDispatcher = Dispatchers.IO,
        )
}
