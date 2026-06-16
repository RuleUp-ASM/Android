package com.ruleup.analytics

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Swift(Firebase iOS SDK)가 구현해 등록하는 브리지.
 *
 * Kotlin/Native 는 Firebase Objective-C SDK 를 직접 호출할 수 없으므로, 실제 이벤트 전송은
 * Swift 쪽 구현이 담당한다. Swift 가 앱 시작 시 [AnalyticsBridgeRegistry.bridge] 에 구현을 주입한다.
 */
interface AnalyticsBridge {
    fun logEvent(
        name: String,
        params: Map<String, Any>,
    )

    fun setUserId(id: String?)

    fun setUserProperty(
        key: String,
        value: String,
    )
}

/**
 * Swift 가 앱 시작(AppDelegate)에서 [bridge] 를 주입하는 전역 레지스트리.
 * 주입 전에는 null 이며, 이때 [IosAnalyticsLogger] 는 Kermit 로그로만 폴백한다.
 */
object AnalyticsBridgeRegistry {
    var bridge: AnalyticsBridge? = null
}

/**
 * iOS [AnalyticsLogger] 구현. [AnalyticsBridgeRegistry] 에 등록된 Swift 브리지로 위임한다.
 * 브리지 미등록 시(설정 누락/프리뷰 등)에는 os_log(Kermit)로만 출력해 크래시를 피한다.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosAnalyticsLogger : AnalyticsLogger {
    private val log = Logger.withTag("Analytics")

    override fun log(event: AnalyticsEvent) {
        val bridge = AnalyticsBridgeRegistry.bridge
        if (bridge != null) {
            bridge.logEvent(event.name, event.params)
        } else {
            log.d { "(no bridge) event=${event.name} params=${event.params}" }
        }
    }

    override fun setUserId(id: String?) {
        val bridge = AnalyticsBridgeRegistry.bridge
        if (bridge != null) bridge.setUserId(id) else log.d { "(no bridge) setUserId=$id" }
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        val bridge = AnalyticsBridgeRegistry.bridge
        if (bridge != null) bridge.setUserProperty(key, value) else log.d { "(no bridge) $key=$value" }
    }
}
