package com.ruleup.android_ruleup.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.android_ruleup.debug.DebugLogOverlay
import com.ruleup.android_ruleup.debug.TrackJankScreen
import com.ruleup.domain.message.MessageEffect
import com.ruleup.onboarding.domain.SplashPage
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.rememberSingleClick
import com.ruleup.ui.theme.RuleUpTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun RootComposable(
    modifier: Modifier = Modifier,
    startStack: List<NavKey> = listOf(GenericNavKey(SplashPage.PATH)),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var oneButtonDialogEffect by remember {
        mutableStateOf<MessageEffect.ShowOneButtonDialog?>(null)
    }

    RuleUpTheme {
        val backStack = rememberAppBackStack(startStack)
        val messageHelper = LocalMessageHelper.current

        // 디버그 빌드: 현재 화면(네비 경로)을 JankStats 상태로 주입 → jank 로그에 화면 이름이 함께 찍힌다.
        if (BuildConfig.DEBUG) {
            val currentScreen = (backStack.lastOrNull() as? GenericNavKey)?.path ?: "unknown"
            TrackJankScreen(currentScreen)
        }

        val onShowOneButtonDialog =
            remember<(MessageEffect.ShowOneButtonDialog) -> Unit> {
                { oneButtonDialogEffect = it }
            }
        MessageEffect(
            messageEffectFlow = messageHelper.effect,
            snackBarHostState = snackBarHostState,
            onShowOneButtonDialog = onShowOneButtonDialog,
        )

        oneButtonDialogEffect?.let { dialog ->
            AlertDialog(
                onDismissRequest = {
                    if (!dialog.cantIgnore) oneButtonDialogEffect = null
                },
                title =
                    dialog.titleText?.let { titleText ->
                        { Text(text = titleText, maxLines = Int.MAX_VALUE) }
                    },
                text = { Text(text = dialog.descText, maxLines = Int.MAX_VALUE) },
                confirmButton = {
                    TextButton(
                        onClick =
                            rememberSingleClick {
                                dialog.onClickButton?.invoke()
                                oneButtonDialogEffect = null
                            },
                    ) {
                        Text(text = dialog.buttonText)
                    }
                },
                properties =
                    DialogProperties(
                        dismissOnBackPress = !dialog.cantIgnore,
                        dismissOnClickOutside = !dialog.cantIgnore,
                    ),
            )
        }

        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackBarHostState) },
            ) { innerPadding ->
                AppNavHost(
                    backStack = backStack,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            // 디버그 빌드 전용: 우측 상단 반투명 로그 오버레이. 포인터 입력이 없어 터치를 통과시킨다.
            if (BuildConfig.DEBUG) {
                DebugLogOverlay()
            }
        }
    }
}

@Composable
private fun MessageEffect(
    messageEffectFlow: Flow<MessageEffect>,
    snackBarHostState: SnackbarHostState,
    onShowOneButtonDialog: (MessageEffect.ShowOneButtonDialog) -> Unit,
) {
    val showToast = rememberShowToast()
    val currentOnShowOneButtonDialog by rememberUpdatedState(onShowOneButtonDialog)

    LaunchedEffect(Unit) {
        messageEffectFlow.collect { effect ->
            when (effect) {
                is MessageEffect.ShowToastMsg -> showToast(effect.message)
                is MessageEffect.ShowSnackBarError -> snackBarHostState.showSnackbar(effect.message)
                is MessageEffect.ShowOneButtonDialog -> currentOnShowOneButtonDialog(effect)
            }
        }
    }
}
