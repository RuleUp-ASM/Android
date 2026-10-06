package com.ruleup.challenge.presentation.mychallenges

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.LeftType
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengeSegment
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesEffect
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesIntent
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesState
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesViewModel
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryIconRes
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpProgressBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot
import java.time.Instant

/** 내 챌린지. */
@Composable
fun MyChallengesScreen(
    modifier: Modifier = Modifier,
    viewModel: MyChallengesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) { viewModel.onIntent(MyChallengesIntent.Load) }
    // 방에서 돌아오면 달성률이 그새 움직였을 수 있다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(MyChallengesIntent.Refresh) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MyChallengesEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    MyChallengesContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun MyChallengesContent(
    state: MyChallengesState,
    onIntent: (MyChallengesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 탭 바를 겹쳐 그리면 탭 바 높이(내비게이션 바 포함)만큼 목록 끝이 가려진다
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding(),
        ) {
            Text(
                text = "챌린지",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.title,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 12.dp),
            )
            SegmentBar(state = state, onIntent = onIntent)

            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                    }

                state.errorMessage != null && state.current.isEmpty() ->
                    Box(Modifier.fillMaxSize().ttiContentDrawn(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.errorMessage,
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }

                state.current.isEmpty() -> EmptyState(segment = state.segment, onIntent = onIntent)

                else -> ChallengeList(state = state, onIntent = onIntent)
            }
        }

        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.CHALLENGE,
            onCreateClick = { onIntent(MyChallengesIntent.CreateChallenge) },
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.HOME -> onIntent(MyChallengesIntent.OpenHomeTab)
                    RuleUpBottomTab.EXPLORE -> onIntent(MyChallengesIntent.OpenExploreTab)
                    RuleUpBottomTab.CHALLENGE -> Unit
                    RuleUpBottomTab.MY -> onIntent(MyChallengesIntent.OpenMyTab)
                }
            },
        )
    }
}

/** 세그먼트. */
@Composable
private fun SegmentBar(
    state: MyChallengesState,
    onIntent: (MyChallengesIntent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentTab(
            label = "진행 중 ${state.inProgress.size}",
            selected = state.segment == MyChallengeSegment.IN_PROGRESS,
            onClick = { onIntent(MyChallengesIntent.SelectSegment(MyChallengeSegment.IN_PROGRESS)) },
            modifier = Modifier.weight(1f),
        )
        SegmentTab(
            label = if (state.finishedPaging.hasNext) "완료 · 이탈" else "완료 · 이탈 ${state.finished.size}",
            selected = state.segment == MyChallengeSegment.FINISHED,
            onClick = { onIntent(MyChallengesIntent.SelectSegment(MyChallengeSegment.FINISHED)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) RuleUpTheme.colors.surface else RuleUpTheme.colors.surfaceVariant)
                .singleClickable(onClick = onClick)
                .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}

@Composable
private fun ChallengeList(
    state: MyChallengesState,
    onIntent: (MyChallengesIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val finished = state.segment == MyChallengeSegment.FINISHED

    // 끝에서 두 번째 항목이 보이면 다음 장을 당긴다
    if (finished && state.finishedPaging.hasNext) {
        val shouldLoadMore by remember(state.finished.size) {
            derivedStateOf {
                val last =
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0
                last >= state.finished.size - 2
            }
        }
        // onIntent 를 이펙트 안에서 직접 잡으면 재구성 때 옛 람다가 남는다.
        val loadMore by rememberUpdatedState { onIntent(MyChallengesIntent.LoadMore) }
        LaunchedEffect(listState) {
            snapshotFlow { shouldLoadMore }.collect { if (it) loadMore() }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().ttiContentDrawn(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.current, key = { it.challengeId }) { challenge ->
            if (finished) {
                FinishedCard(challenge = challenge) { onIntent(MyChallengesIntent.OpenChallenge(challenge.challengeId)) }
            } else {
                InProgressCard(
                    challenge = challenge,
                    progress = state.progress.of(challenge.challengeId),
                    unreadBadge = state.unread.badgeOf(challenge.challengeId),
                ) { onIntent(MyChallengesIntent.OpenChallenge(challenge.challengeId)) }
            }
        }
        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

private fun ProgressSnapshot?.of(challengeId: String): ChallengeProgress? = this?.challenges?.firstOrNull { it.challengeId == challengeId }

/** 진행 중 카드. */
@Composable
private fun InProgressCard(
    challenge: MyChallenge,
    progress: ChallengeProgress?,
    unreadBadge: String?,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(challenge = challenge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = challenge.title,
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.cardTitle,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    dDayLabel(challenge)?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = it,
                            color = RuleUpTheme.colors.brand,
                            style = RuleUpTheme.typography.captionBold,
                        )
                    }
                    unreadBadge?.let {
                        Spacer(Modifier.width(6.dp))
                        UnreadBadge(text = it)
                    }
                }
                Text(
                    text = challenge.compositionLabel,
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
        if (progress != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "달성률",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.caption,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${progress.progressRate.toInt()}%",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.smallBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            RuleUpProgressBar(progress = (progress.progressRate / 100.0).toFloat().coerceIn(0f, 1f))

            // 오늘 인증해야 하는데 신호가 끊긴 상태다.
            if (progress.signalStale(Instant.now())) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "신호가 오지 않고 있어요",
                        color = RuleUpPalette.StatusWarn,
                        style = RuleUpTheme.typography.captionBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "인증 권한을 확인해 주세요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.caption,
                    )
                }
            }
        }
    }
}

/** 이 방의 읽지 않은 알림 수. */
@Composable
private fun UnreadBadge(text: String) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(RuleUpTheme.colors.danger)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = RuleUpTheme.colors.surface,
            style = RuleUpTheme.typography.micro,
        )
    }
}

/** 완료·이탈 카드. */
@Composable
private fun FinishedCard(
    challenge: MyChallenge,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.cardTitle,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = challenge.finishedSubtitle,
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
                challenge.finalSuccessLabel?.let { label ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.captionBold,
                    )
                }
            }
            FinishedBadge(challenge = challenge)
        }
    }
}

@Composable
private fun FinishedBadge(challenge: MyChallenge) {
    val left = challenge.leftType != null
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (left) RuleUpTheme.colors.surfaceVariant else RuleUpTheme.colors.successContainer)
                .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = if (left) "이탈" else "완료",
            color = if (left) RuleUpTheme.colors.textMuted else RuleUpTheme.colors.success,
            style = RuleUpTheme.typography.captionBold,
        )
    }
}

@Composable
private fun CategoryIcon(challenge: MyChallenge) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(categoryAccentColor(challenge.category).copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(categoryIconRes(challenge.category)),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Card(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .singleClickable(onClick = onClick)
                .padding(16.dp),
        content = content,
    )
}

@Composable
private fun EmptyState(
    segment: MyChallengeSegment,
    onIntent: (MyChallengesIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp).ttiContentDrawn(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (segment == MyChallengeSegment.IN_PROGRESS) "아직 참여한 챌린지가 없어요" else "끝난 챌린지가 없어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text =
                if (segment == MyChallengeSegment.IN_PROGRESS) {
                    "탐색에서 마음에 드는 방을 찾아보세요"
                } else {
                    "완주하거나 중간에 나온 방이 여기 쌓여요"
                },
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.small,
        )
        if (segment == MyChallengeSegment.IN_PROGRESS) {
            Spacer(Modifier.height(18.dp))
            RuleUpPrimaryButton(
                text = "챌린지 둘러보기",
                modifier = Modifier.width(160.dp),
                onClick = { onIntent(MyChallengesIntent.OpenExplore) },
            )
        }
    }
}

/** "그룹 8명 · 주 5일" / "솔로 · 주 5일". */
private val MyChallenge.compositionLabel: String
    get() {
        val who = if (mode.isGroup) "그룹 ${participantCount}명" else "솔로"
        return "$who · 주 ${weeklyCount}일"
    }

/** "최종 88%". */
private val MyChallenge.finalSuccessLabel: String?
    get() = successRate?.let { "최종 ${Math.round(it * 100)}%" }

/** 챌린지 기간 표기. */
private val MyChallenge.finishedSubtitle: String
    get() {
        val range = "${monthDay(period.start)} – ${monthDay(period.end)}"
        return if (leftType != null) "$range 중단" else range
    }

/** "2026-06-02" → "6.2". */
private fun monthDay(isoDate: String): String {
    val parts = isoDate.substringBefore('T').split('-')
    if (parts.size != 3) return isoDate
    val month = parts[1].trimStart('0').ifEmpty { "0" }
    val day = parts[2].trimStart('0').ifEmpty { "0" }
    return "$month.$day"
}

/** D-12. */
private fun dDayLabel(challenge: MyChallenge): String? {
    if (challenge.status == ChallengeStatus.UPCOMING) return "시작 전"
    val end = runCatching { java.time.LocalDate.parse(challenge.period.end) }.getOrNull() ?: return null
    val remaining =
        java.time.temporal.ChronoUnit.DAYS
            .between(
                com.ruleup.domain.time.ServiceDate
                    .today(),
                end,
            )
    return when {
        remaining < 0 -> "종료"
        remaining == 0L -> "오늘 종료"
        else -> "D-$remaining"
    }
}

/** 이탈 방식 라벨. */
internal val LeftType.label: String
    get() =
        when (this) {
            LeftType.SELF -> "중도 탈퇴"
            LeftType.KICK_CHEAT -> "부정행위로 강퇴"
            LeftType.KICK_FAIL -> "연속 실패로 강퇴"
            LeftType.KICK_PERMISSION -> "권한 미허용으로 강퇴"
            LeftType.AUTO_TIER -> "티어 미달로 자동 탈퇴"
            LeftType.AUTO_SANCTION -> "계정 제재로 자동 탈퇴"
            LeftType.AUTO_CLOSED -> "챌린지 종료로 자동 탈퇴"
            LeftType.KICK_REPORT, LeftType.KICK_BY_OWNER -> "강퇴"
        }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MyChallengesContentPreview() {
    RuleUpTheme {
        MyChallengesContent(
            state =
                com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesState.initial.copy(
                    isLoading = false,
                ),
            onIntent = {
            },
        )
    }
}
