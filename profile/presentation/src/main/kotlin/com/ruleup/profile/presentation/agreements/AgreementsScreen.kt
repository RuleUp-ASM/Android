package com.ruleup.profile.presentation.agreements

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.presentation.agreements.viewmodel.AgreementsEffect
import com.ruleup.profile.presentation.agreements.viewmodel.AgreementsIntent
import com.ruleup.profile.presentation.agreements.viewmodel.AgreementsState
import com.ruleup.profile.presentation.agreements.viewmodel.AgreementsViewModel
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.ui.helper.LocalMessageHelper

/**
 * 약관 · 개인정보 동의 관리 (설정 허브 → 약관).
 *
 * 필수 3종은 **토글을 두지 않는다** — 철회하려면 탈퇴해야 하므로(서버 400
 * `AGREEMENT_REVOKE_FORBIDDEN`), 눌러 놓고 거절당하는 스위치를 만들지 않는다.
 */
@Composable
fun AgreementsScreen(
    modifier: Modifier = Modifier,
    viewModel: AgreementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) { viewModel.onIntent(AgreementsIntent.Load) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AgreementsEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    AgreementsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun AgreementsContent(
    state: AgreementsState,
    onIntent: (AgreementsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "약관 · 개인정보", onBack = { onIntent(AgreementsIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.status == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "동의 상태를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> AgreementsBody(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun AgreementsBody(
    state: AgreementsState,
    onIntent: (AgreementsIntent) -> Unit,
) {
    val status = state.status ?: return
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.reconsentRequired.isNotEmpty()) {
            ReconsentBanner(
                count = state.reconsentRequired.size,
                isSubmitting = state.isReconsenting,
                onReconsent = { onIntent(AgreementsIntent.Reconsent) },
            )
        }

        SectionLabel("필수 약관")
        AgreementCard {
            val required = status.agreements.filter { it.required }
            required.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
                RequiredRow(item = item)
            }
        }

        SectionLabel("선택 동의")
        AgreementCard {
            val optional = status.agreements.filter { !it.required }
            optional.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
                OptionalRow(
                    item = item,
                    enabled = state.submitting == null,
                    onToggle = { onIntent(AgreementsIntent.Toggle(item.type, it)) },
                )
            }
        }

        Text(
            text = "필수 약관은 철회할 수 없어요. 철회하려면 회원 탈퇴가 필요해요.",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
    }
}

/** 약관이 개정됐을 때. 어느 항목인지보다 **다시 동의해야 한다**는 사실이 먼저다. */
@Composable
private fun ReconsentBanner(
    count: Int,
    isSubmitting: Boolean,
    onReconsent: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.brandSoft)
                .padding(16.dp),
    ) {
        Text(
            text = "다시 동의가 필요한 약관이 ${count}건 있어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "약관이 개정됐어요. 계속 이용하려면 새 버전에 동의해 주세요.",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
        Spacer(Modifier.height(14.dp))
        RuleUpPrimaryButton(text = "다시 동의하기", enabled = !isSubmitting, onClick = onReconsent)
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
private fun AgreementCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun RequiredRow(item: AgreementState) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.type.label,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(
                text = item.agreedLabel,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(text = "필수", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.captionBold)
    }
}

@Composable
private fun OptionalRow(
    item: AgreementState,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.type.label,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(
                text = item.agreedLabel,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Switch(
            checked = item.agreed,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = RuleUpTheme.colors.brand),
        )
    }
}

/** "2026.06.01 동의" / "동의 안 함" / "받은 적 없음" — 셋은 서로 다른 사실이다. */
private val AgreementState.agreedLabel: String
    get() {
        val at = agreedAt
        return when {
            agreed && at != null -> "${dateDotLabel(at)} 동의"
            agreed -> "동의함"
            everAgreed -> "동의 안 함"
            else -> "아직 받지 않았어요"
        }
    }

private val AgreementType.label: String
    get() =
        when (this) {
            AgreementType.TERMS_OF_SERVICE -> "서비스 이용약관"
            AgreementType.PRIVACY_POLICY -> "개인정보 처리방침"
            AgreementType.LOCATION_SERVICE -> "위치기반 서비스 약관"
            AgreementType.MARKETING -> "마케팅 정보 수신"
            AgreementType.EVENT -> "이벤트 알림 수신"
            AgreementType.NIGHT_PUSH -> "야간 알림 수신"
            AgreementType.LOCATION_INFO -> "위치정보 수집 · 이용"
            AgreementType.HEALTH_INFO -> "건강정보 수집 · 이용"
        }
