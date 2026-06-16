package com.ruleup.android_ruleup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartStack
import com.ruleup.shared.AppRoot

class MainActivity : ComponentActivity() {
    // App 에서 생성된 전역 그래프. 필드 주입(@AndroidEntryPoint) 대신 그래프 accessor 로 의존성을 꺼낸다.
    private val graph by lazy { (application as App).appGraph }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startStack = resolveStartStack(intent?.data)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot(graph = graph, startStack = startStack)
        }
    }

    // 앱이 떠 있는 동안 들어온 딥링크(ruleup://app/...) 처리. 미등록 path 는 무시된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data
            ?.let { resolveNewIntentRoute(it) }
            ?.let { graph.navigationHelper.navigateByRoute(it) }
    }
}
