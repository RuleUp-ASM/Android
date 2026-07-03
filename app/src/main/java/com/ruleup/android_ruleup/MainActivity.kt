package com.ruleup.android_ruleup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.metrics.performance.JankStats
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartStack
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationHelper: NavigationHelper

    @Inject
    lateinit var messageHelper: MessageHelper

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    // 프레임 jank 측정(디버그 빌드만). janky 프레임을 Timber 로 남기면 디버그 오버레이([DebugLogOverlay])에도 뜬다.
    private var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val startStack = resolveStartStack(intent?.data)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot(
                navigationHelper = navigationHelper,
                messageHelper = messageHelper,
                analyticsLogger = analyticsLogger,
                startStack = startStack,
            )
        }
        if (BuildConfig.DEBUG) {
            jankStats =
                JankStats.createAndTrack(window) { frameData ->
                    if (frameData.isJank) {
                        val ms = frameData.frameDurationUiNanos / 1_000_000.0
                        val states = frameData.states.joinToString { "${it.key}=${it.value}" }
                        Timber
                            .tag("Jank")
                            .w("느린 프레임 %.1fms%s".format(ms, if (states.isEmpty()) "" else " [$states]"))
                    }
                }
        }
    }

    // 화면이 보일 때만 측정(JankStats 권장). 디버그가 아니면 jankStats == null 이라 no-op.
    override fun onResume() {
        super.onResume()
        jankStats?.isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        jankStats?.isTrackingEnabled = false
    }

    // 앱이 떠 있는 동안 들어온 딥링크(ruleup://app/...) 처리. 미등록 path 는 무시된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data
            ?.let { resolveNewIntentRoute(it) }
            ?.let { navigationHelper.navigateByRoute(it) }
    }
}
