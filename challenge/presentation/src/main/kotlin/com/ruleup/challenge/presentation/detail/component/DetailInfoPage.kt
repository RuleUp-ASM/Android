package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.presentation.common.capacityLabel
import com.ruleup.challenge.presentation.create.label
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 「상세 내용 보기」. 표지에서 다 못 보여 준 챌린지 정보를 모은다.
 *
 * [primaryLabel] 은 표지의 오른쪽 버튼과 같은 동작이다(가입하기 / 들어가기 / 셋업 단계).
 * 꺼진 벌칙은 숨기지 않고 회색 「꺼짐」으로 둬서, 이 챌린지에 무엇이 없는지도 알 수 있게 한다.
 */
@Composable
internal fun DetailInfoPage(
    detail: ChallengeDetail,
    primaryLabel: String?,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    extraBottom: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxSize().background(RuleUpTheme.colors.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            ChallengeCoverImage(
                imageUrl = detail.imageUrl,
                category = detail.category,
                modifier = Modifier.matchParentSize(),
            )
            Box(coverScrim())
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                CoverTopBar(menuItems = emptyList(), onBack = onBack)
                Spacer(Modifier.weight(1f))
                GlassChip(detail.title)
                Text(
                    text = "상세 내용",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = RuleUpTheme.typography.title,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            detail.description?.takeIf { it.isNotBlank() }?.let { description ->
                RuleUpCard {
                    RoomSectionHeader(title = "소개")
                    Text(text = description, color = RuleUpTheme.colors.textSlate, style = RuleUpTheme.typography.body)
                }
            }
            RuleUpCard {
                RoomSectionHeader(title = "인증 방법")
                Text(
                    text = if (detail.verification.type.isAuto) "자동 인증" else "직접 체크",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.smallBold,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(RuleUpTheme.colors.brandSoft)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                detail.verification.detail?.let {
                    Text(text = it, color = RuleUpTheme.colors.textSlate, style = RuleUpTheme.typography.body)
                }
            }
            RuleUpCard {
                RoomSectionHeader(title = "기간·일정")
                InfoRow(label = "기간", value = periodLabel(detail.period.start, detail.period.end))
                detail.weeklyCount?.let { count ->
                    InfoRow(label = "인증하는 날", value = if (count >= ChallengeLimits.WEEKLY_COUNT_MAX) "매일" else "주 ${count}회")
                }
                InfoRow(
                    label = "시작",
                    value =
                        when (detail.joinNote) {
                            JoinNote.IMMEDIATE -> "가입하면 바로 시작"
                            JoinNote.NEXT_CYCLE -> "다음 주기부터 판정"
                        },
                )
            }
            detail.penalties?.let { penalties ->
                RuleUpCard {
                    RoomSectionHeader(title = "벌칙")
                    PenaltyRow(title = "점수 차감", description = "실패하면 티어 점수가 줄어요", on = penalties.score)
                    PenaltyRow(title = "그룹에 공유", description = "실패가 방 피드에 올라가요", on = penalties.groupShare)
                    PenaltyRow(title = "감시자 알림", description = "실패가 확정되면 감시자에게 알려요", on = penalties.watcher)
                }
            }
            RuleUpCard {
                RoomSectionHeader(title = "참여 조건")
                InfoRow(label = "최소 티어", value = detail.gate.minTier?.let { "${it.label()} 이상" } ?: "제한 없음")
                InfoRow(label = "정원", value = "${detail.participantCount}명 / ${capacityLabel(detail.capacity)}")
                InfoRow(label = "공개 범위", value = if (detail.visibility?.isPrivate == true) "초대로만" else "전체 공개")
            }
            RuleUpCard {
                RoomSectionHeader(title = "이 챌린지 기록")
                Row {
                    StatCell(label = "완주율", rate = detail.stats.completionRate, modifier = Modifier.weight(1f))
                    StatCell(label = "유지율", rate = detail.stats.retentionRate, modifier = Modifier.weight(1f))
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            extraBottom?.invoke()
            if (primaryLabel != null) {
                RuleUpPrimaryButton(text = primaryLabel, enabled = primaryEnabled, onClick = onPrimary)
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(text = label, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.body, modifier = Modifier.width(88.dp))
        Text(
            text = value,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PenaltyRow(
    title: String,
    description: String,
    on: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (on) RuleUpTheme.colors.success else RuleUpTheme.colors.border),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (on) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.bodyBold,
            )
            Text(
                text = if (on) description else "이 챌린지는 꺼져 있어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(
            text = if (on) "켜짐" else "꺼짐",
            color = if (on) RuleUpTheme.colors.success else RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    rate: Double?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 표본이 없으면 0% 가 아니라 집계 전이다
        Text(
            text = rate?.let { "${it.toPercentText()}%" } ?: "집계 전",
            color = if (rate != null) RuleUpTheme.colors.brand else RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.section,
        )
        Text(text = label, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
    }
}
