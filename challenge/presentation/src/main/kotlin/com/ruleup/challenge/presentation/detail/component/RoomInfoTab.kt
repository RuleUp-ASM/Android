package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.challenge.presentation.common.capacityLabel
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.failureText

/** 정보 탭 */
@Composable
internal fun RoomInfoTab(
    detail: ChallengeDetail,
    room: ChallengeRoom,
    today: TodayResult?,
    modifier: Modifier = Modifier,
    onRegisterApps: (() -> Unit)? = null,
    onRegisterAnchor: (() -> Unit)? = null,
    onSubmitAppeal: ((verificationId: String, reason: String) -> Unit)? = null,
    // 수동 방일 때만 넘어온다
    onOpenManualCheck: (() -> Unit)? = null,
    onOpenPermissionRepair: (() -> Unit)? = null,
    isSubmittingAppeal: Boolean = false,
    appealImageUrl: String? = null,
    isUploadingAppealImage: Boolean = false,
    appealReasonError: String? = null,
    onPickAppealImage: () -> Unit = {},
    onDismissAppeal: () -> Unit = {},
    extraSections: @Composable () -> Unit = {},
) {
    // 이의 입력 다이얼로그 열림 여부.
    var appealOpen by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TodayVerificationCard(
            roomStatus = room.myTodayStatus,
            today = today,
            onOpenManualCheck = onOpenManualCheck,
            onRegisterAnchor = onRegisterAnchor,
            onOpenPermissionRepair = onOpenPermissionRepair,
            // 이의는 서버가 낼 수 있다고 한 건(appeal.eligible)에만, 대상 인증 건 ID 를 알 때만 낸다.
            onAppealClick =
                { appealOpen = true }
                    .takeIf { onSubmitAppeal != null && today?.appeal?.eligible == true && today.verificationId != null },
        )

        MySetupCard(
            onRegisterApps = onRegisterApps,
            onRegisterAnchor = onRegisterAnchor,
        )

        VerificationRuleCard(detail = detail)

        ProgressInfoCard(detail = detail, room = room, today = today)

        if (room.topRanking.isNotEmpty()) {
            RuleUpCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("상위 랭킹", style = RuleUpTheme.typography.cardTitle)
                    room.topRanking.take(3).forEach { ranker ->
                        Text(
                            "${ranker.rank}위 · ${ranker.nickname} · ${(ranker.successRate * 100).toInt()}%",
                            style = RuleUpTheme.typography.small,
                        )
                    }
                }
            }
        }
        extraSections()
    }

    // 낼 대상(verificationId)이 없으면 시트를 열지 않는다
    val appealTarget = today?.toAppealTarget()
    if (appealOpen && onSubmitAppeal != null && appealTarget != null) {
        AppealSheet(
            target = appealTarget,
            submitting = isSubmittingAppeal,
            imageUrl = appealImageUrl,
            uploadingImage = isUploadingAppealImage,
            reasonError = appealReasonError,
            onPickImage = onPickAppealImage,
            onSubmit = { reason ->
                appealOpen = false
                onSubmitAppeal(appealTarget.verificationId, reason)
            },
            onDismiss = {
                appealOpen = false
                onDismissAppeal()
            },
        )
    }
}

/** 오늘 내 인증. */
@Composable
internal fun TodayVerificationCard(
    roomStatus: TodayVerificationStatus?,
    today: TodayResult?,
    onAppealClick: (() -> Unit)?,
    onOpenManualCheck: (() -> Unit)? = null,
    onRegisterAnchor: (() -> Unit)? = null,
    onOpenPermissionRepair: (() -> Unit)? = null,
) {
    val status = today?.status ?: roomStatus?.toResultStatus() ?: return
    val colors = RuleUpTheme.colors
    val label: String
    val color: Color
    when (status) {
        TodayResultStatus.DONE -> {
            label = "인증 완료"
            color = colors.success
        }
        // 인증 창이 아직 열려 있다.
        TodayResultStatus.IN_PROGRESS -> {
            label = "인증 진행 중"
            color = colors.brand
        }
        // 이대로면 실패지만 확정 전이다.
        TodayResultStatus.FAIL_EXPECTED -> {
            label = "실패 예정"
            color = RuleUpPalette.StatusWarn
        }
        TodayResultStatus.FAILED -> {
            label = "인증 실패"
            color = colors.danger
        }
        TodayResultStatus.NOT_TARGET -> {
            label = "인증 불필요"
            color = colors.textMuted
        }
    }
    // 이의를 낼 수 있는 건만 카드 테두리로 알린다.
    val appealAction = onAppealClick
    val appealable = appealAction != null
    Column(
        modifier =
            Modifier
                .ruleUpCardSurface()
                .then(
                    if (appealable) {
                        Modifier.border(1.dp, colors.dangerContainer, RuleUpTheme.shapes.card)
                    } else {
                        Modifier
                    },
                ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "오늘 내 인증",
                color = colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
            )
            Spacer(Modifier.weight(1f))
            today?.todayDetail()?.let {
                Text(
                    text = it,
                    color = colors.textSecondary,
                    style = RuleUpTheme.typography.caption,
                )
                Spacer(Modifier.width(8.dp))
            }
            // 이의 가능 구간에는 실패 배지를 달지 않는다
            if (status == TodayResultStatus.FAILED && !appealable) {
                StatusChip(text = "실패 확정", tone = StatusChipTone.Danger)
            } else {
                Text(
                    text = label,
                    color = color,
                    style = RuleUpTheme.typography.bodyBold,
                )
            }
        }

        todayNote(status, today)?.let {
            Text(
                text = it,
                color = colors.textSecondary,
                style = RuleUpTheme.typography.caption,
            )
        }

        // 사유를 말했으면 그걸 푸는 곳으로 보낸다.
        today?.failureReason?.fixAction(onRegisterAnchor, onOpenPermissionRepair, onOpenManualCheck)?.let { fix ->
            Text(
                text = fix.label,
                color = colors.brand,
                style = RuleUpTheme.typography.smallBold,
                modifier = Modifier.singleClickable(onClick = fix.onClick),
            )
        }

        appealAction?.let { action ->
            RuleUpPrimaryButton(
                text = today?.appealButtonText() ?: "이의 제기",
                onClick = action,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 수동 방의 체크는 전용 화면에서 한다
        if (onOpenManualCheck != null) {
            when (status) {
                TodayResultStatus.DONE ->
                    Text(
                        text = "인증 수정",
                        color = colors.textMuted,
                        style = RuleUpTheme.typography.caption,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .singleClickable(onClick = onOpenManualCheck)
                                .padding(vertical = 8.dp),
                    )

                TodayResultStatus.IN_PROGRESS ->
                    RuleUpPrimaryButton(
                        text = "오늘 인증 체크",
                        onClick = onOpenManualCheck,
                        modifier = Modifier.fillMaxWidth(),
                    )

                else -> Unit
            }
        }
    }
}

/** 실패 사유를 풀 수 있는 화면과 그리로 가는 문구. */
internal class FixAction(
    val label: String,
    val onClick: () -> Unit,
)

/** 사유별 조치 경로. */
internal fun FailureReason.fixAction(
    onRegisterAnchor: (() -> Unit)?,
    onOpenPermissionRepair: (() -> Unit)?,
    onOpenManualCheck: (() -> Unit)?,
): FixAction? =
    when (this) {
        FailureReason.GEOFENCE_NOT_CONFIGURED ->
            onRegisterAnchor?.let { FixAction("인증 장소 등록하기", it) }

        FailureReason.PERMISSION_MISSING ->
            onOpenPermissionRepair?.let { FixAction("권한 다시 연결하기", it) }

        FailureReason.MANUAL_NOT_SUBMITTED ->
            onOpenManualCheck?.let { FixAction("오늘 체크하기", it) }

        else -> null
    }

/** 상태 아래에 붙는 안내 문구. */
internal fun todayNote(
    status: TodayResultStatus,
    today: TodayResult?,
): String? =
    when (status) {
        TodayResultStatus.NOT_TARGET -> "오늘은 인증하는 날이 아니에요"
        TodayResultStatus.DONE ->
            today
                ?.streak
                ?.after
                ?.takeIf { it > 0 }
                ?.let { "${it}일 연속 성공 중이에요" }
        // 실패 예정에도 사유·근거를 보인다
        TodayResultStatus.FAIL_EXPECTED,
        TodayResultStatus.FAILED,
        ->
            buildList {
                // 사유는 한 줄만 말한다.
                val reason = today?.failureReason?.failureText() ?: today?.pendingReason?.pendingText()
                reason?.let { add(it) }
                // 끊긴 연속 기록은 사실만 말한다(재촉하지 않는다).
                today?.streak?.takeIf { it.before > 0 && it.after == 0 }?.let { add("연속 ${it.before}일이 끊겼어요") }
            }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        TodayResultStatus.IN_PROGRESS -> today?.pendingReason?.pendingText()
    }

/** 상태 옆에 붙는 보조 문구. */
private fun TodayResult.todayDetail(): String? =
    when (status) {
        TodayResultStatus.DONE -> confirmedAt?.let(::feedTimeLabel)?.takeIf { it.isNotBlank() }
        TodayResultStatus.IN_PROGRESS -> window
        else -> null
    }

/** 이의 진입 버튼 문구. */
private fun TodayResult.appealButtonText(): String {
    val until = appeal?.eligibleUntil?.let { appealDeadlineLabel(it) }
    return if (until == null) "이의 제기" else "이의 제기 · ${until}까지"
}

/** room 의 상태를 오늘 결과의 상태로 옮긴다. */
private fun TodayVerificationStatus.toResultStatus(): TodayResultStatus =
    when (this) {
        TodayVerificationStatus.IN_PROGRESS -> TodayResultStatus.IN_PROGRESS
        TodayVerificationStatus.FAIL_EXPECTED -> TodayResultStatus.FAIL_EXPECTED
        TodayVerificationStatus.DONE -> TodayResultStatus.DONE
        TodayVerificationStatus.FAILED -> TodayResultStatus.FAILED
        TodayVerificationStatus.NOT_TARGET -> TodayResultStatus.NOT_TARGET
    }

/** 내 세부 설정. */
@Composable
internal fun MySetupCard(
    onRegisterApps: (() -> Unit)?,
    onRegisterAnchor: (() -> Unit)?,
) {
    if (onRegisterApps == null && onRegisterAnchor == null) return
    RuleUpCard {
        RoomSectionHeader(title = "내 세부 설정")
        onRegisterApps?.let {
            SetupRow(label = "대상 앱", actionLabel = "수정", onClick = it)
        }
        onRegisterAnchor?.let {
            SetupRow(label = "인증 장소", actionLabel = "수정", onClick = it)
        }
    }
}

@Composable
private fun SetupRow(
    label: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textSlate,
            style = RuleUpTheme.typography.body,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = actionLabel,
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallMedium,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .singleClickable(onClick = onClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

/** 인증 규칙. */
@Composable
private fun VerificationRuleCard(detail: ChallengeDetail) {
    RuleUpCard {
        RoomSectionHeader(title = "인증 규칙")
        Text(
            text =
                detail.verification.detail
                    ?: if (detail.verification.type.isAuto) "자동 인증" else "직접 체크",
            color = RuleUpTheme.colors.textSlate,
            style = RuleUpTheme.typography.body,
        )
    }
}

/** 진행 정보 */
@Composable
private fun ProgressInfoCard(
    detail: ChallengeDetail,
    room: ChallengeRoom,
    today: TodayResult?,
) {
    RuleUpCard {
        RoomSectionHeader(title = "진행 정보")
        InfoLine(
            label = "기간",
            value = periodLabel(detail.period.start, detail.period.end),
        )
        detail.weeklyCount?.let { count ->
            InfoLine(
                label = "빈도",
                value = if (count >= ChallengeLimits.WEEKLY_COUNT_MAX) "매일" else "주 ${count}회",
            )
        }
        InfoLine(
            label = "인원",
            value =
                buildString {
                    append("${room.summary.participantCount}명")
                    append(" / 정원 ${capacityLabel(room.summary.capacity)}")
                    // 봇방장은 승계자가 없어 자리를 지키는 상태다
                    val owner =
                        if (room.ownerType == OwnerType.BOT) {
                            "방장 없음"
                        } else {
                            detail.owner
                                ?.nickname
                                ?.let { "방장 $it" }
                        }
                    owner?.let { append(" · $it") }
                },
        )
        InfoLine(
            label = "방 성공률",
            // 판정 이력이 없으면 null 이다.
            value = room.summary.roomSuccessRate?.let { "${it.toPercentText()}%" } ?: "아직 집계 전",
        )
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.body,
            modifier = Modifier.width(72.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomInfoTabPreview() {
    RuleUpTheme {
        RoomInfoTab(
            detail = com.ruleup.challenge.presentation.common.previewChallenge,
            room =
                com.ruleup.challenge.domain.entity.ChallengeRoom(
                    myRole =
                        com.ruleup.challenge.domain.entity.MemberRole.entries
                            .first(),
                    ownerType =
                        com.ruleup.challenge.domain.entity.OwnerType.entries
                            .first(),
                    summary =
                        com.ruleup.challenge.domain.entity.RoomSummary(
                            title = "매일 꾸준히 걷기",
                            roomSuccessRate = null,
                            remainingDays = 1,
                            participantCount = 1,
                            capacity = null,
                        ),
                    topRanking = emptyList(),
                    myTodayStatus = null,
                ),
            today = null,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TodayVerificationCardPreview() {
    RuleUpTheme {
        TodayVerificationCard(roomStatus = null, today = null, onAppealClick = { })
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MySetupCardPreview() {
    RuleUpTheme {
        MySetupCard(onRegisterApps = { }, onRegisterAnchor = { })
    }
}
