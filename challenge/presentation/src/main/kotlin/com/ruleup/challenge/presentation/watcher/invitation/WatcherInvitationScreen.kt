package com.ruleup.challenge.presentation.watcher.invitation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.WatcherInvitationInfo
import com.ruleup.challenge.domain.entity.WatcherInvitationState
import com.ruleup.challenge.presentation.watcher.invitation.viewmodel.WatcherInvitationIntent
import com.ruleup.challenge.presentation.watcher.invitation.viewmodel.WatcherInvitationViewModel
import com.ruleup.ui.R
import com.ruleup.ui.component.PrimaryGradientButton
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme

/**
 * 감시자 초대 수락 화면. 카카오톡 초대 카드 링크(딥링크)로 token 과 함께 진입한다.
 * 수락 = 수신동의(무엇에 동의하는지 명시). 토큰 상태별(만료/이미 수락/차단) 안내로 분기한다.
 */
@Composable
fun WatcherInvitationScreen(
    token: String,
    modifier: Modifier = Modifier,
    viewModel: WatcherInvitationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(token) { viewModel.onIntent(WatcherInvitationIntent.Load(token)) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Header(onBack = { viewModel.onIntent(WatcherInvitationIntent.Back) })
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val info = state.info
            when {
                state.isLoading -> CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                state.errorMessage != null && info == null ->
                    Notice(
                        emoji = "⚠️",
                        title = "초대를 확인할 수 없어요",
                        description = state.errorMessage ?: "",
                    )

                info == null -> Unit
                state.accepted ->
                    AcceptedContent(
                        info = info,
                        onOpenChallenge = { viewModel.onIntent(WatcherInvitationIntent.OpenChallenge) },
                    )

                else ->
                    InvitationContent(
                        info = info,
                        isAccepting = state.isAccepting,
                        errorMessage = state.errorMessage,
                        onAccept = { viewModel.onIntent(WatcherInvitationIntent.Accept) },
                    )
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "뒤로",
            tint = RuleUpTheme.colors.textPrimary,
            modifier =
                Modifier
                    .size(24.dp)
                    .singleClickable(onClick = onBack),
        )
        Text(
            text = "감시자 초대",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun InvitationContent(
    info: WatcherInvitationInfo,
    isAccepting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
) {
    when (info.state) {
        WatcherInvitationState.PENDING ->
            PendingContent(
                info = info,
                isAccepting = isAccepting,
                errorMessage = errorMessage,
                onAccept = onAccept,
            )

        WatcherInvitationState.EXPIRED ->
            Notice(
                emoji = "⏰",
                title = "초대가 만료됐어요",
                description = "${info.ownerNickname}님에게 다시 초대를 요청해 주세요",
            )

        WatcherInvitationState.ALREADY_ACCEPTED ->
            Notice(
                emoji = "✅",
                title = "이미 수락한 초대예요",
                description = "${info.ownerNickname}님이 실패하면 알림으로 알려드릴게요",
            )

        WatcherInvitationState.BLOCKED ->
            Notice(
                emoji = "🚫",
                title = "지금은 이 초대를 받을 수 없어요",
                description = "수신거부 후 30일이 지나면 다시 받을 수 있어요",
            )
    }
}

@Composable
private fun PendingContent(
    info: WatcherInvitationInfo,
    isAccepting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WatchBadge()
        Text(
            text = "${info.ownerNickname}님이 당신을\n루틴 감시자로 초대했어요",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        ChallengeTitleChip(title = info.challengeTitle)
        // 수락 = 수신동의. 무엇에 동의하는지 투명하게 명시한다(스펙 메시지 ① 메모).
        Text(
            text =
                "${info.ownerNickname}님이 약속을 지키는지 지켜봐 주세요.\n" +
                    "수락하면 루틴 인증에 실패할 때 알림을 받아요.",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = RuleUpTheme.colors.danger,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(10.dp))
        PrimaryGradientButton(
            text = if (isAccepting) "수락하는 중..." else "수락하기",
            onClick = onAccept,
        )
    }
}

@Composable
private fun AcceptedContent(
    info: WatcherInvitationInfo,
    onOpenChallenge: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WatchBadge()
        Text(
            text = "감시자가 됐어요",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${info.ownerNickname}님이 [${info.challengeTitle}] 인증에\n실패하면 알림으로 알려드릴게요",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        PrimaryGradientButton(
            text = "챌린지 보러 가기",
            onClick = onOpenChallenge,
        )
    }
}

@Composable
private fun Notice(
    emoji: String,
    title: String,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = emoji, fontSize = 40.sp)
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WatchBadge() {
    Box(
        modifier =
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RuleUpTheme.colors.brandSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "👀", fontSize = 32.sp)
    }
}

@Composable
private fun ChallengeTitleChip(title: String) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.brandSoft)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = title,
            color = RuleUpTheme.colors.brandStrong,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
