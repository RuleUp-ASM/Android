package com.ruleup.profile.presentation.invite

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.FriendInvitee
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.invite.viewmodel.FriendInviteEffect
import com.ruleup.profile.presentation.invite.viewmodel.FriendInviteIntent
import com.ruleup.profile.presentation.invite.viewmodel.FriendInviteState
import com.ruleup.profile.presentation.invite.viewmodel.FriendInviteViewModel
import com.ruleup.ui.helper.LocalMessageHelper

/**
 * 친구 초대 (피그마 435:332). 초대 코드/링크(카카오톡·복사·QR — 사용자 본인 발신) + 초대 현황.
 * 딥링크 앱 라우팅은 초대 경로 확정 후 별도 — 서버가 준 URL 을 그대로 공유만 한다 (#111).
 */
@Composable
fun FriendInviteScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendInviteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) {
        viewModel.onIntent(FriendInviteIntent.Load)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FriendInviteEffect.LaunchKakaoShare -> {
                    val shared =
                        FriendInviteSharer.share(
                            context = context,
                            inviteUrl = effect.inviteUrl,
                            inviteCode = effect.inviteCode,
                        )
                    if (!shared) messageHelper.showToast("카카오톡 공유를 열지 못했어요")
                }

                is FriendInviteEffect.CopyToClipboard -> {
                    clipboard.setText(AnnotatedString(effect.inviteUrl))
                    messageHelper.showToast("초대 링크를 복사했어요")
                }

                is FriendInviteEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    FriendInviteContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — 공유·복사는 Context·클립보드가 필요해 바깥이 맡는다. */
@Composable
internal fun FriendInviteContent(
    state: FriendInviteState,
    onIntent: (FriendInviteIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "친구 초대", onBack = { onIntent(FriendInviteIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.invitation == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "초대 정보를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else ->
                InviteBody(
                    invitation = state.invitation!!,
                    onShareKakao = { onIntent(FriendInviteIntent.ShareKakao) },
                    onCopyLink = { onIntent(FriendInviteIntent.CopyLink) },
                )
        }
    }
}

@Composable
private fun InviteBody(
    invitation: FriendInvitation,
    onShareKakao: () -> Unit,
    onCopyLink: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- 초대 코드 + QR ----------
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(RuleUpPalette.Primary50, RuleUpPalette.Primary50)),
                    ).padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "내 초대 코드",
                color = RuleUpPalette.TextSub,
                style = RuleUpTheme.typography.smallBold,
            )
            Text(
                text = invitation.inviteCode,
                color = RuleUpPalette.TextInk,
                style = RuleUpTheme.typography.numberL,
                // 코드를 한 글자씩 끊어 읽으라고 벌린 값이라 스타일이 정할 값이 아니다.
                letterSpacing = 6.sp,
            )
            val qr = rememberQrBitmap(content = invitation.inviteUrl)
            if (qr != null) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(RuleUpPalette.BgSurface)
                            .padding(10.dp),
                ) {
                    Image(
                        bitmap = qr,
                        contentDescription = "초대 링크 QR",
                        modifier = Modifier.size(150.dp),
                    )
                }
            }
            if (invitation.rewardDescription.isNotBlank()) {
                Text(
                    text = invitation.rewardDescription,
                    color = RuleUpPalette.TextSub,
                    style = RuleUpTheme.typography.small,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }

        // ---------- 공유 버튼 ----------
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShareButton(
                label = "💬 카카오톡",
                background = RuleUpPalette.Kakao,
                labelColor = RuleUpPalette.KakaoLabel,
                onClick = onShareKakao,
                modifier = Modifier.weight(1f),
            )
            ShareButton(
                label = "🔗 링크 복사",
                background = RuleUpTheme.colors.brandSoft,
                labelColor = RuleUpTheme.colors.brand,
                onClick = onCopyLink,
                modifier = Modifier.weight(1f),
            )
        }

        // ---------- 초대 현황 ----------
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "초대 현황",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallBold,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RuleUpTheme.colors.surface)
                        .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp)),
            ) {
                if (invitation.invitees.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "아직 초대로 가입한 친구가 없어요",
                            color = RuleUpTheme.colors.textMuted,
                            style = RuleUpTheme.typography.small,
                        )
                    }
                } else {
                    invitation.invitees.forEachIndexed { index, invitee ->
                        if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
                        InviteeRow(invitee = invitee)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareButton(
    label: String,
    background: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

@Composable
private fun InviteeRow(invitee: FriendInvitee) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🎉", style = RuleUpTheme.typography.labelMedium)
        Spacer(Modifier.width(10.dp))
        Text(
            text = invitee.nickname,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${dateDotLabel(invitee.occurredAt)} 가입",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}
