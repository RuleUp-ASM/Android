package com.ruleup.verification.presentation.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.presentation.progress.viewmodel.VerificationProgressIntent
import com.ruleup.verification.presentation.progress.viewmodel.VerificationProgressViewModel
import com.ruleup.verification.presentation.render.isStaleSync
import com.ruleup.verification.presentation.render.parseIsoToEpochMillis
import com.ruleup.verification.presentation.render.todayStatusLabel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 내 챌린지 진행률 일괄(명세 §6.1). 카드: 제목/카테고리/진행률/오늘 상태/남은일.
 * lastSyncedAt 이 오래되면(>2h) "신호 미수신" 경고 배지로 권한 점검을 유도한다.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun VerificationProgressScreen(modifier: Modifier = Modifier) {
    val viewModel = metroViewModel<VerificationProgressViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = remember { Clock.System.now().toEpochMilliseconds() }

    LaunchedEffect(Unit) { viewModel.onIntent(VerificationProgressIntent.Load) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!)
            else ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.challenges, key = { it.challengeId }) { challenge ->
                        ProgressCard(
                            challenge = challenge,
                            stale = isStaleSync(parseIsoToEpochMillis(challenge.lastSyncedAt), now),
                            onClick = { viewModel.onIntent(VerificationProgressIntent.OpenDetail(challenge.challengeId)) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun ProgressCard(
    challenge: ChallengeProgress,
    stale: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(challenge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("진행률 ${challenge.progressRate.toInt()}%")
                Text(todayStatusLabel(challenge.todayStatus))
            }
            Text("남은 ${challenge.remainingDays}일", style = MaterialTheme.typography.bodySmall)
            if (stale) {
                Text("⚠️ 신호 미수신 — 권한을 점검해주세요", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
