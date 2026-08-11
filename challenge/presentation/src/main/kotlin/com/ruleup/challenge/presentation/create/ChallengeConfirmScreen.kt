package com.ruleup.challenge.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.presentation.create.component.ConfirmEditSection
import com.ruleup.challenge.presentation.create.component.ConfirmEditSheet
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.designsystem.R
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 확인 화면 (Figma 1134:604) — 초안을 **요약 한 장**으로 보여주고, 고칠 항목만 바텀시트로 연다.
 *
 * 항목을 전부 펼쳐 놓지 않는다. 초안은 대부분 그대로 쓰이므로 스크롤 한 화면에 요약을 담고,
 * 고칠 사람만 해당 줄을 탭해 들어가게 한다.
 *
 * 잠긴 줄(카테고리)은 회색 처리로 끝내지 않고 **자물쇠와 함께** 보여준다 — 왜 못 고치는지
 * 모른 채 눌러보게 두지 않기 위해서다.
 */
@Composable
fun ChallengeConfirmContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    state: CreateChallengeState = CreateChallengeState.initial,
) {
    val nav = LocalNavigationHelper.current
    var editing by remember { mutableStateOf<ConfirmEditSection?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        ConfirmAppBar(onBack = { nav.navigateToBack() })

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { HeaderCard(state = state) }
            item { SummaryCard(state = state, onEdit = { editing = it }) }
            item {
                Text(
                    text = "만든 뒤에는 이름·설명·정원만 바꿀 수 있어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.background)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        ) {
            RuleUpPrimaryButton(
                text = if (state.isCreating) "만드는 중…" else "이대로 만들기",
                enabled = state.hasDraft && !state.isCreating,
                onClick = { onIntent(CreateChallengeIntent.Create) },
            )
        }
    }

    editing?.let { section ->
        ConfirmEditSheet(
            section = section,
            state = state,
            onIntent = onIntent,
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ConfirmAppBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "뒤로",
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .size(22.dp)
                    .singleClickable(onClick = onBack),
            tint = RuleUpTheme.colors.textPrimary,
        )
        Text(
            text = "확인하고 만들기",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
    }
}

/** 무엇을 만드는지 한눈에. 인증 방식·모드 칩 + 제목 + 설명. */
@Composable
private fun HeaderCard(
    state: CreateChallengeState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderChip(
                text = state.verificationLabel(),
                background = RuleUpTheme.colors.brandSoft,
                textColor = RuleUpTheme.colors.brand,
            )
            HeaderChip(
                text = if (state.isGroup) "그룹" else "솔로",
                background = RuleUpTheme.colors.background,
                textColor = RuleUpTheme.colors.textSecondary,
            )
        }
        Text(
            text = state.title.ifBlank { "제목 없음" },
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.numberM,
        )
        if (state.description.isNotBlank()) {
            Text(
                text = state.description,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HeaderChip(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(24.dp)
                .clip(RuleUpTheme.shapes.small)
                .background(background)
                .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = textColor, style = RuleUpTheme.typography.captionBold)
    }
}

@Composable
private fun SummaryCard(
    state: CreateChallengeState,
    onEdit: (ConfirmEditSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large),
    ) {
        SummaryRow(
            label = "이름·설명",
            value = state.title.ifBlank { "제목 없음" },
            onClick = { onEdit(ConfirmEditSection.TITLE_DESCRIPTION) },
        )
        SummaryRow(
            label = "모드·인원",
            value = state.modeSummary(),
            onClick = { onEdit(ConfirmEditSection.MODE_CAPACITY) },
        )
        SummaryRow(
            label = "인증",
            value = state.verificationSummary(),
            onClick = { onEdit(ConfirmEditSection.VERIFICATION) },
        )
        SummaryRow(
            label = "빈도·기간",
            value = state.frequencyPeriodSummary(),
            onClick = { onEdit(ConfirmEditSection.PERIOD) },
        )
        // 점수 차감·그룹 공개는 서버가 강제하지만 감시자 알림은 고를 수 있다 — 줄 전체를 잠그면
        // 유일하게 고를 수 있는 항목까지 같이 막힌다. 시트 안에서 잠긴 둘과 구분해 보여준다.
        SummaryRow(
            label = "실패하면",
            value = state.penaltySummary(),
            caption = "감시자 알림은 각자 설정한 사람에게만 가요",
            onClick = { onEdit(ConfirmEditSection.PENALTIES) },
        )
        // 카테고리는 확인 화면부터 수정 불가이며 생성 후에도 불변이다.
        SummaryRow(
            label = "카테고리",
            value = state.category?.label ?: "분류 없음",
            locked = true,
            last = true,
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    locked: Boolean = false,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(if (locked) RuleUpTheme.colors.background else RuleUpTheme.colors.surface)
                    .let { base -> if (onClick != null) base.singleClickable(onClick = onClick) else base }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.width(64.dp),
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.smallMedium,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = value,
                    color = if (locked) RuleUpTheme.colors.textMuted else RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyMedium,
                )
                caption?.let {
                    Text(it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.micro)
                }
            }
            Icon(
                painter =
                    painterResource(
                        if (locked) R.drawable.ic_lock else R.drawable.ic_chevron_right,
                    ),
                contentDescription = if (locked) "수정 불가" else "수정",
                modifier = Modifier.size(if (locked) 12.dp else 14.dp),
                tint = RuleUpTheme.colors.textMuted,
            )
        }
        if (!last) HorizontalDivider(thickness = 1.dp, color = RuleUpTheme.colors.background)
    }
}

// ---------- 요약 문구 ----------

/** 헤더 칩에 쓰는 짧은 인증 라벨. */
private fun CreateChallengeState.verificationLabel(): String =
    when (verification?.method) {
        VerificationMethod.GPS_PRESENCE -> "GPS 인증"
        VerificationMethod.GPS_AVOID -> "장소 회피"
        VerificationMethod.SCREEN_TIME_MAX -> "사용 시간 제한"
        VerificationMethod.SCREEN_TIME_MIN -> "사용 시간 목표"
        VerificationMethod.HEALTH -> "건강 데이터"
        VerificationMethod.WAKE -> "기상 인증"
        VerificationMethod.SLEEP -> "취침 인증"
        else -> "직접 체크"
    }

private fun CreateChallengeState.modeSummary(): String =
    if (mode == ChallengeMode.GROUP) {
        val visibilityLabel = if (visibility == ChallengeVisibility.PRIVATE) "비공개" else "그룹"
        val tierLabel = minTier?.let { "티어 ${it.label()} 이상" } ?: "티어 제한 없음"
        "$visibilityLabel · 정원 ${capacity}명 · $tierLabel"
    } else {
        val ranking = if (rankingVisible == false) "랭킹 비공개" else "랭킹 공개"
        "솔로 · $ranking"
    }

/**
 * 인증 요약. 목표값이 있으면 그 값이 곧 인증 조건이므로 앞세운다 — "오전 6:00 · 기상 인증" 처럼.
 * 서버가 표시 문구([detail])를 주면 그게 가장 정확하므로 우선한다.
 */
private fun CreateChallengeState.verificationSummary(): String {
    verification?.detail?.takeIf { it.isNotBlank() }?.let { return it }
    val paramPart = params.joinToString(" · ") { it.summary() }
    return listOf(paramPart, verificationLabel()).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * 빈도·기간. 빈도는 초안이 준 값 그대로다 — 생성 요청 계약에 `weeklyCount` 가 없어 고칠 수단이 없다.
 * 기간만 시트에서 바꾼다.
 */
private fun CreateChallengeState.frequencyPeriodSummary(): String {
    val frequency = if (weeklyCount >= 7) "매일" else "주 ${weeklyCount}회"
    if (period.start.isBlank()) return "$frequency · 기간 미정"
    val days = ChallengeDates.daysBetween(period.start, period.end) + 1
    return "$frequency · ${ChallengeDates.formatMonthDay(period.start)}부터 ${ChallengeDates.durationLabel(days)}"
}

private fun CreateChallengeState.penaltySummary(): String {
    val parts =
        buildList {
            if (penalties.groupShare) add("그룹에 공개")
            if (penalties.score) add("점수 차감")
            if (penalties.watcher) add("감시자 알림")
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "패널티 없음"
}

/** 목표값 한 줄 표기. 단위는 서버가 준 값을 그대로 붙인다(루틴별 분기 하드코딩 금지). */
internal fun ParamSpec.summary(): String =
    when (kind) {
        ParamKind.TIME -> value
        ParamKind.NUMBER -> listOfNotNull(value.takeIf { it.isNotBlank() }, unit).joinToString("")
    }

internal fun Tier.label(): String =
    when (this) {
        Tier.BRONZE -> "브론즈"
        Tier.SILVER -> "실버"
        Tier.GOLD -> "골드"
        Tier.DIAMOND -> "다이아"
        Tier.RUBY -> "루비"
    }
