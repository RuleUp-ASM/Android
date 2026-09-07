package com.ruleup.profile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.designsystem.component.RuleUpBottomTab
import com.ruleup.designsystem.component.RuleUpBottomTabBar
import com.ruleup.designsystem.component.RuleUpProgressBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.presentation.common.label
import com.ruleup.profile.presentation.common.thousandsLabel
import com.ruleup.profile.presentation.home.viewmodel.MyHomeEffect
import com.ruleup.profile.presentation.home.viewmodel.MyHomeIntent
import com.ruleup.profile.presentation.home.viewmodel.MyHomeState
import com.ruleup.profile.presentation.home.viewmodel.MyHomeViewModel
import com.ruleup.ui.helper.LocalMessageHelper

private val AvatarGradient = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300)

/** 마이 허브 (Figma 1134:1353). 하단 MY 탭의 루트 화면. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MyHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyHomeIntent.Load)
    }
    // 프로필 편집·챌린지 진행 등에서 돌아오면 조용히 갱신한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(MyHomeIntent.Refresh)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MyHomeEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    MyHomeContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyHomeContent(
    state: MyHomeState,
    onIntent: (MyHomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.home == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "마이 정보를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .padding(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MyHomeHeader()
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileRow(
                            home = state.home,
                            onClick = { onIntent(MyHomeIntent.OpenProfileEdit) },
                        )
                        TierCard(home = state.home, onDetail = { onIntent(MyHomeIntent.OpenTier) })
                        CountsRow(home = state.home, stats = state.stats)
                        RecordMenuCard(onIntent = onIntent)
                        AccountMenuCard(onIntent = onIntent)
                        ExtraMenuCard(onIntent = onIntent)
                    }
                }
        }

        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.MY,
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.HOME -> onIntent(MyHomeIntent.OpenHomeTab)
                    RuleUpBottomTab.EXPLORE -> onIntent(MyHomeIntent.OpenChallengeTab)
                    // TODO(#411): "챌린지" 탭 목적지는 챌린지 탭 화면과 함께 붙인다.
                    RuleUpBottomTab.CHALLENGE -> Unit
                    RuleUpBottomTab.MY -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    val picker = state.rankingPicker
    if (picker != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(MyHomeIntent.DismissRankingPicker) },
            sheetState = sheetState,
            containerColor = RuleUpTheme.colors.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "어느 그룹의 랭킹을 볼까요?",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.section,
                )
                Spacer(Modifier.height(12.dp))
                picker.forEach { challenge ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .singleClickable(
                                    onClick = {
                                        onIntent(MyHomeIntent.SelectRankingChallenge(challenge.challengeId))
                                    },
                                ).padding(horizontal = 4.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = challenge.title,
                            color = RuleUpTheme.colors.textPrimary,
                            style = RuleUpTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "›",
                            color = RuleUpTheme.colors.textMuted,
                            style = RuleUpTheme.typography.section,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyHomeHeader() {
    Text(
        text = "마이",
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.title,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp),
    )
}

/** 프로필 행 (Figma 1134:1372). 탭하면 프로필 수정으로 간다. */
@Composable
private fun ProfileRow(
    home: MyHome,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .singleClickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(AvatarGradient)),
            contentAlignment = Alignment.Center,
        ) {
            if (home.profileImageUrl != null) {
                AsyncImage(
                    model = home.profileImageUrl,
                    contentDescription = "프로필 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = home.nickname.take(1),
                    color = RuleUpPalette.BgSurface,
                    // 장식용 글리프라 타입 스케일(최대 22)에 넣으면 확 줄어든다. 그리는 크기로 잡는다.
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = home.nickname,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.cardTitle,
                )
                nicknameBadgeLabel(home.nicknameStatus)?.let { badge ->
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RuleUpTheme.colors.surfaceVariant)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = badge,
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.tinyBold,
                        )
                    }
                }
            }
            Text(
                text = "프로필 수정",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(
            text = "›",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.section,
        )
    }
}

// 검수 중/반려만 뱃지 노출 (본인 화면은 닉네임 자체는 그대로 보여준다)
private fun nicknameBadgeLabel(status: NicknameStatus): String? =
    when (status) {
        NicknameStatus.PENDING -> "검수 중"
        NicknameStatus.REJECTED -> "반려됨"
        // 복원 중 선점 충돌. 로그인 직후 재설정을 강제하므로 마이 홈까지 오는 경우는 없지만,
        // 상태값이 존재하는 이상 뱃지로 이유를 알려 준다.
        NicknameStatus.CONFLICT -> "변경 필요"
        NicknameStatus.APPROVED -> null
    }

/**
 * 티어 요약 (Figma 1134:1380).
 *
 * 표시 티어를 쓴다 — 유예 밴드에서는 실제 티어보다 높고, 방 입장 판정도 이 값이라
 * 화면이 다른 값을 보여 주면 "들어갈 수 있는 방"이 어긋난다.
 */
@Composable
private fun TierCard(
    home: MyHome,
    onDetail: () -> Unit,
) {
    val tier = home.displayTier
    val next = Tier.entries.getOrNull(tier.ordinal + 1)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .singleClickable(onClick = onDetail)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RuleUpTheme.colors.surfaceVariant)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = tier.label,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.smallBold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = home.score.thousandsLabel(),
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.numberM,
                )
                Text(
                    text = "점",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.caption,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "자세히",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.smallBold,
            )
        }
        RuleUpProgressBar(progress = tierProgress(home))
        if (next != null) {
            Row {
                Text(
                    text = "${next.label}까지 ",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
                Text(
                    text = "${(next.minScore - home.score).coerceAtLeast(0).thousandsLabel()}점",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.captionBold,
                )
            }
        }
    }
}

/**
 * 표시 티어 구간 안에서의 진행률.
 *
 * 상세 화면은 서버가 준 값으로 그리지만 `GET /me/home` 은 구간 경계를 내려주지 않아 여기서만
 * 티어 표로 계산한다. 유예 밴드에서 음수가 되므로 0 으로 자른다.
 */
private fun tierProgress(home: MyHome): Float {
    val tier = home.displayTier
    val span = tier.maxScore - tier.minScore
    if (span <= 0) return 1f
    return ((home.score - tier.minScore).toFloat() / span).coerceIn(0f, 1f)
}

/** 진행 중 · 완료 · 전체 성공률 (Figma 1134:1394). 성공률만 `/me/stats` 에서 온다. */
@Composable
private fun CountsRow(
    home: MyHome,
    stats: StatsReport?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountCell(
            value = "${home.counts.inProgress}",
            label = "진행 중",
            valueColor = RuleUpTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        CellDivider()
        CountCell(
            value = "${home.counts.completed}",
            label = "완료",
            valueColor = RuleUpTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        CellDivider()
        CountCell(
            // 아직 안 왔거나 표본이 없으면 "—". 0% 로 접으면 전부 실패한 것처럼 보인다.
            value = stats?.successRate?.let { "${(it * 100).toInt()}%" } ?: "—",
            label = "전체 성공률",
            valueColor = RuleUpTheme.colors.brand,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CellDivider() {
    Box(
        modifier =
            Modifier
                .width(1.dp)
                .height(28.dp)
                .background(RuleUpTheme.colors.border),
    )
}

@Composable
private fun CountCell(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.tinyMedium)
        Text(text = value, color = valueColor, style = RuleUpTheme.typography.title)
    }
}

/** 기록 메뉴 (Figma 1134:1408). */
@Composable
private fun RecordMenuCard(onIntent: (MyHomeIntent) -> Unit) {
    MenuCard {
        MenuRow(label = "인증 기록") { onIntent(MyHomeIntent.OpenCalendar) }
        MenuDivider()
        // Figma 의 "이번 달 2회 남음"은 그리지 않는다 — 이의 횟수 한도가 폐기됐다(챌린지 정책 §7.2).
        MenuRow(label = "이의 내역") { onIntent(MyHomeIntent.OpenAppeals) }
        MenuDivider()
        MenuRow(label = "감시자", trailing = "챌린지별 설정") { onIntent(MyHomeIntent.OpenWatchers) }
    }
}

/** 계정 메뉴 (Figma 1134:1423). */
@Composable
private fun AccountMenuCard(onIntent: (MyHomeIntent) -> Unit) {
    MenuCard {
        MenuRow(label = "알림 설정") { onIntent(MyHomeIntent.OpenNotificationSettings) }
        MenuDivider()
        MenuRow(label = "계정 · 약관") { onIntent(MyHomeIntent.OpenSettings) }
    }
}

/**
 * Figma 최종안에 자리가 없는 기존 화면들.
 *
 * 통계·그룹 랭킹·친구 초대·차단 목록은 이미 동작하는 화면이라, 디자인에서 빠졌다는 이유만으로
 * 진입점을 지우면 도달할 방법이 사라진다. 자리를 옮길지는 디자인 확인 후 정한다.
 */
@Composable
private fun ExtraMenuCard(onIntent: (MyHomeIntent) -> Unit) {
    MenuCard {
        MenuRow(label = "통계") { onIntent(MyHomeIntent.OpenStats) }
        MenuDivider()
        MenuRow(label = "그룹 랭킹") { onIntent(MyHomeIntent.OpenRanking) }
        MenuDivider()
        MenuRow(label = "친구 초대") { onIntent(MyHomeIntent.OpenInvite) }
        MenuDivider()
        MenuRow(label = "신고한 사용자 · 챌린지") { onIntent(MyHomeIntent.OpenBlocks) }
    }
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(color = RuleUpTheme.colors.border)
}

@Composable
private fun MenuRow(
    label: String,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .singleClickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(
            text = "›",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.section,
        )
    }
}
