package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.challenge.presentation.explore.list.DDAY_URGENT_THRESHOLD
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/** 방 상세 상단바. */
@Composable
internal fun RoomAppBar(
    title: String,
    menuItems: List<RoomMenuItem>,
    onBack: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconSlot(
            iconRes = com.ruleup.designsystem.R.drawable.ic_arrow_back,
            description = "뒤로",
            onClick = onBack,
        )
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box {
            // 메뉴에 담을 게 없으면 자리만 비워 제목이 가운데를 유지하게 한다.
            if (menuItems.isEmpty()) {
                Spacer(Modifier.size(48.dp))
            } else {
                IconSlot(
                    iconRes = com.ruleup.designsystem.R.drawable.ic_more_vertical,
                    description = "더 보기",
                    onClick = { menuOpen = true },
                )
            }
            if (menuOpen) {
                RoomMenuPopup(
                    items = menuItems,
                    onDismiss = { menuOpen = false },
                )
            }
        }
    }
}

/** ⋯ 메뉴 박스. 화면 끝에 붙지 않게 우측 15dp, 상단바 아래 10dp 에 띄운다. */
@Composable
internal fun RoomMenuPopup(
    items: List<RoomMenuItem>,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val offset =
        with(density) {
            IntOffset(x = (-15).dp.roundToPx(), y = (48 + 10).dp.roundToPx())
        }
    val shape = RoundedCornerShape(14.dp)
    Popup(
        alignment = Alignment.TopEnd,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier =
                Modifier
                    .width(151.dp)
                    .shadow(6.dp, shape, clip = false)
                    .clip(shape)
                    .background(RuleUpTheme.colors.surface),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
                Text(
                    text = item.label,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .singleClickable {
                                onDismiss()
                                item.onClick()
                            }.padding(horizontal = 15.dp, vertical = 15.dp),
                )
            }
        }
    }
}

/** 상단바 ⋯ 메뉴 항목. */
internal data class RoomMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun IconSlot(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = RuleUpTheme.colors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 정보 탭 헤더 */
@Composable
internal fun RoomInfoHeader(
    categoryLabel: String?,
    remainingDays: Int,
    myProgressRate: Double?,
    imageUrl: String? = null,
) {
    // 카테고리부터 탭 위까지를 대표 사진 배경으로 채운다.
    ChallengeCoverBackground(
        imageUrl = imageUrl,
        scrim = RuleUpTheme.colors.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categoryLabel?.let {
                Text(
                    text = it,
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.captionMedium,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RuleUpTheme.colors.brandSoft)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "종료까지",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.caption,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // 종료일 당일·경과는 음수가 되므로 D-day 표기를 나눈다.
                        text = if (remainingDays > 0) "D-$remainingDays" else "D-day",
                        color =
                            if (remainingDays <= DDAY_URGENT_THRESHOLD) {
                                RuleUpTheme.colors.danger
                            } else {
                                RuleUpTheme.colors.warning
                            },
                        style = RuleUpTheme.typography.numberXl,
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "내 달성률",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.caption,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = myProgressRate?.let { "${it.toPercentText()}%" } ?: "-",
                        color = RuleUpTheme.colors.brand,
                        style = RuleUpTheme.typography.numberXl,
                    )
                }
            }
            RoomProgressBar(rate = myProgressRate)
        }
    }
}

@Composable
private fun RoomProgressBar(rate: Double?) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(RuleUpTheme.colors.surfaceVariant),
    ) {
        val fraction = (rate ?: 0.0).coerceIn(0.0, 1.0).toFloat()
        if (fraction > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(3.dp))
                        .background(RuleUpTheme.colors.brand),
            )
        }
    }
}

/** 정보 · 피드 · 랭킹 탭. */
@Composable
internal fun RoomTabRow(
    selected: RoomTab,
    onSelect: (RoomTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        RoomTab.entries.forEach { tab ->
            val isSelected = tab == selected
            // 폭을 글자에 묶지 않으면 아래 밑줄(fillMaxWidth)이 첫 탭을 줄 끝까지 늘려 나머지 탭이 밀려난다.
            Column(
                modifier = Modifier.width(IntrinsicSize.Max).singleClickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
                    style =
                        if (isSelected) {
                            RuleUpTheme.typography.bodyBold
                        } else {
                            RuleUpTheme.typography.bodyMedium
                        },
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isSelected) {
                                    RuleUpTheme.colors.textPrimary
                                } else {
                                    RuleUpTheme.colors.background
                                },
                            ),
                )
            }
        }
    }
}

/** 랭킹 탭의 멤버 ↔ 방 순위 세그먼트. */
@Composable
internal fun <T> RoomSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) RuleUpTheme.colors.surface else RuleUpTheme.colors.surfaceVariant,
                        ).singleClickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textSecondary,
                    style =
                        if (isSelected) {
                            RuleUpTheme.typography.bodyBold
                        } else {
                            RuleUpTheme.typography.bodyMedium
                        },
                )
            }
        }
    }
}

@Composable
internal fun RoomSectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.smallMedium,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .singleClickable(onClick = onAction)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** 아바타 자리. */
@Composable
internal fun RoomAvatar(
    nickname: String,
    size: androidx.compose.ui.unit.Dp = 34.dp,
    highlighted: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (highlighted) RuleUpTheme.colors.brand else RuleUpTheme.colors.brandSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.take(1).ifBlank { "?" },
            color = if (highlighted) RuleUpTheme.colors.surface else RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallBold,
        )
    }
}

@Composable
internal fun RoomEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.body,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.bodyBold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(RuleUpTheme.colors.brandSoft)
                        .singleClickable(onClick = onAction)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
internal fun RoomVerticalDivider() {
    Box(
        modifier =
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(RuleUpTheme.colors.border),
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomAppBarPreview() {
    RuleUpTheme {
        RoomAppBar(title = "매일 꾸준히 걷기", menuItems = emptyList(), onBack = { })
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomInfoHeaderPreview() {
    RuleUpTheme {
        RoomInfoHeader(categoryLabel = null, remainingDays = 1, myProgressRate = null)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomTabRowPreview() {
    RuleUpTheme {
        RoomTabRow(
            selected =
                com.ruleup.challenge.presentation.detail.viewmodel.RoomTab.entries
                    .first(),
            onSelect = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomSectionHeaderPreview() {
    RuleUpTheme {
        RoomSectionHeader(title = "매일 꾸준히 걷기")
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomAvatarPreview() {
    RuleUpTheme {
        RoomAvatar(nickname = "룰업")
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomEmptyStatePreview() {
    RuleUpTheme {
        RoomEmptyState(message = "아직 등록된 내용이 없어요")
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomVerticalDividerPreview() {
    RuleUpTheme {
        RoomVerticalDivider()
    }
}
