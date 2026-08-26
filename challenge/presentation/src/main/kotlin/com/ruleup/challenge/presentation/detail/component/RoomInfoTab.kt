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
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus

/**
 * 정보 탭 (Figma 1134:143) — 오늘 내 상태 · 내 세부 설정 · 인증 규칙 · 진행 정보.
 *
 * 방에 들어와 가장 먼저 확인하는 건 "오늘 내가 됐나"이므로 그 카드를 맨 위에 둔다. 아래로는 바꿀 수
 * 있는 것(내 설정) → 바뀌지 않는 것(규칙·기간) 순이다.
 *
 * 세부 설정과 감시자는 **이미 있는 화면·섹션으로 연결**한다 — 이 탭에 같은 기능을 다시 만들지 않는다.
 */
@Composable
internal fun RoomInfoTab(
    detail: ChallengeDetail,
    room: ChallengeRoom,
    today: TodayResult?,
    modifier: Modifier = Modifier,
    onRegisterApps: (() -> Unit)? = null,
    onRegisterAnchor: (() -> Unit)? = null,
    onSubmitAppeal: ((reason: String) -> Unit)? = null,
    // 수동 방일 때만 넘어온다 — 자동 방에 보조 수동 버튼을 두지 않는 것이 확정 규칙이다.
    onManualCheck: (() -> Unit)? = null,
    onManualUncheck: (() -> Unit)? = null,
    isManualChecking: Boolean = false,
    isSubmittingAppeal: Boolean = false,
    appealImageUrl: String? = null,
    isUploadingAppealImage: Boolean = false,
    appealReasonError: String? = null,
    onPickAppealImage: () -> Unit = {},
    onDismissAppeal: () -> Unit = {},
    extraSections: @Composable () -> Unit = {},
) {
    // 이의 입력 다이얼로그 열림 여부. 실패 카드에서만 열린다.
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
            onManualCheck = onManualCheck,
            onManualUncheck = onManualUncheck,
            isManualChecking = isManualChecking,
            // 이의는 실패 확정 건에만, 그것도 대상 인증 건 ID 를 알 때만 낼 수 있다.
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

        extraSections()
    }

    if (appealOpen && onSubmitAppeal != null && today != null) {
        AppealSheet(
            today = today,
            submitting = isSubmittingAppeal,
            imageUrl = appealImageUrl,
            uploadingImage = isUploadingAppealImage,
            reasonError = appealReasonError,
            onPickImage = onPickAppealImage,
            onSubmit = { reason ->
                appealOpen = false
                onSubmitAppeal(reason)
            },
            onDismiss = {
                appealOpen = false
                onDismissAppeal()
            },
        )
    }
}

/**
 * 오늘 내 인증 (Figma `1134:143` · 실패 변형 `1134:512`).
 *
 * 상태는 인증 모듈의 오늘 결과([today])를 먼저 쓴다 — room 의 `myTodayStatus` 는 상태 하나뿐이라
 * "몇 시에 인증됐는지"·"창이 언제까지인지"를 말해 주지 못한다. 오늘 결과 조회가 실패하면 room 값으로
 * 떨어지고, 그마저 앱이 모르는 값이면 카드를 그리지 않는다 — 모르는 상태를 성공이나 실패로 접어
 * 보여주는 쪽이 아무것도 안 보여주는 것보다 나쁘다.
 *
 * 실패는 **두 얼굴**이다. 아직 이의할 수 있으면 붉은 배지 대신 카드 톤으로만 알리고 마감 시각과 함께
 * 진입점을 준다. 기한이 지나면 그때 `실패 확정` 배지가 붙는다 — 되돌릴 수 있는 실패와 끝난 실패를
 * 같은 얼굴로 보여주면 사용자는 아직 남은 기회를 모른 채 넘긴다.
 */
@Composable
private fun TodayVerificationCard(
    roomStatus: TodayVerificationStatus?,
    today: TodayResult?,
    onAppealClick: (() -> Unit)?,
    onManualCheck: (() -> Unit)? = null,
    onManualUncheck: (() -> Unit)? = null,
    isManualChecking: Boolean = false,
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
        // 인증 창이 아직 열려 있다. 실패가 아니므로 경고색을 쓰지 않는다.
        TodayResultStatus.IN_PROGRESS -> {
            label = "인증 진행 중"
            color = colors.brand
        }
        // 창은 닫혔지만 성공·실패 양쪽으로 열려 있는 구간이다. 실패로 보이게 하지 않는다.
        TodayResultStatus.CHECKING -> {
            label = "검사중"
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
    // 이의를 낼 수 있는 실패만 카드 테두리로 알린다(배지 없음, Figma 1134:512 상태 1).
    val appealAction = onAppealClick?.takeIf { status == TodayResultStatus.FAILED }
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
            // 이의 가능 구간에는 실패 배지를 달지 않는다 — 아직 끝난 결과가 아니다.
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

        appealAction?.let { action ->
            RuleUpPrimaryButton(
                text = today?.appealButtonText() ?: "이의 제기",
                onClick = action,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 수동 방의 체크·해제. 자동 방에는 onManual* 이 넘어오지 않아 아무것도 그리지 않는다.
        when {
            status == TodayResultStatus.DONE && onManualUncheck != null ->
                Text(
                    text = if (isManualChecking) "해제하는 중…" else "체크 해제",
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .singleClickable(enabled = !isManualChecking, onClick = onManualUncheck)
                            .padding(vertical = 8.dp),
                )

            status == TodayResultStatus.IN_PROGRESS && onManualCheck != null ->
                RuleUpPrimaryButton(
                    text = if (isManualChecking) "체크하는 중…" else "오늘 인증 체크",
                    onClick = onManualCheck,
                    enabled = !isManualChecking,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

/**
 * 상태 아래에 붙는 안내 문구 (프론트엔드 테크스펙 4-8).
 *
 * 실패는 사유를 먼저 말한다 — 자동 인증은 신호가 비는 것만으로도 실패하므로, 사유 없이 "실패"만
 * 남으면 사용자는 자기가 뭘 잘못했는지 알 수 없다.
 */
private fun todayNote(
    status: TodayResultStatus,
    today: TodayResult?,
): String? =
    when (status) {
        // 확정 전 유예 구간. 미완료나 실패로 읽히지 않게 계산 중임을 밝힌다.
        TodayResultStatus.CHECKING -> "최종 결과를 계산하고 있어요"
        TodayResultStatus.NOT_TARGET -> "오늘은 인증하는 날이 아니에요"
        TodayResultStatus.DONE ->
            today
                ?.streak
                ?.after
                ?.takeIf { it > 0 }
                ?.let { "${it}일 연속 성공 중이에요" }
        TodayResultStatus.FAILED ->
            buildList {
                today?.failureReason?.let { add(it.failureText()) }
                // 끊긴 연속 기록은 사실만 말한다(재촉하지 않는다).
                today?.streak?.takeIf { it.before > 0 && it.after == 0 }?.let { add("연속 ${it.before}일이 끊겼어요") }
            }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        TodayResultStatus.IN_PROGRESS -> null
    }

/**
 * 상태 옆에 붙는 보조 문구. 성공이면 확정 시각("06:24"), 진행 중이면 인증 창이다 —
 * 어느 쪽도 없으면 붙이지 않는다.
 */
private fun TodayResult.todayDetail(): String? =
    when (status) {
        TodayResultStatus.DONE -> confirmedAt?.let(::feedTimeLabel)?.takeIf { it.isNotBlank() }
        TodayResultStatus.IN_PROGRESS -> window
        else -> null
    }

/** 이의 진입 버튼 문구. 마감 시각을 버튼에 실어 "언제까지"를 놓치지 않게 한다(Figma 1134:512). */
private fun TodayResult.appealButtonText(): String {
    val until = appeal?.eligibleUntil?.let { appealDeadlineLabel(it) }
    return if (until == null) "이의 제기" else "이의 제기 · ${until}까지"
}

/** room 의 상태를 오늘 결과의 상태로 옮긴다. 두 계약은 같은 어휘를 쓴다. */
private fun TodayVerificationStatus.toResultStatus(): TodayResultStatus =
    when (this) {
        TodayVerificationStatus.IN_PROGRESS -> TodayResultStatus.IN_PROGRESS
        TodayVerificationStatus.CHECKING -> TodayResultStatus.CHECKING
        TodayVerificationStatus.DONE -> TodayResultStatus.DONE
        TodayVerificationStatus.FAILED -> TodayResultStatus.FAILED
        TodayVerificationStatus.NOT_TARGET -> TodayResultStatus.NOT_TARGET
    }

/**
 * 내 세부 설정 (Figma 1134:200).
 *
 * 개인 인증 설정 화면은 대상 앱 등록·인증 장소 등록 **두 개가 이미 있다.** 여기서는 그 진입점만 두고,
 * 해당 인증 방식이 아니면(콜백이 null) 줄 자체를 만들지 않는다 — 눌러도 아무것도 없는 항목을
 * 남겨 두면 설정이 빠진 것처럼 읽힌다.
 */
@Composable
private fun MySetupCard(
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

/**
 * 인증 규칙 (Figma 1134:209).
 *
 * 서버가 사람이 읽는 문장(`verification.detail`)을 내려주므로 앱이 루틴별 문구를 조립하지 않는다 —
 * 루틴 표는 서버에서 계속 늘어난다.
 */
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

/** 진행 정보 (Figma 1134:214) — 기간 · 인원과 방장 · 공개 범위. */
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
        InfoLine(
            label = "인원",
            value =
                buildString {
                    append("${room.summary.participantCount}명")
                    if (room.summary.capacity > 0) append(" / 정원 ${room.summary.capacity}명")
                    // 봇방장은 승계자가 없어 자리를 지키는 상태다 — 사람 이름이 없다는 사실을 그대로 적는다.
                    val owner =
                        if (room.ownerType == OwnerType.BOT) "방장 없음" else detail.owner?.nickname?.let { "방장 $it" }
                    owner?.let { append(" · $it") }
                },
        )
        InfoLine(
            label = "방 성공률",
            // 판정 이력이 없으면 null 이다. "아직 없음"과 0% 는 다른 사실이라 구분해 적는다.
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
