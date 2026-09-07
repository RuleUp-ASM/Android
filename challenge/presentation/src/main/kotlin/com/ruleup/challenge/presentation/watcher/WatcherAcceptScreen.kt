package com.ruleup.challenge.presentation.watcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptFailure
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptIntent
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptState
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptViewModel
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 감시자 초대 수락 (카카오톡 `/w/{token}` 링크 진입).
 *
 * **자동으로 수락하지 않는다** — 수락이 곧 수신 동의라, 링크를 연 것만으로 동의가 성립하면 안 된다.
 * 무엇에 동의하는지 먼저 보여 주고 사용자가 누를 때만 보낸다.
 *
 * 감시자에게는 실패자 닉네임·챌린지명·루틴명 셋만 간다 — 이 화면에도 방 상세로 가는 진입점을
 * 두지 않는다(감시자 테크 스펙 6).
 */
@Composable
fun WatcherAcceptScreen(
    token: String,
    modifier: Modifier = Modifier,
    viewModel: WatcherAcceptViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(token) { viewModel.onIntent(WatcherAcceptIntent.Load(token)) }

    WatcherAcceptContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun WatcherAcceptContent(
    state: WatcherAcceptState,
    onIntent: (WatcherAcceptIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "감시자 초대", onBack = { onIntent(WatcherAcceptIntent.Back) })
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                state.accepted != null -> AcceptedBody(onIntent = onIntent)
                state.failure != null -> FailureBody(state = state, onIntent = onIntent)
                else -> InviteBody(isSubmitting = state.isSubmitting, onIntent = onIntent)
            }
        }
    }
}

/** 수락 전. 통지가 언제 어떤 내용으로 오는지까지 말한다 — 그게 동의의 실질이다. */
@Composable
private fun ColumnScope.InviteBody(
    isSubmitting: Boolean,
    onIntent: (WatcherAcceptIntent) -> Unit,
) {
    Text(
        text = "루틴 감시자로 초대받았어요",
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.title,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "수락하면 이 사람이 루틴에 실패한 날 알림을 받아요. 알림에는 닉네임과 챌린지 이름만 담겨요.",
        color = RuleUpTheme.colors.textSecondary,
        style = RuleUpTheme.typography.body,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    RuleUpPrimaryButton(
        text = "수락하기",
        enabled = !isSubmitting,
        onClick = { onIntent(WatcherAcceptIntent.Accept) },
    )
    Spacer(Modifier.height(10.dp))
    TextAction(text = "다음에 할게요") { onIntent(WatcherAcceptIntent.Back) }
}

@Composable
private fun ColumnScope.AcceptedBody(onIntent: (WatcherAcceptIntent) -> Unit) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(RuleUpTheme.colors.successContainer)
                .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(text = "수락 완료", color = RuleUpTheme.colors.success, style = RuleUpTheme.typography.smallBold)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "이제 감시자예요",
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.title,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "실패가 확정된 날에만 알림이 가요. 언제든 마이 → 설정 → 내가 받는 알림에서 끌 수 있어요.",
        color = RuleUpTheme.colors.textSecondary,
        style = RuleUpTheme.typography.body,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    RuleUpPrimaryButton(text = "홈으로", onClick = { onIntent(WatcherAcceptIntent.GoHome) })
}

/** 실패 사유마다 다음에 할 일이 다르다 — 문구도 버튼도 그에 맞춰 갈린다. */
@Composable
private fun ColumnScope.FailureBody(
    state: WatcherAcceptState,
    onIntent: (WatcherAcceptIntent) -> Unit,
) {
    val failure = state.failure ?: return
    Text(
        text = failure.title,
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.title,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = state.errorMessage.orEmpty(),
        color = RuleUpTheme.colors.textSecondary,
        style = RuleUpTheme.typography.body,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    RuleUpPrimaryButton(text = "홈으로", onClick = { onIntent(WatcherAcceptIntent.GoHome) })
}

@Composable
private fun TextAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .singleClickable(onClick = onClick)
                .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.bodyMedium)
    }
}

private val WatcherAcceptFailure.title: String
    get() =
        when (this) {
            WatcherAcceptFailure.EXPIRED -> "초대가 만료됐어요"
            WatcherAcceptFailure.ALREADY_ACCEPTED -> "이미 수락했어요"
            WatcherAcceptFailure.SELF -> "내 챌린지예요"
            WatcherAcceptFailure.BLOCKED -> "지금은 수락할 수 없어요"
            WatcherAcceptFailure.INVALID -> "초대를 확인할 수 없어요"
            WatcherAcceptFailure.UNKNOWN -> "수락하지 못했어요"
        }
