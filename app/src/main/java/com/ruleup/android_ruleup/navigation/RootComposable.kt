package com.ruleup.android_ruleup.navigation

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.ruleup.domain.message.MessageEffect
import com.ruleup.onboarding.domain.SplashPage
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
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
        val backStack = rememberNavBackStack(*startStack.toTypedArray())
        val navigationHelper = LocalNavigationHelper.current
        val messageHelper = LocalMessageHelper.current

        val currentKey = backStack.lastOrNull() as? GenericNavKey
        val currentRoute = currentKey?.let { appRouteByPath[it.path] }

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
                        {
                            Text(
                                text = titleText,
                                maxLines = Int.MAX_VALUE,
                            )
                        }
                    },
                text = {
                    Text(
                        text = dialog.descText,
                        maxLines = Int.MAX_VALUE,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog.onClickButton?.invoke()
                            oneButtonDialogEffect = null
                        },
                    ) {
                        Text(
                            text = dialog.buttonText,
                        )
                    }
                },
                properties =
                    DialogProperties(
                        dismissOnBackPress = !dialog.cantIgnore,
                        dismissOnClickOutside = !dialog.cantIgnore,
                    ),
            )
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackBarHostState) },
        ) { innerPadding ->
            AppNavHost(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun MessageEffect(
    messageEffectFlow: Flow<MessageEffect>,
    snackBarHostState: SnackbarHostState,
    onShowOneButtonDialog: (MessageEffect.ShowOneButtonDialog) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val currentOnShowOneButtonDialog by rememberUpdatedState(onShowOneButtonDialog)

    LaunchedEffect(Unit) {
        messageEffectFlow.collect { effect ->
            when (effect) {
                is MessageEffect.ShowToastMsg -> {
                    Toast
                        .makeText(
                            appContext,
                            effect.message,
                            Toast.LENGTH_LONG,
                        ).show()
                }

                is MessageEffect.ShowSnackBarError -> {
                    snackBarHostState.showSnackbar(effect.message)
                }

                is MessageEffect.ShowOneButtonDialog -> {
                    currentOnShowOneButtonDialog(effect)
                }
            }
        }
    }
}
