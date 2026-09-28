package com.ruleup.android_ruleup.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.observability.data.ObservabilityDiagnostics
import com.ruleup.observability.debug.InspectorLog
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// 접힌 상태에서 보이는 줄 수 × 줄당 최대 행 수가 화면을 넘지 않아야 한다.
private const val COLLAPSED_LINES = 8
private const val EXPANDED_LINES = 100
private const val MAX_ROWS_PER_LINE = 2

/** 오버레이는 ViewModel 이 없으므로 EntryPoint 로 그래프에서 진단 값을 꺼낸다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface DiagnosticsEntryPoint {
    fun diagnostics(): ObservabilityDiagnostics
}

/** 디버그 빌드 전용 로그 오버레이. */
@Composable
fun DebugLogOverlay(modifier: Modifier = Modifier) {
    val version by InspectorLog.version.collectAsState()
    val context = LocalContext.current
    val diagnostics =
        remember {
            EntryPointAccessors
                .fromApplication(context.applicationContext, DiagnosticsEntryPoint::class.java)
                .diagnostics()
        }
    var expanded by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(Channel.entries.toSet()) }

    val lineCount = if (expanded) EXPANDED_LINES else COLLAPSED_LINES
    val recent = remember(version, expanded, visible) { InspectorLog.recent(visible, lineCount) }
    val floors = remember(version, visible) { diagnostics.config().channelFloors }
    val anomalies = remember(version) { diagnostics.anomalySummary() }

    val scrollState = rememberScrollState()
    LaunchedEffect(version, expanded) {
        if (expanded) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .widthIn(max = if (expanded) 320.dp else 260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = if (expanded) 0.85f else 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Channel.entries.forEach { channel ->
                    val shown = channel in visible
                    Text(
                        text = "${channel.name.take(4)}=${(floors[channel] ?: Severity.VERBOSE).name.take(4)}",
                        color = if (shown) Color(0xFF9AD0FF) else Color(0xFF5A5A5A),
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier =
                            Modifier
                                .clickable { visible = toggle(visible, channel) }
                                .padding(end = 6.dp),
                    )
                }
                if (anomalies.isNotEmpty()) {
                    Text(
                        text = anomalies,
                        color = Color(0xFFFFD166),
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Color(0xFFE0E0E0),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }

            val lines: @Composable () -> Unit = {
                recent.forEach { entry ->
                    Text(
                        text = entry.format(),
                        color = entry.color(),
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = MAX_ROWS_PER_LINE,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(scrollState),
                ) { lines() }
            } else {
                lines()
            }
        }
    }
}

/** 칩 탭 규칙. */
private fun toggle(
    current: Set<Channel>,
    channel: Channel,
): Set<Channel> {
    val all = Channel.entries.toSet()
    return when {
        current == all -> setOf(channel)
        channel in current && current.size == 1 -> all
        channel in current -> current - channel
        else -> current + channel
    }
}

private fun InspectorLog.Entry.format(): String =
    buildString {
        tag?.let { append("$it: ") }
        append(message)
        screen?.let { append(" @$it") }
    }

private fun InspectorLog.Entry.color(): Color =
    when (severity) {
        Severity.ERROR -> Color(0xFFFF6B6B)
        Severity.WARN -> Color(0xFFFFD166)
        Severity.INFO -> Color(0xFF8CE99A)
        else -> Color(0xFFE0E0E0)
    }
