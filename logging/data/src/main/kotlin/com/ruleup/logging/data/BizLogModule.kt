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

/**
 * 비즈니스 이벤트 전송 배선.
 *
 * **[BizLogConfig]·[BizScreenSource]·[BizUserSource] 는 `:app` 이 제공한다** — 키와 빌드 종류는 앱의
 * `BuildConfig` 에만 있고, 지금 화면과 로그인한 사용자는 네비게이션·세션을 쥔 쪽만 안다.
 *
 * 출구는 목록으로 조립한다. 어느 백엔드가 붙는지가 배선 코드에 그대로 드러나야, *"왜 이 이벤트가
 * 대시보드에 없지"* 를 여기 한 파일에서 답할 수 있다.
 */
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

    /**
     * 기록기는 도메인이 만든다.
     *
     * 생성자 주입 대신 여기서 조립하는 것은 순수 코틀린인 `:logging:domain` 이 안드로이드 디스패처를
     * 참조하지 않게 하려는 것이다. 전송은 전부 IO 에서 돈다.
     */
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
