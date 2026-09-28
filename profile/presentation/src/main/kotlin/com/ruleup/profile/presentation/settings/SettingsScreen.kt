package com.ruleup.profile.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.SocialProvider
import com.ruleup.profile.presentation.settings.viewmodel.SettingsDialog
import com.ruleup.profile.presentation.settings.viewmodel.SettingsEffect
import com.ruleup.profile.presentation.settings.viewmodel.SettingsIntent
import com.ruleup.profile.presentation.settings.viewmodel.SettingsState
import com.ruleup.profile.presentation.settings.viewmodel.SettingsViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.ui.helper.LocalMessageHelper

/** 설정 허브. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) { viewModel.onIntent(SettingsIntent.Load) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    SettingsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "설정", onBack = { onIntent(SettingsIntent.Back) })
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("알림")
            MenuCard {
                MenuRow(
                    label = "알림 설정",
                    onClick = { onIntent(SettingsIntent.OpenNotificationSettings) },
                    note = "푸시 알림 · 종류별 수신",
                )
                MenuDivider()
                MenuRow(
                    label = "알림함",
                    onClick = { onIntent(SettingsIntent.OpenNotificationCenter) },
                    note = "받은 알림은 6개월 동안 남아요",
                )
            }

            SectionLabel("수신 · 차단")
            MenuCard {
                MenuRow(
                    label = "내가 받는 알림 관리",
                    onClick = { onIntent(SettingsIntent.OpenWatching) },
                    note = "다른 사람의 실패 알림 수신 설정",
                )
                MenuDivider()
                MenuRow(
                    label = "신고한 사용자 · 챌린지",
                    onClick = { onIntent(SettingsIntent.OpenBlocks) },
                    note = "차단을 풀면 다시 보여요",
                )
            }

            SectionLabel("고객센터")
            MenuCard {
                MenuRow(
                    label = "문의하기",
                    onClick = { onIntent(SettingsIntent.OpenInquiry) },
                    note = "평일 10:00~18:00 · 영업일 2일 안에 답변드려요",
                )
                MenuDivider()
                MenuRow(
                    label = "내 문의 내역",
                    onClick = { onIntent(SettingsIntent.OpenInquiryHistory) },
                    // 답변은 푸시도 알림함도 쓰지 않는다
                    trailing = if (state.answeredInquiryCount > 0) "새 답변 ${state.answeredInquiryCount}건" else null,
                    highlightTrailing = true,
                )
            }

            SectionLabel("계정")
            MenuCard {
                ConnectedAccountRow(provider = state.provider)
                MenuDivider()
                MenuRow(
                    label = "약관 · 개인정보",
                    onClick = { onIntent(SettingsIntent.OpenAgreements) },
                    trailing = if (state.reconsentCount > 0) "재동의 ${state.reconsentCount}건" else null,
                    highlightTrailing = true,
                )
                MenuDivider()
                MenuRow(
                    label = "제재 이력",
                    onClick = { onIntent(SettingsIntent.OpenSanctions) },
                    trailing = if (state.hasActiveSanction) "진행 중" else null,
                    highlightTrailing = true,
                )
                MenuDivider()
                MenuRow(label = "로그아웃", onClick = { onIntent(SettingsIntent.ConfirmLogout) })
                MenuDivider()
                MenuRow(label = "회원 탈퇴", onClick = { onIntent(SettingsIntent.ConfirmWithdraw) }, danger = true)
            }
        }
    }

    state.dialog?.let { dialog ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
            sheetState = sheetState,
            containerColor = RuleUpTheme.colors.surface,
        ) {
            ConfirmSheet(dialog = dialog, isSubmitting = state.isSubmitting, onIntent = onIntent)
        }
    }
}

/** 확인 시트. */
@Composable
private fun ConfirmSheet(
    dialog: SettingsDialog,
    isSubmitting: Boolean,
    onIntent: (SettingsIntent) -> Unit,
) {
    val withdraw = dialog == SettingsDialog.WITHDRAW
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
    ) {
        Text(
            text = if (withdraw) "정말 탈퇴할까요?" else "로그아웃할까요?",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                if (withdraw) {
                    "참여 중인 챌린지에서 모두 나가게 돼요. 1년 안에 같은 계정으로 로그인하면 기록이 복원돼요."
                } else {
                    "다시 로그인하면 그대로 이어서 할 수 있어요."
                },
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
        Spacer(Modifier.height(20.dp))
        RuleUpPrimaryButton(
            text = if (withdraw) "탈퇴할게요" else "로그아웃",
            enabled = !isSubmitting,
            onClick = { onIntent(if (withdraw) SettingsIntent.Withdraw else SettingsIntent.Logout) },
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .singleClickable(onClick = { onIntent(SettingsIntent.DismissDialog) })
                    .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "닫기",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.captionBold,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
    )
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(color = RuleUpTheme.colors.border)
}

/** 「연결된 계정 · 카카오」. */
@Composable
private fun ConnectedAccountRow(provider: SocialProvider?) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "연결된 계정",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = provider.label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
    }
}

private val SocialProvider?.label: String
    get() =
        when (this) {
            SocialProvider.KAKAO -> "카카오"
            SocialProvider.GOOGLE -> "구글"
            null -> "연결됨"
        }

@Composable
private fun MenuRow(
    label: String,
    onClick: () -> Unit,
    note: String? = null,
    trailing: String? = null,
    highlightTrailing: Boolean = false,
    danger: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (note == null) 56.dp else 64.dp)
                .singleClickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (danger) RuleUpTheme.colors.danger else RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            note?.let {
                Text(text = it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
            }
        }
        trailing?.let {
            Text(
                text = it,
                color = if (highlightTrailing) RuleUpTheme.colors.brand else RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.captionBold,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(
            text = "›",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.section,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun SettingsContentPreview() {
    RuleUpTheme {
        SettingsContent(
            state =
                com.ruleup.profile.presentation.settings.viewmodel.SettingsState.initial
                    .copy(isLoading = false),
            onIntent = { },
        )
    }
}
