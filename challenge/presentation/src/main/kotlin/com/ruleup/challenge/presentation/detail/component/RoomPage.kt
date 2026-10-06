package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 들어간 뒤 상단(Figma 시안 C). 사진 위에 제목·진행 상황을 올리고, 감시자 벌칙이 켜진 방이면 [감시자 N명 ›] 를 단다.
 */
@Composable
internal fun RoomCoverHeader(
    detail: ChallengeDetail,
    subtitle: String,
    watcherCount: Int?,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenWatchers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ChallengeCoverImage(
            imageUrl = detail.imageUrl,
            category = detail.category,
            emojiSize = 64.sp,
            modifier = Modifier.matchParentSize(),
        )
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.42f)))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            CoverTopBar(menuItems = emptyList(), onBack = onBack, onOpenMenu = onOpenMenu)
            Spacer(Modifier.height(12.dp))
            detail.category?.let { GlassChip(it.label) }
            Text(
                text = detail.title,
                color = Color.White,
                style = RuleUpTheme.typography.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    style = RuleUpTheme.typography.smallMedium,
                    modifier = Modifier.weight(1f),
                )
                if (watcherCount != null) {
                    val shape = RoundedCornerShape(99.dp)
                    Text(
                        text = "감시자 ${watcherCount}명 ›",
                        color = Color.White,
                        style = RuleUpTheme.typography.smallBold,
                        modifier =
                            Modifier
                                .clip(shape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
                                .singleClickable(onClick = onOpenWatchers)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** 피드 / 랭킹 알약 탭. */
@Composable
internal fun RoomPillTabs(
    selected: RoomTab,
    onSelect: (RoomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(4.dp),
    ) {
        RoomTab.entries.forEach { tab ->
            val on = tab == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (on) RuleUpTheme.colors.brand else Color.Transparent)
                        .singleClickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    color = if (on) Color.White else RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.bodyBold,
                )
            }
        }
    }
}

/** 오늘 인증 카드 아래 바로가기 한 줄. 할 수 있는 것만 보인다. */
@Composable
internal fun TodayQuickActions(
    onManualCheck: (() -> Unit)?,
    onPermissionRepair: (() -> Unit)?,
    onCalendar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val actions =
        listOfNotNull(
            onManualCheck?.let { "오늘 체크하기" to it },
            onPermissionRepair?.let { "권한 다시 연결" to it },
            onCalendar?.let { "캘린더" to it },
        )
    if (actions.isEmpty()) return
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { (label, onClick) ->
            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(shape)
                        .background(RuleUpTheme.colors.surface)
                        .border(1.dp, RuleUpTheme.colors.border, shape)
                        .singleClickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.smallBold)
            }
        }
    }
}

/** ⋯ 메뉴 시트 항목. [toggle] 이 있으면 오른쪽에 스위치를 그린다. */
internal data class RoomSheetEntry(
    val label: String,
    val value: String? = null,
    val danger: Boolean = false,
    val toggle: Boolean? = null,
    val toggleEnabled: Boolean = true,
    val onClick: () -> Unit,
)

/** 방의 ⋯ 메뉴. 가끔 쓰는 설정·기록·관리 동작을 모은다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomMenuSheet(
    entries: List<RoomSheetEntry>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = RuleUpTheme.colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0 && entry.danger && !entries[index - 1].danger) {
                    HorizontalDivider(color = RuleUpTheme.colors.border, modifier = Modifier.padding(horizontal = 24.dp))
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .singleClickable(enabled = entry.toggle == null || entry.toggleEnabled) {
                                if (entry.toggle == null) onDismiss()
                                entry.onClick()
                            }.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.label,
                        color = if (entry.danger) RuleUpTheme.colors.danger else RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.bodyBold,
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        entry.toggle != null ->
                            Switch(
                                checked = entry.toggle,
                                onCheckedChange = { entry.onClick() },
                                enabled = entry.toggleEnabled,
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor = RuleUpTheme.colors.brand,
                                        checkedThumbColor = Color.White,
                                    ),
                            )

                        entry.value != null ->
                            Text(text = entry.value, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.small)
                    }
                }
            }
        }
    }
}

/** 시트에 한 덩어리 내용을 담는다(캘린더 · 멤버). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomContentSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = RuleUpTheme.colors.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

/** 솔로 방은 피드·랭킹이 없어 오늘 인증과 캘린더를 한 화면에 둔다. */
@Composable
internal fun SoloRoomBody(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}
