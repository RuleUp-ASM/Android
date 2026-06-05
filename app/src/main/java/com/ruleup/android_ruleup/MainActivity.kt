package com.ruleup.android_ruleup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ruleup.android_ruleup.deeplink.resolveNewIntentRoute
import com.ruleup.android_ruleup.deeplink.resolveStartStack
import com.ruleup.android_ruleup.navigation.RootComposable
import com.ruleup.android_ruleup.ui.theme.AndroidRuleUpTheme
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationHelper: NavigationHelper

    @Inject
    lateinit var messageHelper: MessageHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        val startStack = resolveStartStack(intent?.data)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalNavigationHelper provides navigationHelper,
                LocalMessageHelper provides messageHelper,
            ) {
                RootComposable(startStack = startStack)
            }
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

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    AndroidRuleUpTheme {
        Greeting("Android")
    }
}
