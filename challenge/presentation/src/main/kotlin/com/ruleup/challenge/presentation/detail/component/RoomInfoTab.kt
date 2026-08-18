package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.AppealPolicy
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus

/**
 * 정보 탭 (Figma 1134:143) — 오늘 내 상태 · 고정 공지 · 내 세부 설정 · 인증 규칙 · 진행 정보.
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
    onOpenNoticeInFeed: () -> Unit,
    modifier: Modifier = Modifier,
    onRegisterApps: (() -> Unit)? = null,
    onRegisterAnchor: (() -> Unit)? = null,
    extraSections: @Composable () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TodayVerificationCard(roomStatus = room.myTodayStatus, today = today)

        // Phase 1 은 서버가 고정 공지를 내려주지 않아 이 카드가 통째로 빠진다(공지는 Phase 2).
        room.pinnedNotice?.let {
            PinnedNoticeRow(notice = it, onOpenInFeed = onOpenNoticeInFeed)
        }

        MySetupCard(
            onRegisterApps = onRegisterApps,
            onRegisterAnchor = onRegisterAnchor,
        )

        VerificationRuleCard(detail = detail)

        ProgressInfoCard(detail = detail, room = room, today = today)

        extraSections()
    }
}

/**
 * 오늘 내 인증 (Figma 1134:189).
 *
 * 상태는 인증 모듈의 오늘 결과([today])를 먼저 쓴다 — room 의 `myTodayStatus` 는 상태 하나뿐이라
 * "몇 시에 인증됐는지"·"창이 언제까지인지"를 말해 주지 못한다. 오늘 결과 조회가 실패하면 room 값으로
 * 떨어지고, 그마저 앱이 모르는 값이면 카드를 그리지 않는다 — 모르는 상태를 성공이나 실패로 접어
 * 보여주는 쪽이 아무것도 안 보여주는 것보다 나쁘다.
 */
@Composable
private fun TodayVerificationCard(
    roomStatus: TodayVerificationStatus?,
    today: TodayResult?,
) {
    val label: String
    val color: Color
    when (today?.status ?: roomStatus?.toResultStatus()) {
        null -> return
        TodayResultStatus.DONE -> {
            label = "인증 완료"
            color = RuleUpTheme.colors.success
        }
        // 인증 창이 아직 열려 있다. 실패가 아니므로 경고색을 쓰지 않는다.
        TodayResultStatus.IN_PROGRESS -> {
            label = "인증 진행 중"
            color = RuleUpTheme.colors.brand
        }
        // 00~03시 유예 구간 — 창은 닫혔지만 아직 실패가 아니다.
        TodayResultStatus.CHECKING -> {
            label = "판정 대기 중"
            color = RuleUpPalette.StatusWarn
        }
        TodayResultStatus.FAILED -> {
            label = "인증 실패"
            color = RuleUpTheme.colors.danger
        }
        TodayResultStatus.NOT_TARGET -> {
            label = "오늘은 쉬는 날"
            color = RuleUpTheme.colors.textMuted
        }
    }
    Row(
        modifier = Modifier.ruleUpCardSurface(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "오늘 내 인증",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Spacer(Modifier.weight(1f))
        today?.todayDetail()?.let {
            Text(
                text = it,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.caption,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = color,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
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

/** room 의 상태를 오늘 결과의 상태로 옮긴다. 두 계약은 같은 어휘를 쓴다. */
private fun TodayVerificationStatus.toResultStatus(): TodayResultStatus =
    when (this) {
        TodayVerificationStatus.IN_PROGRESS -> TodayResultStatus.IN_PROGRESS
        TodayVerificationStatus.CHECKING -> TodayResultStatus.CHECKING
        TodayVerificationStatus.DONE -> TodayResultStatus.DONE
        TodayVerificationStatus.FAILED -> TodayResultStatus.FAILED
        TodayVerificationStatus.NOT_TARGET -> TodayResultStatus.NOT_TARGET
    }

/** 고정 공지 요약 행 (Figma 1134:195). 본문은 피드 최상단 배너에서 이어 읽는다. */
@Composable
private fun PinnedNoticeRow(
    notice: NoticeSummary,
    onOpenInFeed: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .singleClickable(onClick = onOpenInFeed)
                .ruleUpCardSurface(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "공지",
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.captionBold,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RuleUpTheme.colors.brandSoft)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = notice.title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "피드에서 보기",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
    }
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
        // 이의는 실패한 날에만 낼 수 있어 서버도 FAILED 일 때만 내려준다 — 없으면 줄을 만들지 않는다.
        today?.appeal?.let { appeal ->
            InfoLine(
                label = "내 이의",
                value = "이번 달 ${appeal.remainingThisMonth}회 남음 (월 ${AppealPolicy.MONTHLY_LIMIT}회)",
            )
        }
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
