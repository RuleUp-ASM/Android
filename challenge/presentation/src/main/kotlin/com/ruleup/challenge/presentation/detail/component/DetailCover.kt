package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.presentation.common.capacityLabel
import com.ruleup.designsystem.R
import com.ruleup.designsystem.category.CategoryCover
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category
import com.ruleup.tti.presentation.rememberTtiLargeContent

/**
 * 챌린지 대표 사진. 없거나 못 불러오면 카테고리 기본 커버를 그린다.
 * 사진은 기본 커버 위에 덮어서, 불러오는 동안에도 기본 커버가 보인다.
 */
@Composable
internal fun ChallengeCoverImage(
    imageUrl: String?,
    category: Category?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CategoryCover(category = category, modifier = Modifier.matchParentSize())
        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val onImageSettled = rememberTtiLargeContent()
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { onImageSettled() },
                onError = { onImageSettled() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 사진 위에 올리는 반투명 원형 버튼. */
@Composable
internal fun GlassIconButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 사진 위 반투명 칩. */
@Composable
internal fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.24f),
) {
    Text(
        text = text,
        color = Color.White,
        style = RuleUpTheme.typography.smallBold,
        modifier =
            modifier
                .clip(RoundedCornerShape(99.dp))
                .background(background)
                .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** 사진 위 상단: 뒤로 + ⋯ 메뉴. */
@Composable
internal fun CoverTopBar(
    menuItems: List<RoomMenuItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMenu: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(iconRes = R.drawable.ic_arrow_back, description = "뒤로", onClick = onBack)
        Spacer(Modifier.weight(1f))
        if (onOpenMenu != null || menuItems.isNotEmpty()) {
            Box {
                GlassIconButton(
                    iconRes = R.drawable.ic_more_vertical,
                    description = "더 보기",
                    onClick = { if (onOpenMenu != null) onOpenMenu() else menuOpen = true },
                )
                if (menuOpen) RoomMenuPopup(items = menuItems, onDismiss = { menuOpen = false })
            }
        }
    }
}

/** 사진을 어둡게 덮어 위아래 글자가 읽히게 한다. */
internal fun BoxScope.coverScrim(): Modifier =
    Modifier.matchParentSize().background(
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.28f),
            0.35f to Color.Black.copy(alpha = 0.05f),
            0.6f to Color.Black.copy(alpha = 0.45f),
            1f to Color.Black.copy(alpha = 0.85f),
        ),
    )

/** 「10.1 – 10.31」. */
internal fun ChallengeDetail.periodRange(): String = periodLabel(period.start, period.end).substringBefore(" · ")

/** 「4주 2일 · 주 5회」. */
internal fun ChallengeDetail.scheduleLabel(): String =
    listOfNotNull(
        periodLabel(period.start, period.end).substringAfter(" · ", "").takeIf { it.isNotBlank() },
        weeklyCount?.let { if (it >= ChallengeLimits.WEEKLY_COUNT_MAX) "매일" else "주 ${it}회" },
        verification.detail,
    ).joinToString(" · ")

/** 방장이 나가면 봇이 자리를 지킨다(owner 가 null 이 된다). */
internal fun ChallengeDetail.ownerLabel(): String = owner?.nickname?.let { "방장 $it" } ?: if (ownerType == OwnerType.BOT) "봇 방장" else "방장 없음"

/**
 * 챌린지 표지(Figma 시안 C). 대표 사진이 화면을 덮고, 구석에 카테고리·기간, 아래에 제목·일정·멤버·버튼을 올린다.
 *
 * [primaryLabel] 이 null 이면 오른쪽 버튼 대신 [blockedNotice] 를 그린다(초대 링크로만 들어오는 방).
 */
@Composable
internal fun DetailCover(
    detail: ChallengeDetail,
    members: ChallengeMembers?,
    primaryLabel: String?,
    primaryEnabled: Boolean,
    blockedNotice: String?,
    onOpenMenu: () -> Unit,
    onPrimary: () -> Unit,
    onOpenInfo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ChallengeCoverImage(
            imageUrl = detail.imageUrl,
            category = detail.category,
            modifier = Modifier.fillMaxSize(),
        )
        Box(coverScrim())
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            CoverTopBar(menuItems = emptyList(), onBack = onBack, onOpenMenu = onOpenMenu)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                detail.category?.let { GlassChip(it.label) }
                Spacer(Modifier.weight(1f))
                GlassChip(detail.periodRange(), background = Color.Black.copy(alpha = 0.35f))
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (detail.myRole.isMember) GlassChip("참여 중", background = RuleUpTheme.colors.success)
                Text(
                    text = detail.title,
                    color = Color.White,
                    style = RuleUpTheme.typography.numberL,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                detail.scheduleLabel().takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = Color.White.copy(alpha = 0.85f), style = RuleUpTheme.typography.smallMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            MemberPanel(detail = detail, members = members)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoverButton(
                    text = "상세 내용 보기",
                    filled = false,
                    enabled = true,
                    onClick = onOpenInfo,
                    modifier = Modifier.weight(1f),
                )
                if (primaryLabel != null) {
                    CoverButton(
                        text = primaryLabel,
                        filled = true,
                        enabled = primaryEnabled,
                        onClick = onPrimary,
                        modifier = Modifier.weight(1f),
                    )
                } else if (blockedNotice != null) {
                    Text(
                        text = blockedNotice,
                        color = Color.White.copy(alpha = 0.85f),
                        style = RuleUpTheme.typography.caption,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

/** 인원 · 아바타 · 방장. 멤버 목록을 못 받았으면 숫자만 보인다. */
@Composable
private fun MemberPanel(
    detail: ChallengeDetail,
    members: ChallengeMembers?,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${detail.participantCount} / ${capacityLabel(detail.capacity)} 참여 중",
                color = Color.White,
                style = RuleUpTheme.typography.bodyBold,
                modifier = Modifier.weight(1f),
            )
            Text(text = detail.ownerLabel(), color = Color.White.copy(alpha = 0.85f), style = RuleUpTheme.typography.smallMedium)
        }
        val shown = members?.members.orEmpty().take(MEMBER_AVATAR_MAX)
        if (shown.isNotEmpty()) {
            Box {
                shown.forEachIndexed { index, member ->
                    Box(
                        modifier =
                            Modifier
                                .offset(x = (index * 30).dp)
                                .border(2.dp, Color.White, CircleShape),
                    ) {
                        RoomAvatar(nickname = member.user.nickname, size = 40.dp, highlighted = index == 0)
                    }
                }
                val rest = (members?.participantCount ?: detail.participantCount) - shown.size
                if (rest > 0) {
                    Box(
                        modifier =
                            Modifier
                                .offset(x = (shown.size * 30).dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "+$rest", color = Color.White, style = RuleUpTheme.typography.smallBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverButton(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier =
            modifier
                .height(54.dp)
                .clip(shape)
                .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
                .border(1.dp, if (filled) Color.Transparent else Color.White.copy(alpha = 0.4f), shape)
                .singleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) RuleUpTheme.colors.brand else Color.White,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

private const val MEMBER_AVATAR_MAX = 5
