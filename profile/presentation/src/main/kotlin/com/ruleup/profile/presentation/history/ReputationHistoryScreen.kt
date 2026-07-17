package com.ruleup.profile.presentation.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.profile.domain.entity.MilestoneType
import com.ruleup.profile.domain.entity.ReputationHistory
import com.ruleup.profile.domain.entity.ReputationMilestone
import com.ruleup.profile.presentation.common.MyTopBar
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.common.trimLabel
import com.ruleup.profile.presentation.history.viewmodel.ReputationHistoryIntent
import com.ruleup.profile.presentation.history.viewmodel.ReputationHistoryViewModel
import com.ruleup.ui.theme.RuleUpPalette
import com.ruleup.ui.theme.RuleUpTheme

// 히어로 그라데이션 (피그마 435:395 — rose → amber)
private val PeakGradient = listOf(Color(0xFFF43F5E), Color(0xFFF59E0B))

/** 평판 히스토리 (피그마 435:395). 역대 최고 온도 + 마일스톤 피드. */
@Composable
fun ReputationHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: ReputationHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(ReputationHistoryIntent.Load)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        MyTopBar(title = "평판 히스토리", onBack = { viewModel.onIntent(ReputationHistoryIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.history == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "히스토리를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        fontSize = 14.sp,
                    )
                }

            else -> HistoryBody(history = state.history!!)
        }
    }
}

@Composable
private fun HistoryBody(history: ReputationHistory) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PeakCard(history = history) }
        item {
            Text(
                text = "마일스톤",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (history.milestones.isEmpty()) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "아직 마일스톤이 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            items(history.milestones, key = { it.type.name + it.achievedAt + it.label }) { milestone ->
                MilestoneRow(milestone = milestone)
            }
        }
    }
}

@Composable
private fun PeakCard(history: ReputationHistory) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(PeakGradient))
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "역대 최고 매너 온도",
            color = RuleUpPalette.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${history.peakTemperature.trimLabel()}℃",
                color = RuleUpPalette.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
            if (history.peakAchievedAt.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = dateDotLabel(history.peakAchievedAt),
                    color = RuleUpPalette.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun MilestoneRow(milestone: ReputationMilestone) {
    val (emoji, badgeColor) =
        when (milestone.type) {
            MilestoneType.TIER_REACHED -> "🏆" to RuleUpPalette.Amber500
            MilestoneType.STREAK -> "🎯" to RuleUpTheme.colors.brand
            MilestoneType.FIRST_COMPLETION -> "📗" to RuleUpTheme.colors.success
            MilestoneType.SIGNUP -> "🌱" to RuleUpPalette.Violet500
            MilestoneType.ETC -> "⭐" to RuleUpTheme.colors.textMuted
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = milestone.label,
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateDotLabel(milestone.achievedAt),
                color = RuleUpTheme.colors.textMuted,
                fontSize = 11.sp,
            )
        }
    }
}
