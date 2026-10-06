package com.ruleup.tti.presentation

import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecorder
import com.ruleup.tti.domain.TtiTimeline
import com.ruleup.tti.domain.TtiTimeline.BACKEND
import com.ruleup.tti.domain.TtiTimeline.BIG_PART_LOADING
import com.ruleup.tti.domain.TtiTimeline.VIEW_BINDING
import com.ruleup.tti.domain.TtiTimeline.VIEW_CREATE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 화면 하나의 구간 여닫기 규칙.
 * 여기가 깨지면 TTI 가 데이터 도착·그리기·이미지 로딩과 어긋난 시각을 잰다.
 */
class TtiPageScopeTest {
    @Test
    fun `로딩 중에 그려진 콘텐츠는 바인딩 구간을 닫지 않는다`() {
        val recorder = RecordingRecorder()
        val page = loadingPage(recorder)

        page.contentDrawn()

        assertTrue(recorder.ended.isEmpty())
    }

    @Test
    fun `데이터 콘텐츠가 처음 그려지면 바인딩을 닫고 큰 덩어리가 없으면 한 건을 쏜다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder)

        page.contentDrawn()

        assertEquals(listOf(VIEW_BINDING, BIG_PART_LOADING), recorder.ended)
        assertEquals(1, recorder.shots)
    }

    @Test
    fun `콘텐츠가 다시 그려져도 한 건을 두 번 쏘지 않는다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder)

        page.contentDrawn()
        page.contentDrawn()

        assertEquals(1, recorder.shots)
    }

    @Test
    fun `이미지가 로딩 중이면 콘텐츠가 그려져도 쏘지 않는다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder)
        TtiLargeContent(page).start()

        page.contentDrawn()

        assertEquals(0, recorder.shots)
    }

    @Test
    fun `이미지가 여럿이면 마지막 장이 끝나야 쏜다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder)
        val first = TtiLargeContent(page).also { it.start() }
        val second = TtiLargeContent(page).also { it.start() }
        page.contentDrawn()

        first.settle()
        assertEquals(0, recorder.shots)
        second.settle()

        assertEquals(1, recorder.shots)
        assertEquals(1, recorder.started.count { it == BIG_PART_LOADING })
    }

    @Test
    fun `같은 이미지가 두 번 끝나도 다른 이미지의 몫을 닫지 않는다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder)
        val first = TtiLargeContent(page).also { it.start() }
        TtiLargeContent(page).start()
        page.contentDrawn()

        // 성공 콜백 뒤 화면을 떠나며 dispose 로 한 번 더 들어온다
        first.settle()
        first.settle()

        assertEquals(0, recorder.shots)
    }

    @Test
    fun `측정이 끝난 뒤 나타난 이미지는 기록을 다시 열지 않는다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder)
        page.contentDrawn()

        TtiLargeContent(page).also { it.start() }.settle()

        assertEquals(1, recorder.started.count { it == BIG_PART_LOADING })
        assertEquals(1, recorder.shots)
    }

    @Test
    fun `큰 덩어리를 화면이 직접 재면 콘텐츠가 그려져도 건너뛰지 않는다`() {
        val recorder = RecordingRecorder()
        val page = loadedPage(recorder).apply { measureLargeContentSeparately = true }

        page.contentDrawn()

        assertEquals(listOf(VIEW_BINDING), recorder.ended)
        assertEquals(0, recorder.shots)
    }

    /** VIEW_CREATE 가 끝나고 BACKEND 가 열린 상태. */
    private fun loadingPage(recorder: RecordingRecorder): TtiPageScope =
        TtiPageScope(recorder, Tti("t"), PAGE).apply {
            start(VIEW_CREATE)
            end(VIEW_CREATE)
            start(BACKEND)
            recorder.clear()
        }

    /** 데이터가 도착해 VIEW_BINDING 이 열린 상태. */
    private fun loadedPage(recorder: RecordingRecorder): TtiPageScope =
        loadingPage(recorder).apply {
            end(BACKEND)
            start(VIEW_BINDING)
            recorder.clear()
        }

    private class RecordingRecorder : TtiRecorder {
        val started = mutableListOf<TtiTimeline>()
        val ended = mutableListOf<TtiTimeline>()
        var shots = 0

        fun clear() {
            started.clear()
            ended.clear()
        }

        override fun init() = Unit

        override fun startRecord(
            timeline: TtiTimeline,
            tti: Tti,
            pageName: String,
        ) {
            started += timeline
        }

        override fun endRecord(
            timeline: TtiTimeline,
            tti: Tti,
            pageName: String,
        ) {
            ended += timeline
        }

        override fun shot(
            tti: Tti,
            pageName: String,
        ) {
            shots++
        }

        override fun destroy() = Unit
    }

    private companion object {
        const val PAGE = "my_home"
    }
}
