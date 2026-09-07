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
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot

/**
 * 내 챌린지 (Figma 1134:1205 · 1162:2 · 빈 상태 1134:2085). 하단 「챌린지」 탭의 루트 화면.
 *
 * Figma 의 완료 카드에 있는 「최종 88%」는 그리지 않는다 — `GET /challenges` 응답에 최종 성공률이
 * 없다(BE 확인 요청). 진행 중 카드의 달성률은 인증 진행률에서 오므로 그 조회가 실패하면 비운다.
 */
@Composable
fun MyChallengesScreen(
    modifier: Modifier = Modifier,
    viewModel: MyChallengesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun MyChallengesContent(
    state: MyChallengesState,
    onIntent: (MyChallengesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.HOME -> onIntent(MyChallengesIntent.OpenHomeTab)
                    RuleUpBottomTab.EXPLORE -> onIntent(MyChallengesIntent.OpenExploreTab)
                    RuleUpBottomTab.CHALLENGE -> Unit
                    RuleUpBottomTab.MY -> onIntent(MyChallengesIntent.OpenMyTab)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 세그먼트 (Figma 1134:1220).
 *
 * 완료·이탈 개수는 **아직 다 못 받았으면 붙이지 않는다** — 받은 만큼만 세어 붙이면 뒤 페이지를
 * 불러올 때마다 숫자가 늘어 사용자가 방이 새로 생긴 줄 안다.
 */
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

    // 끝에서 두 번째 항목이 보이면 다음 장을 당긴다 — 바닥에 닿고 나서 부르면 빈 화면이 한 번 보인다.
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.current, key = { it.challengeId }) { challenge ->
            if (finished) {
                FinishedCard(challenge = challenge) { onIntent(MyChallengesIntent.OpenChallenge(challenge.challengeId)) }
            } else {
                InProgressCard(
                    challenge = challenge,
                    progress = state.progress.of(challenge.challengeId),
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

/** 진행 중 카드 (Figma 1158:2). 달성률·D-day 는 진행률 응답이 있어야 그린다. */
@Composable
private fun InProgressCard(
    challenge: MyChallenge,
    progress: ChallengeProgress?,
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
                    dDayLabel(challenge, progress)?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = it,
                            color = RuleUpTheme.colors.brand,
                            style = RuleUpTheme.typography.captionBold,
                        )
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
        }
    }
}

/** 완료·이탈 카드 (Figma 1162:2). 기간과 끝난 방식만 말한다. */
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
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

/** "그룹 8명 · 주 5일" / "솔로 · 주 5일" (Figma 1158:10). */
private val MyChallenge.compositionLabel: String
    get() {
        val who = if (mode.isGroup) "그룹 ${participantCount}명" else "솔로"
        return "$who · 주 ${weeklyCount}일"
    }

/** "6.2 – 7.13" / 이탈이면 "5.1 – 5.20 중단" (Figma 1162:2). */
private val MyChallenge.finishedSubtitle: String
    get() {
        val range = "${monthDay(period.start)} – ${monthDay(period.end)}"
        return if (leftType != null) "$range 중단" else range
    }

/**
 * "2026-06-02" → "6.2".
 *
 * 파싱하지 않고 자른다 — 이 자리에 필요한 건 날짜 계산이 아니라 표기이고, 서버가 형식을 바꾸면
 * 조용히 틀린 날짜를 만드는 것보다 원문이 그대로 보이는 편이 낫다.
 */
private fun monthDay(isoDate: String): String {
    val parts = isoDate.substringBefore('T').split('-')
    if (parts.size != 3) return isoDate
    val month = parts[1].trimStart('0').ifEmpty { "0" }
    val day = parts[2].trimStart('0').ifEmpty { "0" }
    return "$month.$day"
}

/**
 * D-12. 남은 일수는 진행률 응답에서만 온다 — 목록 응답에 없고, 종료일로 클라가 계산하면
 * 서버의 판정 경계(KST 자정)와 하루 어긋난다.
 *
 * 시작 전 방은 남은 일수 대신 기간이 아직 시작하지 않았다는 사실이 더 중요해 D-day 를 붙이지 않는다.
 */
private fun dDayLabel(
    challenge: MyChallenge,
    progress: ChallengeProgress?,
): String? {
    if (challenge.status == ChallengeStatus.UPCOMING) return "시작 전"
    val remaining = progress?.remainingDays ?: return null
    return if (remaining <= 0) "오늘 종료" else "D-$remaining"
}

/** 이탈 방식 라벨. 지금은 카드에 쓰지 않지만 상세 진입 전 뱃지 문구가 필요해지면 여기서 쓴다. */
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
