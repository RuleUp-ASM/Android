package com.ruleup.tti.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecorder
import com.ruleup.tti.domain.TtiTimeline

/** 화면 트리에 내려주는 기록기. */
val LocalTtiRecorder = staticCompositionLocalOf<TtiRecorder?> { null }

/** 지금 측정 중인 화면. */
val LocalTtiPage = staticCompositionLocalOf<TtiPageScope?> { null }

/** 화면 하나의 측정 손잡이. */
class TtiPageScope internal constructor(
    private val recorder: TtiRecorder,
    val tti: Tti,
    val pageName: String,
) {
    private val started = mutableSetOf<TtiTimeline>()
    private val ended = mutableSetOf<TtiTimeline>()

    fun start(timeline: TtiTimeline) {
        if (!started.add(timeline)) return
        recorder.startRecord(timeline, tti, pageName)
    }

    fun end(timeline: TtiTimeline) {
        // 열린 적 없는 구간은 길이 0 으로 남긴다
        if (timeline !in started) {
            skip(timeline)
            return
        }
        if (!ended.add(timeline)) return
        recorder.endRecord(timeline, tti, pageName)
        // 마지막 구간이 닫혔다면 그 자리에서 쏜다.
        if (ended.size == TtiTimeline.REQUIRED_COUNT) shot()
    }

    /** 이 화면에 [timeline] 에 해당하는 것이 아예 없다고 알린다 */
    fun skip(timeline: TtiTimeline) {
        if (timeline in started) return
        start(timeline)
        end(timeline)
    }

    fun shot() {
        recorder.shot(tti, pageName)
    }
}

/** 한 화면의 측정을 연다. */
@Composable
fun TtiPage(
    pageName: String,
    content: @Composable () -> Unit,
) {
    val recorder = LocalTtiRecorder.current
    // 기록기가 없으면 page 도 null 이고 아래 helper 들이 전부 no-op 이 된다.
    val page = remember(recorder, pageName) { recorder?.let { TtiPageScope(it, Tti(), pageName) } }

    page?.start(TtiTimeline.VIEW_CREATE)
    CompositionLocalProvider(LocalTtiPage provides page) {
        content()
    }
    page?.end(TtiTimeline.VIEW_CREATE)
}

/** 초기 데이터와 첫 렌더 완료 측정. */
@Composable
fun TtiScreenEffect(
    loading: Boolean = false,
    measureLargeContentSeparately: Boolean = false,
) {
    val page = LocalTtiPage.current ?: return
    LaunchedEffect(page, loading) {
        if (loading) {
            page.start(TtiTimeline.BACKEND)
        } else {
            withFrameNanos { }
            page.end(TtiTimeline.BACKEND)
            page.start(TtiTimeline.VIEW_BINDING)
            withFrameNanos { }
            page.end(TtiTimeline.VIEW_BINDING)
            if (!measureLargeContentSeparately) page.skip(TtiTimeline.BIG_PART_LOADING)
        }
    }
}

/** [running] 이 true 인 동안 [timeline] 구간을 연다. */
@Composable
fun TtiSpanEffect(
    timeline: TtiTimeline,
    running: Boolean,
) {
    val page = LocalTtiPage.current ?: return
    LaunchedEffect(page, timeline, running) {
        if (running) page.start(timeline) else page.end(timeline)
    }
}

/** 이 화면에는 [timeline] 에 해당하는 것이 아예 없다고 알린다. */
@Composable
fun TtiEmptySpanEffect(timeline: TtiTimeline) {
    val page = LocalTtiPage.current ?: return
    LaunchedEffect(page, timeline) {
        page.skip(timeline)
    }
}
