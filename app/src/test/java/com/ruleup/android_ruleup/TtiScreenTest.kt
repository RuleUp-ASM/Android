package com.ruleup.android_ruleup

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecorder
import com.ruleup.tti.domain.TtiTimeline
import com.ruleup.tti.presentation.LocalTtiRecorder
import com.ruleup.tti.presentation.TtiPage
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.TtiSpanEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class TtiScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `초기 로딩 완료 후 프레임을 기다리고 한번만 전송한다`() {
        val loading = mutableStateOf(true)
        val recorder = RecordingTti()
        compose.setContent {
            CompositionLocalProvider(LocalTtiRecorder provides recorder) {
                TtiPage("test") { TtiScreenEffect(loading.value) }
            }
        }
        compose.runOnIdle { assertTrue(recorder.ended.isEmpty() || TtiTimeline.BACKEND !in recorder.ended) }
        compose.runOnIdle { loading.value = false }
        compose.runOnIdle { assertEquals(1, recorder.shots) }
        compose.runOnIdle { loading.value = true }
        compose.runOnIdle { loading.value = false }
        compose.runOnIdle { assertEquals(1, recorder.shots) }
    }

    @Test
    fun `지도 준비 또는 실패 신호까지 전송을 기다린다`() {
        val mapLoading = mutableStateOf(true)
        val recorder = RecordingTti()
        compose.setContent {
            CompositionLocalProvider(LocalTtiRecorder provides recorder) {
                TtiPage("map") {
                    TtiScreenEffect(measureLargeContentSeparately = true)
                    TtiSpanEffect(TtiTimeline.BIG_PART_LOADING, mapLoading.value)
                }
            }
        }
        compose.runOnIdle { assertEquals(0, recorder.shots) }
        compose.runOnIdle { mapLoading.value = false }
        compose.runOnIdle { assertEquals(1, recorder.shots) }
    }

    private class RecordingTti : TtiRecorder {
        val ended = mutableSetOf<TtiTimeline>()
        var shots = 0

        override fun init() = Unit

        override fun destroy() = Unit

        override fun startRecord(
            timeline: TtiTimeline,
            tti: Tti,
            pageName: String,
        ) = Unit

        override fun endRecord(
            timeline: TtiTimeline,
            tti: Tti,
            pageName: String,
        ) {
            ended.add(timeline)
        }

        override fun shot(
            tti: Tti,
            pageName: String,
        ) {
            shots++
        }
    }
}
