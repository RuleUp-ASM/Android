package com.ruleup.android_ruleup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartStack
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.shared.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationHelper: NavigationHelper

    @Inject
    lateinit var messageHelper: MessageHelper

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

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
