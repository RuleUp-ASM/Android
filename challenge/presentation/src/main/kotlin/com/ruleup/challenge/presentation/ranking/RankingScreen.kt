package com.ruleup.challenge.presentation.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.MyRank
import com.ruleup.challenge.domain.entity.RankingEntry
import com.ruleup.challenge.domain.entity.RankingPolicy
import com.ruleup.challenge.presentation.ranking.viewmodel.RankingIntent
import com.ruleup.challenge.presentation.ranking.viewmodel.RankingState
import com.ruleup.challenge.presentation.ranking.viewmodel.RankingViewModel
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import java.util.Locale

// 포디움 순위별 색 (피그마 434:545~557 — #1 Amber, #2 Slate, #3 Orange)
private val PodiumFirst = RuleUpPalette.StatusWarn
private val PodiumSecond = RuleUpPalette.TextFaint

// 3위 주황은 Figma 팔레트 15색에 없다. 화면 디자인에서 온 값이라 남긴다.
private val PodiumThird = Color(0xFFEA580C)

/**
 * 그룹 랭킹 화면 (피그마 434:514). 방 홈의 랭킹 섹션으로 진입한다.
 * 상위 3 포디움 + 내 순위 카드 + 전체 목록. 기간 탭·챌린지 선택은 API 부재로 제외(Phase 2 시즌제).
 */
@Composable
fun RankingScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(challengeId) {
        viewModel.onIntent(RankingIntent.Load(challengeId))
    }

    RankingContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun RankingContent(
    state: RankingState,
    onIntent: (RankingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RankingTopBar(onBack = { onIntent(RankingIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.ranking == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "랭킹을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> RankingBody(ranking = state.ranking!!)
        }
    }
}

@Composable
private fun RankingTopBar(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "그룹 랭킹",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
    }
}

@Composable
private fun RankingBody(ranking: ChallengeRanking) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val top3 = ranking.items.filter { it.rank != null && it.rank in 1..3 }
        if (top3.isNotEmpty()) {
            item { Podium(top3 = top3) }
        }
        item { MyRankCard(me = ranking.me) }
        if (ranking.items.isNotEmpty()) {
            item {
                Text(
                    text = "전체 순위",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.smallBold,
                )
            }
            items(ranking.items, key = { it.user.userId }) { entry ->
                RankingRow(entry = entry)
            }
        } else {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 랭킹이 집계되지 않았어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.body,
                    )
                }
            }
        }
    }
}

/** 상위 3 포디움 — 가운데 1위(가장 높음), 좌 2위, 우 3위 (시안 434:541). */
@Composable
private fun Podium(top3: List<RankingEntry>) {
    val first = top3.find { it.rank == 1 }
    val second = top3.find { it.rank == 2 }
    val third = top3.find { it.rank == 3 }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumColumn(entry = second, color = PodiumSecond, barHeight = 100.dp, modifier = Modifier.weight(1f))
        PodiumColumn(entry = first, color = PodiumFirst, barHeight = 130.dp, modifier = Modifier.weight(1f))
        PodiumColumn(entry = third, color = PodiumThird, barHeight = 80.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumColumn(
    entry: RankingEntry?,
    color: Color,
    barHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (entry != null) {
            Text(text = rankMedal(entry.rank), style = RuleUpTheme.typography.title)
            Text(
                text = entry.user.nickname,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.smallBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = entry.successRate.percentLabel(),
                    color = RuleUpPalette.BgSurface,
                    style = RuleUpTheme.typography.title,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "#${entry.rank}",
                    color = RuleUpPalette.BgSurface,
                    style = RuleUpTheme.typography.captionBold,
                )
            }
        }
    }
}

/**
 * 내 순위 카드 (시안 434:560).
 *
 * 참여 10회 미만이면 등재되지 않아 순위가 없다 — 이때는 "-" 를 보여주고 몇 회 남았는지로 안내한다.
 * 1위면 격차 대신 축하 문구를 쓴다.
 */
@Composable
private fun MyRankCard(me: MyRank) {
    val gap = me.gapToFirst
    val subLabel =
        when {
            !me.ranked ->
                "${RankingPolicy.IN_ROOM_MIN_PARTICIPATIONS}회부터 순위에 올라요 · 지금 ${me.participations}회"

            (me.rank ?: Int.MAX_VALUE) <= 1 || gap == null ->
                "성공률 ${me.successRate.percentLabel()} · 지금 1위를 지키고 있어요"

            else ->
                "성공률 ${me.successRate.percentLabel()} · 1위와 ${gap.percentLabel()} 차이"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(RuleUpPalette.Primary50, RuleUpPalette.Primary50)),
                ).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.brand),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = me.rank?.toString() ?: "-",
                color = RuleUpPalette.BgSurface,
                style = RuleUpTheme.typography.cardTitle,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = me.rank?.let { "내 순위 #$it" } ?: "아직 순위가 없어요",
                color = RuleUpPalette.TextInk,
                style = RuleUpTheme.typography.bodyBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subLabel,
                color = RuleUpPalette.TextSub,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun RankingRow(entry: RankingEntry) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rankMedal(entry.rank),
            style = RuleUpTheme.typography.labelMedium,
            modifier = Modifier.width(34.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.user.nickname,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                // 미등재면 등수 대신 왜 없는지가 정보다.
                text =
                    if (entry.rank == null) {
                        "인증 ${RankingPolicy.IN_ROOM_MIN_PARTICIPATIONS}회 미만"
                    } else {
                        "${entry.successCount}회 성공 · ${entry.participations}회 참여"
                    },
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(
            text = entry.successRate.percentLabel(),
            color = if (entry.rank == null) RuleUpTheme.colors.textMuted else RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.cardTitle,
        )
    }
}

// 미등재(rank null)는 "-" 로 표시한다 — 순위에서 빼면 등수가 어긋난다.
private fun rankMedal(rank: Int?): String =
    when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        null -> "-"
        else -> "$rank"
    }

// 성공률 0~1 → "98%" / "97.5%". 미등재라 값이 없으면 "-".
private fun Double?.percentLabel(): String {
    val percent = (this ?: return "-") * 100
    val text =
        if (percent % 1.0 == 0.0) {
            percent.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", percent)
        }
    return "$text%"
}
