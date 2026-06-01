package com.ruleup.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ruleup.core.navigation.DeepLinkResolver
import com.ruleup.core.navigation.HomeKey
import com.ruleup.core.navigation.LoginKey
import com.ruleup.core.navigation.SignupKey
import com.ruleup.presentation.nav.onboardingEntries
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 전역 네비게이션 합류점. 각 feature 가 제공하는 entry 들을 합치기만 한다.
 *
 * @param initialUri cold start 진입 시의 딥링크 Uri (없으면 기본 백스택)
 * @param newIntents 앱이 떠 있을 때 들어온 [Intent](onNewIntent) 스트림
 */
@Composable
fun RuleUpNavDisplay(
    initialUri: Uri?,
    newIntents: StateFlow<Intent?>,
) {
    val backStack = rememberNavBackStack(*remember { DeepLinkResolver.resolve(initialUri).toTypedArray() })

    // onNewIntent 로 들어온 딥링크를 백스택에 반영
    LaunchedEffect(Unit) {
        newIntents.collect { intent ->
            intent?.data?.let { uri ->
                backStack.apply {
                    clear()
                    addAll(DeepLinkResolver.resolve(uri))
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                onboardingEntries(
                    onFinishIntro = {
                        backStack.apply {
                            clear()
                            add(LoginKey)
                        }
                    },
                    onNavigateHome = {
                        backStack.apply {
                            clear()
                            add(HomeKey)
                        }
                    },
                    onNavigateSignup = { token -> backStack.add(SignupKey(token)) },
                )
                // TODO: home feature 생기면 homeEntries(...) 한 줄 추가
                entry<HomeKey> { PlaceholderScreen("Home") }
            },
    )
}

// TODO: home feature 의 HomeScreen 으로 교체
@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label)
    }
}
