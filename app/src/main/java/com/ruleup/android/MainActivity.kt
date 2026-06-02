package com.ruleup.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.core.designsystem.theme.RuleUpTheme
import com.ruleup.navigation.RuleUpNavDisplay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val newIntents = MutableStateFlow<Intent?>(null)
    private val viewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 세션 확인이 끝날 때까지 splash 유지 (로그인 화면 깜빡임 방지)
        splashScreen.setKeepOnScreenCondition { viewModel.startState.value is AppStartState.Loading }

        setContent {
            RuleUpTheme {
                val startState by viewModel.startState.collectAsStateWithLifecycle()
                (startState as? AppStartState.Resolved)?.let { resolved ->
                    RuleUpNavDisplay(
                        initialUri = intent?.data,
                        isLoggedIn = resolved.isLoggedIn,
                        newIntents = newIntents,
                        sessionExpired = viewModel.sessionExpired,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntents.value = intent
    }
}
