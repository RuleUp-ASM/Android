package com.ruleup.android_ruleup

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class PreviewSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @OptIn(InternalComposeApi::class)
    @Test
    fun `화면 미리보기는 호스트나 네트워크 없이 렌더링된다`() {
        val selected = mutableStateOf<Pair<String, String>?>(null)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                val preview = selected.value
                if (preview != null) {
                    key(preview) {
                        val method = Class.forName(preview.first).declaredMethods.single { it.name == preview.second }
                        method.isAccessible = true
                        method.invoke(null, currentComposer, 0)
                    }
                }
            }
        }
        val previews = javaClass.getResourceAsStream("/preview-smoke-cases.txt")!!.bufferedReader().readLines()
        for (entry in previews) {
            val (className, methodName) = entry.split('#')
            try {
                compose.runOnIdle { selected.value = className to methodName }
                compose.waitForIdle()
            } catch (failure: Throwable) {
                throw AssertionError("미리보기 렌더링 실패: $entry", failure)
            }
        }
    }
}
