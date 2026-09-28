package com.ruleup.challenge.presentation.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.challenge.domain.entity.ChallengeInvitationPreview
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteEffect
import com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteIntent
import com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteState
import com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteViewModel
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.ui.helper.LocalMessageHelper

/** 멤버 초대 링크 진입 (카카오톡 `/c/{token}`). */
@Composable
fun ChallengeInviteScreen(
    token: String,
    modifier: Modifier = Modifier,
    viewModel: ChallengeInviteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(token) { viewModel.onIntent(ChallengeInviteIntent.Load(token)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChallengeInviteEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    ChallengeInviteContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun ChallengeInviteContent(
    state: ChallengeInviteState,
    onIntent: (ChallengeInviteIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "챌린지 초대", onBack = { onIntent(ChallengeInviteIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.preview == null ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.errorMessage ?: "초대를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    RuleUpPrimaryButton(text = "홈으로", onClick = { onIntent(ChallengeInviteIntent.GoHome) })
                }

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    InviteBody(state = state, preview = state.preview, onIntent = onIntent)
                }
        }
    }
}

@Composable
private fun ColumnScope.InviteBody(
    state: ChallengeInviteState,
    preview: ChallengeInvitationPreview,
    onIntent: (ChallengeInviteIntent) -> Unit,
) {
    preview.challenge.imageUrl?.let { url ->
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(20.dp))
    }
    preview.inviterNickname?.let {
        Text(
            text = "${it}님이 초대했어요",
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallBold,
        )
        Spacer(Modifier.height(6.dp))
    }
    Text(
        text = preview.challenge.title,
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.title,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = preview.summaryLine,
        color = RuleUpTheme.colors.textSecondary,
        style = RuleUpTheme.typography.body,
    )
    Spacer(Modifier.height(24.dp))

    val blocked = state.blockedBy ?: preview.blockReason.takeIf { !preview.joinable }
    if (blocked != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RuleUpTheme.colors.surfaceVariant)
                    .padding(16.dp),
        ) {
            Text(
                text = blocked.message,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.small,
            )
        }
        Spacer(Modifier.height(14.dp))
        RuleUpPrimaryButton(text = "홈으로", onClick = { onIntent(ChallengeInviteIntent.GoHome) })
    } else {
        RuleUpPrimaryButton(
            text = "참여하기",
            enabled = state.canAccept,
            onClick = { onIntent(ChallengeInviteIntent.Accept) },
        )
    }
}

/** "그룹 4/10명 · 8.17 시작" */
private val ChallengeInvitationPreview.summaryLine: String
    get() {
        // 무제한 방은 분모가 없다
        val people =
            challenge.capacity?.let { "${challenge.participantCount}/${it}명" } ?: "${challenge.participantCount}명 · 정원 무제한"
        val start = challenge.startDate?.let { "${it.replace('-', '.')} 시작" }
        val tier = challenge.minTier?.let { "${it.value} 이상" }
        return listOfNotNull(people, start, tier).joinToString(" · ")
    }

/** 막힌 이유. */
private val JoinBlockReason?.message: String
    get() =
        when (this) {
            JoinBlockReason.FULL -> "정원이 다 찼어요. 자리가 나면 다시 시도할 수 있어요."
            JoinBlockReason.TIER_GATE -> "이 방은 티어 조건이 있어요. 점수를 더 쌓으면 참여할 수 있어요."
            JoinBlockReason.FREE_LIMIT -> "동시에 참여할 수 있는 챌린지 수를 넘었어요. 하나를 정리하고 오세요."
            JoinBlockReason.REJOIN_COOLDOWN -> "아직 다시 들어올 수 없는 기간이에요."
            JoinBlockReason.ALREADY_JOINED -> "이미 참여 중인 챌린지예요."
            JoinBlockReason.CHALLENGE_COMPLETED -> "이미 끝난 챌린지예요."
            JoinBlockReason.PRIVATE_INVITE_ONLY -> "초대 링크가 더 이상 유효하지 않아요."
            JoinBlockReason.BANNED -> "이 챌린지에는 참여할 수 없어요."
            null -> "지금은 참여할 수 없어요."
        }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ChallengeInviteContentPreview() {
    RuleUpTheme {
        ChallengeInviteContent(
            state =
                com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteState.initial.copy(
                    isLoading = false,
                ),
            onIntent = {
            },
        )
    }
}
