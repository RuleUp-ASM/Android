package com.ruleup.tti.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
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

    /** false 면 콘텐츠가 그려질 때 BIG_PART_LOADING 을 건너뛴다. 화면이 직접 여닫는 경우 true. */
    internal var measureLargeContentSeparately = false

    /** 아직 끝나지 않은 큰 덩어리 수. */
    private var pendingLargeContents = 0

    /**
     * VIEW_BINDING 이 열렸는가. 마커가 draw 에서 읽으므로, 로딩 전부터 떠 있던 콘텐츠도
     * 이 값이 바뀌는 프레임에 다시 그려진다.
     */
    private var viewBindingOpened by mutableStateOf(false)

    fun start(timeline: TtiTimeline) {
        if (!started.add(timeline)) return
        recorder.startRecord(timeline, tti, pageName)
        if (timeline == TtiTimeline.VIEW_BINDING) viewBindingOpened = true
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

    /** 데이터 콘텐츠가 그려졌다. 로딩이 끝나 열린 VIEW_BINDING 만 닫는다. */
    internal fun contentDrawn() {
        // 로딩 중에 그려진 콘텐츠는 데이터가 그려진 게 아니다
        if (!viewBindingOpened || TtiTimeline.VIEW_BINDING in ended) return
        end(TtiTimeline.VIEW_BINDING)
        // 그려진 시점까지 나타나지 않은 큰 덩어리는 이 화면에 없는 것이다
        if (!measureLargeContentSeparately) skip(TtiTimeline.BIG_PART_LOADING)
    }

    /** 큰 덩어리 하나가 로딩을 시작했다. 첫 덩어리가 BIG_PART_LOADING 을 연다. */
    internal fun largeContentStarted() {
        if (pendingLargeContents++ == 0) start(TtiTimeline.BIG_PART_LOADING)
    }

    /** 큰 덩어리 하나가 끝났다(성공·실패). 마지막 덩어리가 BIG_PART_LOADING 을 닫는다. */
    internal fun largeContentSettled() {
        if (pendingLargeContents == 0) return
        if (--pendingLargeContents == 0) end(TtiTimeline.BIG_PART_LOADING)
    }
}

/** 큰 덩어리 하나의 로딩. 시작 전·두 번째 끝남은 무시한다. */
internal class TtiLargeContent(
    private val page: TtiPageScope,
) {
    private var started = false
    private var settled = false

    fun start() {
        if (started) return
        started = true
        page.largeContentStarted()
    }

    fun settle() {
        if (!started || settled) return
        settled = true
        page.largeContentSettled()
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

/**
 * 초기 데이터 측정. 로딩이 끝난 리컴포지션에서 BACKEND 를 닫고 VIEW_BINDING 을 연다.
 * VIEW_BINDING 은 [ttiContentDrawn] 을 붙인 콘텐츠가 처음 그려질 때 닫힌다.
 */
@Composable
fun TtiScreenEffect(
    loading: Boolean = false,
    measureLargeContentSeparately: Boolean = false,
) {
    val page = LocalTtiPage.current ?: return
    page.measureLargeContentSeparately = measureLargeContentSeparately
    if (loading) {
        page.start(TtiTimeline.BACKEND)
    } else {
        page.end(TtiTimeline.BACKEND)
        page.start(TtiTimeline.VIEW_BINDING)
    }
}

/**
 * 로딩이 끝나야 나타나는 데이터 콘텐츠의 루트에 붙인다.
 * 첫 draw 에서 VIEW_BINDING 을 닫는다.
 */
fun Modifier.ttiContentDrawn(): Modifier = this then TtiContentDrawnElement

private data object TtiContentDrawnElement : ModifierNodeElement<TtiContentDrawnNode>() {
    override fun create() = TtiContentDrawnNode()

    override fun update(node: TtiContentDrawnNode) = Unit
}

private class TtiContentDrawnNode :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        currentValueOf(LocalTtiPage)?.contentDrawn()
    }
}

/**
 * 이 자리에 뒤늦게 채워지는 큰 덩어리(이미지 등)가 있다고 알린다.
 * 돌려받은 함수는 로딩이 끝났을 때(성공·실패) 부른다. 끝나기 전에 사라져도 끝난 것으로 친다.
 */
@Composable
fun rememberTtiLargeContent(): () -> Unit {
    val page = LocalTtiPage.current
    val content = remember(page) { page?.let { TtiLargeContent(it) } }
    DisposableEffect(content) {
        content?.start()
        onDispose { content?.settle() }
    }
    return remember(content) { { content?.settle() } }
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
