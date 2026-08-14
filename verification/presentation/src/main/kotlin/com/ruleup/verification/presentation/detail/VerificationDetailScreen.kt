package com.ruleup.verification.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.rememberSingleClick
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.presentation.detail.viewmodel.VerificationDetailEffect
import com.ruleup.verification.presentation.detail.viewmodel.VerificationDetailIntent
import com.ruleup.verification.presentation.detail.viewmodel.VerificationDetailState
import com.ruleup.verification.presentation.detail.viewmodel.VerificationDetailViewModel
import com.ruleup.verification.presentation.render.ctaLabel
import com.ruleup.verification.presentation.render.failureReasonCopy
import com.ruleup.verification.presentation.render.failureReasonCta
import com.ruleup.verification.presentation.render.overallStatusLabel
import com.ruleup.verification.presentation.render.rememberAppSettingsOpener
import com.ruleup.verification.presentation.render.todayStatusLabel

/**
 * 검증 결과/실패 상세(명세 §6.2·§6.4). 성공·실패를 한 응답으로 렌더하며 PENDING 은 실패로 표시하지 않는다.
 */
@Composable
fun VerificationDetailScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: VerificationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openSettings = rememberAppSettingsOpener()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(challengeId) { viewModel.onIntent(VerificationDetailIntent.Load(challengeId)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                VerificationDetailEffect.OpenPermissionSettings -> openSettings()
                is VerificationDetailEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    VerificationDetailContent(
        state = state,
        onCtaClick = { viewModel.onIntent(VerificationDetailIntent.CtaClicked) },
        onSubmitObjection = { targetDate, content ->
            viewModel.onIntent(VerificationDetailIntent.SubmitObjection(targetDate = targetDate, content = content))
        },
        modifier = modifier,
    )
}

@Composable
private fun VerificationDetailContent(
    state: VerificationDetailState,
    onCtaClick: () -> Unit,
    onSubmitObjection: (targetDate: String, content: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 이의 제기 다이얼로그 대상 실패 일자(열려 있으면 non-null).
    var objectionTargetDate by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error)
            state.detail != null ->
                DetailBody(
                    detail = state.detail,
                    onCtaClick = onCtaClick,
                    onObjectionClick = { targetDate -> objectionTargetDate = targetDate },
                )
        }
    }

    objectionTargetDate?.let { targetDate ->
        ObjectionDialog(
            targetDate = targetDate,
            submitting = state.isSubmittingObjection,
            onSubmit = { content ->
                objectionTargetDate = null
                onSubmitObjection(targetDate, content)
            },
            onDismiss = { objectionTargetDate = null },
        )
    }
}

@Composable
private fun ObjectionDialog(
    targetDate: String,
    submitting: Boolean,
    onSubmit: (content: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이의 제기", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$targetDate 인증 실패에 대해 재검토를 요청해요.")
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("사유") },
                    placeholder = { Text("어떤 점이 잘못됐는지 알려주세요") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(content.trim()) },
                enabled = !submitting && content.isNotBlank(),
            ) {
                Text("제출")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun ColumnScope.DetailBody(
    detail: VerificationDetail,
    onCtaClick: () -> Unit,
    onObjectionClick: (targetDate: String) -> Unit,
) {
    Text(detail.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("${overallStatusLabel(detail.overallStatus)} · 진행률 ${detail.progressRate.toInt()}%")
    Text("성공 ${detail.successDays}/${detail.targetDays}일 · 남은 ${detail.remainingDays}일")

    HorizontalDivider()

    // 오늘 상태(명세 §6.4): PENDING≠실패.
    Text("오늘", fontWeight = FontWeight.Bold)
    Text(todayStatusLabel(detail.today.status))

    when (detail.today.status) {
        TodayStatus.SUCCESS -> {
            detail.today.verifiedAt?.let { Text("인증 시각: $it") }
            detail.today.evidence?.let { e ->
                e.dwellMinutes?.let { Text("체류 ${it}분") }
                e.usageMinutes?.let { Text("사용 ${it}분") }
                e.firstUnlockAt?.let { Text("첫 잠금해제 $it") }
            }
        }

        TodayStatus.FAILED -> {
            detail.today.failureReason?.let { reason ->
                Text(failureReasonCopy(reason), color = MaterialTheme.colorScheme.error)
                ctaLabel(failureReasonCta(reason))?.let { label ->
                    Button(onClick = rememberSingleClick(onClick = onCtaClick), modifier = Modifier.fillMaxWidth()) {
                        Text(label)
                    }
                }
            }
            // 이의 제기: 실패 일자를 특정할 수 있을 때만 노출(가장 최근 실패 로그 기준).
            val failedDate = detail.dailyLogs.lastOrNull { it.status.isFailure }?.date
            if (failedDate != null) {
                OutlinedButton(
                    onClick = { onObjectionClick(failedDate) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("이의 제기 · 재검토 요청")
                }
            }
        }

        TodayStatus.PENDING -> Text("아직 진행 중이에요. 창이 닫히면 확정돼요.")
        TodayStatus.NOT_TARGET -> Text("오늘은 이 챌린지의 대상일이 아니에요.")
    }

    HorizontalDivider()

    Text("방식별 평가", fontWeight = FontWeight.Bold)
    detail.methods.forEach { method ->
        val supported = if (method.supported) "" else " (이 기기 미지원)"
        Text("· ${method.method}$supported")
    }

    if (detail.dailyLogs.isNotEmpty()) {
        HorizontalDivider()
        Text("최근 기록", fontWeight = FontWeight.Bold)
        detail.dailyLogs.forEach { log ->
            Text("${log.date} — ${todayStatusLabel(log.status)}")
        }
    }
}
