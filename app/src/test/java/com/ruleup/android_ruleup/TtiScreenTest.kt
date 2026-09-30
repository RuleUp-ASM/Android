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
    fun `초기 로딩이 끝난 리컴포지션에서 BACKEND 를 닫고 콘텐츠가 그려지기 전에는 전송하지 않는다`() {
        val loading = mutableStateOf(true)
        val recorder = RecordingTti()
        compose.setContent {
            CompositionLocalProvider(LocalTtiRecorder provides recorder) {
                TtiPage("test") { TtiScreenEffect(loading.value) }
            }
        }
        compose.runOnIdle { assertTrue(TtiTimeline.BACKEND !in recorder.ended) }
        compose.runOnIdle { loading.value = false }
        compose.runOnIdle {
            assertTrue(TtiTimeline.BACKEND in recorder.ended)
            // VIEW_BINDING 은 ttiContentDrawn 의 첫 draw 가 닫는다(TtiPageScopeTest)
            assertTrue(TtiTimeline.VIEW_BINDING !in recorder.ended)
            assertEquals(0, recorder.shots)
        }
    }

    @Test
    fun `지도 준비 또는 실패 신호가 오면 큰 덩어리 구간을 닫는다`() {
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
        compose.runOnIdle { assertTrue(TtiTimeline.BIG_PART_LOADING !in recorder.ended) }
        compose.runOnIdle { mapLoading.value = false }
        compose.runOnIdle { assertTrue(TtiTimeline.BIG_PART_LOADING in recorder.ended) }
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
