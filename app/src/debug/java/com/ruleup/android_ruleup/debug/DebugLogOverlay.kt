package com.ruleup.android_ruleup.debug

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val VISIBLE_LINES = 12

/**
 * 디버그 빌드 전용 로그 오버레이. 우측 상단에 반투명 패널로 최근 [VISIBLE_LINES] 줄을 띄운다.
 *
 * 포인터 입력(clickable/pointerInput/scroll) 모디파이어를 일절 두지 않으므로 hit-test 대상이 아니다 →
 * 터치가 뒤 컨텐츠로 그대로 통과한다(click-through). 그래서 스크롤 없이 최신 N줄만 tail 한다.
 */
@Composable
fun DebugLogOverlay(modifier: Modifier = Modifier) {
    val logs by DebugLogStore.logs.collectAsState()
    val recent = logs.takeLast(VISIBLE_LINES)
    if (recent.isEmpty()) return

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            recent.forEach { entry ->
                Text(
                    text = entry.format(),
                    color = entry.color(),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun DebugLogStore.Entry.format(): String = tag?.let { "$it: $message" } ?: message

private fun DebugLogStore.Entry.color(): Color =
    when (priority) {
        Log.ERROR, Log.ASSERT -> Color(0xFFFF6B6B)
        Log.WARN -> Color(0xFFFFD166)
        Log.INFO -> Color(0xFF8CE99A)
        else -> Color(0xFFE0E0E0)
    }
