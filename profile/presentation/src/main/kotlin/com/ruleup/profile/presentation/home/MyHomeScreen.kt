package com.ruleup.profile.presentation.home

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
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.presentation.common.trimLabel
import com.ruleup.profile.presentation.home.viewmodel.MyHomeEffect
import com.ruleup.profile.presentation.home.viewmodel.MyHomeIntent
import com.ruleup.profile.presentation.home.viewmodel.MyHomeViewModel
import com.ruleup.ui.helper.LocalMessageHelper

// 히어로 그라데이션 (피그마 434:257 — amber → rose → violet)
private val HeroGradient = listOf(Color(0xFFF59E0B), Color(0xFFF43F5E), Color(0xFF8B5CF6))
private val AvatarGradient = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300)

/** 마이 홈 (피그마 434:250 "내 프로필"). 하단 MY 탭의 루트 화면. */
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
                            .padding(bottom = 88.dp),
                ) {
                    MyHomeHero(
                        home = state.home!!,
                        onClick = { viewModel.onIntent(MyHomeIntent.OpenProfileEdit) },
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        MyHomeCountsRow(home = state.home!!)
                        MyHomeMenuCard(onIntent = viewModel::onIntent)
                    }
                }
        }

        RuleUpBottomTabBar(
            selected = RuleUpBottomTab.MY,
            onTabClick = { tab ->
                when (tab) {
                    RuleUpBottomTab.HOME -> viewModel.onIntent(MyHomeIntent.OpenHomeTab)
                    RuleUpBottomTab.EXPLORE -> viewModel.onIntent(MyHomeIntent.OpenChallengeTab)
                    // TODO(#269): "내 챌린지" 목적지 미정.
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
            onDismissRequest = { viewModel.onIntent(MyHomeIntent.DismissRankingPicker) },
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
                                        viewModel.onIntent(MyHomeIntent.SelectRankingChallenge(challenge.challengeId))
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
private fun MyHomeHero(
    home: MyHome,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(HeroGradient))
                .singleClickable(onClick = onClick)
                .statusBarsPadding()
                .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(AvatarGradient))
                    .border(4.dp, RuleUpPalette.BgSurface, CircleShape),
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
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${home.nickname}의 도전",
                color = RuleUpPalette.BgSurface,
                style = RuleUpTheme.typography.section,
            )
            nicknameBadgeLabel(home.nicknameStatus)?.let { badge ->
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RuleUpPalette.BgSurface.copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badge,
                        color = RuleUpPalette.BgSurface,
                        style = RuleUpTheme.typography.tinyBold,
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(RuleUpPalette.BgSurface.copy(alpha = 0.25f))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            Text(
                text = "🌡️ 매너 온도 ${home.mannerTemperature.trimLabel()}℃",
                color = RuleUpPalette.BgSurface,
                style = RuleUpTheme.typography.smallBold,
            )
        }
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

@Composable
private fun MyHomeCountsRow(home: MyHome) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountCard(
            value = home.counts.completed,
            label = "완주",
            valueColor = RuleUpTheme.colors.brand,
            modifier = Modifier.weight(1f),
        )
        CountCard(
            value = home.counts.inProgress,
            label = "진행 중",
            valueColor = RuleUpPalette.StatusWarn,
            modifier = Modifier.weight(1f),
        )
        CountCard(
            value = home.counts.groups,
            label = "그룹",
            valueColor = RuleUpTheme.colors.success,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CountCard(
    value: Int,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "$value",
            color = valueColor,
            style = RuleUpTheme.typography.title,
        )
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.tinyMedium,
        )
    }
}

@Composable
private fun MyHomeMenuCard(onIntent: (MyHomeIntent) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp)),
    ) {
        MenuRow(emoji = "🌡️", label = "매너 온도 상세") { onIntent(MyHomeIntent.OpenTemperature) }
        MenuDivider()
        MenuRow(emoji = "📅", label = "캘린더") { onIntent(MyHomeIntent.OpenCalendar) }
        MenuDivider()
        MenuRow(emoji = "🏆", label = "그룹 랭킹") { onIntent(MyHomeIntent.OpenRanking) }
        MenuDivider()
        MenuRow(emoji = "📊", label = "통계 리포트") { onIntent(MyHomeIntent.OpenStats) }
        MenuDivider()
        MenuRow(emoji = "👥", label = "친구 초대") { onIntent(MyHomeIntent.OpenInvite) }
        MenuDivider()
        MenuRow(emoji = "⚙️", label = "설정") { onIntent(MyHomeIntent.OpenSettings) }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(color = RuleUpTheme.colors.border)
}

@Composable
private fun MenuRow(
    emoji: String,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .singleClickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = RuleUpTheme.typography.labelMedium)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "›",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.section,
        )
    }
}
