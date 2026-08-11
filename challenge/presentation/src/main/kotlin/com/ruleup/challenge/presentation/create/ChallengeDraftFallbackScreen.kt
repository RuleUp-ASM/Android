package com.ruleup.challenge.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 초안 생성 폴백 화면 (Figma `1134:737` · 생성 · AI 폴백).
 *
 * 서버가 `result=FALLBACK` 을 준 경우다 — **HTTP 200 이고 에러가 아니다.** 그래서 에러 색을 쓰지 않고
 * 다음 행동 세 가지를 제시한다.
 *
 * 입력한 설명은 상태에 그대로 남는다. "처음부터 직접 작성" 으로 돌아가면 쓰던 문장이 살아 있다 —
 * 지우면 사용자가 같은 문장을 다시 쓰게 만드는 벌이 된다.
 */
@Composable
fun ChallengeDraftFallbackContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    state: CreateChallengeState = CreateChallengeState.initial,
) {
    val nav = LocalNavigationHelper.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(22.dp)
                        .singleClickable { nav.navigateToBack() },
            )
            Text("새 챌린지", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.section)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RuleUpTheme.shapes.cardLarge)
                        .background(RuleUpTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    tint = RuleUpTheme.colors.textSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "지금은 초안을 만들지 못했어요",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.title,
                textAlign = TextAlign.Center,
            )
            Text(
                // 서버가 준 안내가 있으면 그걸 먼저 쓴다 — 왜 막혔는지는 서버가 더 잘 안다.
                text = state.fallbackMessage ?: "잠시 후 다시 시도하거나,\n추천 루틴으로 바로 시작할 수 있어요",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FallbackButton(
                text = "다시 시도",
                filled = true,
                // 429 로 막혀 있으면 다시 눌러도 소용없다 — 남은 시간 동안 잠근다.
                enabled = state.retryAfterSeconds == null,
                onClick = { onIntent(CreateChallengeIntent.SubmitDescription) },
            )
            FallbackButton(
                text = "추천 루틴으로 시작",
                filled = false,
                onClick = { onIntent(CreateChallengeIntent.DismissFallback) },
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .singleClickable { onIntent(CreateChallengeIntent.DismissFallback) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "처음부터 직접 작성",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun FallbackButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RuleUpTheme.shapes.large)
                .background(
                    when {
                        !enabled -> RuleUpTheme.colors.border
                        filled -> RuleUpTheme.colors.brand
                        else -> RuleUpTheme.colors.surface
                    },
                ).let { base ->
                    if (filled) base else base.border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                }.singleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) RuleUpTheme.colors.onSuccess else RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
    }
}
