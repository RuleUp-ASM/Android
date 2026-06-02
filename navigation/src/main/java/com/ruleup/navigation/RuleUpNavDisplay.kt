package com.ruleup.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ruleup.core.navigation.DeepLinkResolver
import com.ruleup.core.navigation.HomeKey
import com.ruleup.core.navigation.LoginKey
import com.ruleup.core.navigation.SignupKey
import com.ruleup.presentation.nav.onboardingEntries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 전역 네비게이션 합류점. 각 feature 가 제공하는 entry 들을 합치기만 한다.
 *
 * @param initialUri cold start 진입 시의 딥링크 Uri (없으면 기본 백스택)
 * @param isLoggedIn 앱 시작 시점의 로그인 여부. 딥링크가 없을 때 시작 화면(홈 vs 온보딩)을 가른다.
 * @param newIntents 앱이 떠 있을 때 들어온 [Intent](onNewIntent) 스트림
 * @param sessionExpired 토큰 갱신 실패 등으로 세션이 강제 종료됐을 때의 신호. 받으면 로그인으로 리셋한다.
 */
@Composable
fun RuleUpNavDisplay(
    initialUri: Uri?,
    isLoggedIn: Boolean,
    newIntents: StateFlow<Intent?>,
    sessionExpired: Flow<Unit>,
) {
    val backStack = rememberNavBackStack(*remember { DeepLinkResolver.resolve(initialUri, isLoggedIn).toTypedArray() })

    // onNewIntent 로 들어온 딥링크를 백스택에 반영
    LaunchedEffect(Unit) {
        newIntents.collect { intent ->
            intent?.data?.let { uri ->
                backStack.apply {
                    clear()
                    addAll(DeepLinkResolver.resolve(uri, isLoggedIn))
                }
            }
        }
    }

    // 세션 강제 종료 시 어느 화면에 있든 백스택을 비우고 로그인으로 되돌린다.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        sessionExpired.collect {
            Toast.makeText(context, "세션이 만료되어 다시 로그인해야 해요.", Toast.LENGTH_SHORT).show()
            backStack.apply {
                clear()
                add(LoginKey)
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
