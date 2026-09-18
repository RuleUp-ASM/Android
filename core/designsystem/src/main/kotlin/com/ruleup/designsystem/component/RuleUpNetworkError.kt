package com.ruleup.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 연결이 끊겼을 때의 전체 화면 상태 (Figma `1465:2`).
 *
 * 화면마다 따로 그리지 않고 하나로 둔다 — 같은 상황에 문구가 갈리면 사용자는 앱이 서로 다른 말을
 * 한다고 읽는다. 목록 안의 부분 실패는 여전히 인라인 안내를 쓴다. 이건 **화면 전체가 빈 경우**다.
 *
 * @param note 화면마다 다른 안심 문구. 자동 인증처럼 "끊겨도 잃지 않는다"고 말할 게 있을 때만 준다.
 */
@Composable
fun RuleUpNetworkError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .padding(horizontal = 32.dp)
                .padding(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(RuleUpTheme.shapes.pill)
                    .background(RuleUpTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi_off),
                contentDescription = null,
                tint = RuleUpTheme.colors.textMuted,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = "인터넷 연결이 끊겼어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Wi-Fi나 모바일 데이터를 확인한 뒤\n다시 시도해 주세요.",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier =
                Modifier
                    .width(148.dp)
                    .height(44.dp)
                    .clip(RuleUpTheme.shapes.medium)
                    .background(RuleUpTheme.colors.brand)
                    .singleClickable(onClick = onRetry),
            horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh_cw),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(text = "다시 시도", color = Color.White, style = RuleUpTheme.typography.bodyBold)
        }
        if (note != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RuleUpTheme.shapes.medium)
                        .background(RuleUpTheme.colors.surface)
                        .padding(horizontal = RuleUpTheme.spacing.md, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = RuleUpTheme.colors.success,
                    modifier = Modifier.size(14.dp),
                )
                Text(text = note, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.caption)
            }
        }
    }
}

@Preview
@Composable
private fun RuleUpNetworkErrorPreview() {
    RuleUpTheme {
        RuleUpNetworkError(
            onRetry = {},
            note = "인증 신호는 기기에 모아뒀다가 연결되면 자동으로 보내요",
        )
    }
}
