package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeRankEntry
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.RankingEntry
import com.ruleup.challenge.domain.entity.RankingPolicy
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.RankingScope
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 랭킹 탭 (Figma 1134:326 · 1134:428).
 *
 * 세그먼트로 비교 단위를 바꾼다 — **멤버**는 같은 방 사람끼리 요청 시 실시간 집계, **방 순위**는 같은
 * 모드의 방끼리 하루 1회 03시 배치 스냅샷이다. 등재 기준도 갱신 주기도 달라 한 목록에 섞지 않는다.
 *
 * 등재 미달은 목록에서 빼지 않고 등수 자리에 "-" 를 둔다 — 빼 버리면 자기가 왜 안 보이는지 알 수 없고,
 * 등수가 있는 사람들의 순번도 어긋난다.
 */
@Composable
internal fun RoomRankingTab(
    state: ChallengeDetailState,
    onSelectScope: (RankingScope) -> Unit,
    onLoadMoreCross: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val isRoomScope = state.rankingScope == RankingScope.ROOM

    // 방 순위만 페이징이 있다. 하단 3개 안에 들어오면 다음 페이지를 미리 받는다.
    val shouldPage by remember(state.crossRanking?.items?.size, isRoomScope) {
        derivedStateOf {
            if (!isRoomScope) return@derivedStateOf false
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: return@derivedStateOf false
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    // 이펙트를 콜백 신원 변화로 재시작시키지 않는다 — 재구성마다 새 람다가 오면 페이징이 헛돈다.
    val loadMore by rememberUpdatedState(onLoadMoreCross)
    LaunchedEffect(listState, state.canLoadMoreCrossRanking) {
        snapshotFlow { shouldPage }.collect { if (it && state.canLoadMoreCrossRanking) loadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
    ) {
        item(key = "segment") {
            RoomSegmentedControl(
                options = RankingScope.entries,
                selected = state.rankingScope,
                label = { it.label },
                onSelect = onSelectScope,
            )
            Spacer(Modifier.height(10.dp))
        }

        when (state.rankingScope) {
            RankingScope.MEMBER -> memberRanking(state)
            RankingScope.ROOM -> roomRanking(state)
        }
    }
}

private fun LazyListScope.memberRanking(state: ChallengeDetailState) {
    val ranking = state.ranking
    when {
        ranking == null && state.isRankingLoading -> item(key = "loading") { RankingLoading() }

        ranking == null -> item(key = "error") { RoomEmptyState(message = "랭킹을 불러오지 못했어요") }

        else -> {
            item(key = "me") {
                MyRankSummary(me = ranking.me)
                Spacer(Modifier.height(10.dp))
            }
            if (ranking.items.isEmpty()) {
                item(key = "empty") { RoomEmptyState(message = "아직 순위에 오른 멤버가 없어요") }
            } else {
                items(ranking.items, key = { "member-${it.user.userId}" }) { entry ->
                    MemberRankRow(entry = entry, isMe = entry.user.userId == state.myUserId)
                }
                item(key = "footnote") {
                    RankingFootnote("인증 ${RankingPolicy.IN_ROOM_MIN_PARTICIPATIONS}회부터 순위에 올라요")
                }
            }
        }
    }
}

private fun LazyListScope.roomRanking(state: ChallengeDetailState) {
    val ranking = state.crossRanking
    when {
        ranking == null && state.isCrossRankingLoading -> item(key = "cross-loading") { RankingLoading() }

        ranking == null -> item(key = "cross-error") { RoomEmptyState(message = "방 순위를 불러오지 못했어요") }

        else -> {
            item(key = "cross-me") {
                MyChallengeRankSummary(ranking = ranking)
                Spacer(Modifier.height(10.dp))
            }
            if (ranking.items.isEmpty()) {
                item(key = "cross-empty") { RoomEmptyState(message = "아직 순위에 오른 방이 없어요") }
            } else {
                items(ranking.items, key = { "room-${it.challengeId}" }) { entry ->
                    ChallengeRankRow(entry = entry, isMine = entry.challengeId == state.challengeId)
                }
                if (state.isCrossRankingLoading) {
                    item(key = "cross-paging") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = RuleUpTheme.colors.brand,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                item(key = "cross-footnote") {
                    // 실시간이 아니라는 사실이 "방금 인증했는데 왜 안 오르지"에 대한 답이다.
                    RankingFootnote(rankingUpdatedLabel(ranking.updatedAt))
                }
            }
        }
    }
}

/** 내 순위 요약 (Figma 1134:360) — 내 순위 · 성공률 · 참여. */
@Composable
private fun MyRankSummary(me: MyRank) {
    Row(
        modifier = Modifier.ruleUpCardSurface(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankStat(
            label = "내 순위",
            value = me.rank?.toString() ?: "-",
            unit = if (me.rank != null) "위" else null,
            modifier = Modifier.weight(1f),
        )
        RoomVerticalDivider()
        RankStat(
            label = "성공률",
            value = me.successRate?.toPercentText() ?: "-",
            unit = if (me.successRate != null) "%" else null,
            modifier = Modifier.weight(1f),
        )
        RoomVerticalDivider()
        RankStat(
            // Figma 의 "연속"은 내려오는 값이 없다. 등재까지 얼마나 남았는지가 이 자리에서 실제로 쓸모 있다.
            label = "참여",
            value = me.participations.toString(),
            unit = "회",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MyChallengeRankSummary(ranking: CrossChallengeRanking) {
    val mine = ranking.myChallenge
    Row(
        modifier = Modifier.ruleUpCardSurface(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankStat(
            label = "우리 방",
            value = mine?.rank?.toString() ?: "-",
            unit = if (mine?.rank != null) "위" else null,
            modifier = Modifier.weight(1f),
        )
        RoomVerticalDivider()
        RankStat(
            label = "방 성공률",
            value = mine?.successRate?.toPercentText() ?: "-",
            unit = if (mine?.successRate != null) "%" else null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RankStat(
    label: String,
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.numberL,
            )
            unit?.let {
                Text(
                    text = it,
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.smallMedium,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun MemberRankRow(
    entry: RankingEntry,
    isMe: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isMe) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.rank?.toString() ?: "-",
            color = if (entry.rank == null) RuleUpTheme.colors.textMuted else RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.bodyBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        RoomAvatar(nickname = entry.user.nickname, size = 28.dp, highlighted = isMe)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "${entry.user.nickname} (나)" else entry.user.nickname,
                color = RuleUpTheme.colors.textPrimary,
                style = if (isMe) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.rank == null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "인증 ${RankingPolicy.IN_ROOM_MIN_PARTICIPATIONS}회 미만",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
        Text(
            text = entry.successRate?.let { "${it.toPercentText()}%" } ?: "-",
            color = if (entry.rank == null) RuleUpTheme.colors.textMuted else RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

@Composable
private fun ChallengeRankRow(
    entry: ChallengeRankEntry,
    isMine: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isMine) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.rank.toString(),
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.bodyBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMine) "${entry.title} (우리 방)" else entry.title,
                color = RuleUpTheme.colors.textPrimary,
                style = if (isMine) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${entry.memberCount}명 · 누적 ${entry.totalCount}회",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(
            text = "${entry.successRate.toPercentText()}%",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

@Composable
private fun RankingLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
    }
}

@Composable
private fun RankingFootnote(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.caption,
        modifier = Modifier.padding(top = 10.dp),
    )
}
