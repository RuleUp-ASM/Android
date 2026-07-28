package com.ruleup.challenge.presentation.detail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.presentation.create.component.challengePermissionsGranted
import com.ruleup.challenge.presentation.create.component.rememberPermissionRequester
import com.ruleup.challenge.presentation.detail.component.RoomManageEntry
import com.ruleup.challenge.presentation.detail.component.RoomMemberSection
import com.ruleup.challenge.presentation.detail.component.RoomNoticeSection
import com.ruleup.challenge.presentation.detail.component.RoomRankingSection
import com.ruleup.challenge.presentation.detail.component.RoomSummaryRow
import com.ruleup.challenge.presentation.detail.component.RoomTodayStatusCard
import com.ruleup.challenge.presentation.detail.component.WatcherSection
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailEffect
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailViewModel
import com.ruleup.challenge.presentation.detail.viewmodel.DetailSetupAction
import com.ruleup.challenge.presentation.watcher.WatcherInviteSharer
import com.ruleup.ui.category.categoryAccentColor
import com.ruleup.ui.component.PrimaryGradientButton
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme
import kotlinx.coroutines.launch

/**
 * 챌린지 상세/참여 화면. 홈 카드 탭으로 진입한다.
 *
 * "참여하기" 를 누르면 자동 인증에 필요한 런타임 권한을 확인하고, 하나라도 미허용이면
 * 권한 허용 모달(바텀시트)을 띄운다. 모두 허용되면 좌표 바인딩 화면으로 이어진다.
 */
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ChallengeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messageHelper = LocalMessageHelper.current
    val permissionRequester = rememberPermissionRequester()
    val scope = rememberCoroutineScope()
    var showPermissionSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(challengeId) {
        viewModel.onIntent(ChallengeDetailIntent.Load(challengeId))
    }

    // 단발성 효과: 감시자 초대 카카오톡 공유(사용자 본인 발신) + 안내 토스트.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChallengeDetailEffect.ShareWatcherInvite -> {
                    val shared =
                        WatcherInviteSharer.share(
                            context = context,
                            card = effect.card,
                            inviteUrl = effect.inviteUrl,
                        )
                    if (!shared) messageHelper.showToast("카카오톡 공유를 열지 못했어요")
                }

                is ChallengeDetailEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    // 앱 등록 화면 등에서 돌아오면 등록 상태를 재확인해 버튼 모드를 갱신한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ChallengeDetailIntent.RefreshSetup)
    }

    val setup = state.setup
    // 권한 토큰은 GET setup 의 requiredPermissions 를 우선 사용(없으면 상세 verification 값).
    val tokens =
        setup?.requiredPermissions ?: state.detail
            ?.verification
            ?.requiredPermissions
            .orEmpty()
    val permissionGranted = challengePermissionsGranted(context, tokens)

    // GET setup 요구사항으로 "필요한 등록만" 유도: 권한 → (requiresTargetPackages) 앱 → (requiresAnchors) 지도 → 참여.
    // 수동 인증(manual)이거나 셋업 정보가 없으면 바로 참여.
    val action =
        when {
            state.detail == null || setup == null || setup.manual -> DetailSetupAction.JOIN
            !permissionGranted -> DetailSetupAction.GRANT_PERMISSION
            setup.requiresTargetPackages && !state.targetAppsRegistered -> DetailSetupAction.REGISTER_APPS
            setup.requiresAnchors && !setup.anchorsConfigured -> DetailSetupAction.REGISTER_ANCHOR
            else -> DetailSetupAction.JOIN
        }
    // 이미지 모더레이션 미통과(검수 중·거부)면 모집 차단 — CTA 를 안내로 바꾸고 진행을 막는다(명세).
    val recruitBlocked = state.detail?.moderationStatus?.blocksRecruit == true
    val ctaLabel =
        if (recruitBlocked) {
            "이미지 검수 중 · 모집 준비 중"
        } else {
            when (action) {
                DetailSetupAction.GRANT_PERMISSION -> "권한 허용하기"
                DetailSetupAction.REGISTER_APPS -> "앱 등록하기"
                DetailSetupAction.REGISTER_ANCHOR -> "인증 장소 등록하기"
                DetailSetupAction.JOIN -> "참여하기"
            }
        }

    ChallengeDetailContent(
        modifier = modifier,
        state = state,
        ctaLabel = ctaLabel,
        onIntent = viewModel::onIntent,
        onBack = { viewModel.onIntent(ChallengeDetailIntent.Back) },
        onCta = {
            if (recruitBlocked) {
                messageHelper.showToast("이미지 검수가 끝나면 모집이 시작돼요")
            } else {
                when (action) {
                    DetailSetupAction.GRANT_PERMISSION -> showPermissionSheet = true
                    DetailSetupAction.REGISTER_APPS -> viewModel.onIntent(ChallengeDetailIntent.RegisterApps)
                    DetailSetupAction.REGISTER_ANCHOR -> viewModel.onIntent(ChallengeDetailIntent.RegisterAnchor)
                    DetailSetupAction.JOIN -> viewModel.onIntent(ChallengeDetailIntent.Proceed)
                }
            }
        },
    )

    if (showPermissionSheet) {
        PermissionBottomSheet(
            tokens = tokens,
            onDismiss = { showPermissionSheet = false },
            onAllow = {
                scope.launch {
                    permissionRequester.request(tokens)
                    if (challengePermissionsGranted(context, tokens)) {
                        // 권한이 확보되면 시트를 닫는다. 버튼은 리컴포지션 시 권한 재확인으로 다음 단계로 전환된다.
                        showPermissionSheet = false
                    } else {
                        messageHelper.showToast("계속하려면 권한을 모두 허용해주세요")
                    }
                }
            },
        )
    }
}

@Composable
private fun ChallengeDetailContent(
    state: ChallengeDetailState,
    ctaLabel: String,
    onIntent: (ChallengeDetailIntent) -> Unit,
    onBack: () -> Unit,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmAction by remember { mutableStateOf<MemberConfirm?>(null) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            DetailTopBar(onBack = onBack)

            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                    }

                state.detail == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.errorMessage ?: "챌린지를 불러오지 못했어요",
                            color = RuleUpTheme.colors.textSecondary,
                            fontSize = 14.sp,
                        )
                    }

                else ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DetailHero(state.detail)

                        // 방 홈 (그룹 챌린지 ACTIVE 멤버): 요약·공지·랭킹·오늘 상태를 확장 렌더링.
                        // room == null(비멤버·솔로)이면 기존 공개 상세 그대로.
                        val room = state.room
                        if (room != null) {
                            RoomSummaryRow(summary = room.summary)
                            RoomNoticeSection(
                                pinnedNotice = room.pinnedNotice,
                                unreadCount = room.unreadNoticeCount,
                                onOpenNotices = { onIntent(ChallengeDetailIntent.OpenNotices) },
                                onOpenNotice = { onIntent(ChallengeDetailIntent.OpenNotice(it)) },
                            )
                            RoomRankingSection(
                                topRanking = room.topRanking,
                                onOpenRanking = { onIntent(ChallengeDetailIntent.OpenRanking) },
                            )
                            RoomTodayStatusCard(status = room.myTodayStatus)

                            // 방장·공동 관리자만: 확인 대기함(폴백 인증·이의 제기) 진입.
                            if (room.myRole.canManage) {
                                RoomManageEntry(
                                    label = "확인 대기함",
                                    onClick = { onIntent(ChallengeDetailIntent.OpenPendingReviews) },
                                )
                            }

                            val members = state.members
                            if (members != null) {
                                val delegationBanner =
                                    state.pendingDelegation?.let {
                                        "${state.pendingDelegationNickname ?: "선택한 멤버"}님에게 방장 위임을 요청했어요"
                                    }
                                RoomMemberSection(
                                    members = members.members,
                                    participantCount = members.participantCount,
                                    maxParticipants = members.maxParticipants,
                                    myRole = room.myRole,
                                    myUserId = state.myUserId,
                                    actionEnabled = !state.isMemberActionLoading,
                                    delegationBanner = delegationBanner,
                                    onLeave = { confirmAction = MemberConfirm.LEAVE },
                                    onDelete = { confirmAction = MemberConfirm.DELETE },
                                    onPromote = { onIntent(ChallengeDetailIntent.PromoteMember(it)) },
                                    onDemote = { onIntent(ChallengeDetailIntent.DemoteMember(it)) },
                                    onRequestDelegation = { onIntent(ChallengeDetailIntent.RequestDelegation(it)) },
                                    onCancelDelegation = { onIntent(ChallengeDetailIntent.CancelDelegation) },
                                )
                            }
                        }

                        DetailInfoCard(state.detail)
                        // 감시자는 챌린지 × 참여자 단위 — 내 감시자 조회가 성공한(=참여자) 경우에만 노출.
                        val myWatchers = state.watchers
                        if (myWatchers != null) {
                            WatcherSection(
                                watchers = myWatchers.watchers,
                                limit = myWatchers.limit,
                                isInviting = state.isInvitingWatcher,
                                onInvite = { onIntent(ChallengeDetailIntent.InviteWatcher) },
                                onRemove = { onIntent(ChallengeDetailIntent.RemoveWatcher(it)) },
                            )
                        }
                    }
            }
        }

        // 하단 고정 CTA. 상세 로딩 완료 후에만 활성화하고, 이미 참여 중인 방(멤버)에서는 숨긴다.
        if (state.detail != null && state.room == null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(RuleUpTheme.colors.surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                PrimaryGradientButton(
                    text = ctaLabel,
                    onClick = onCta,
                )
            }
        }
    }

    when (confirmAction) {
        MemberConfirm.LEAVE ->
            MemberConfirmDialog(
                title = "챌린지에서 나갈까요?",
                body = "나가면 이 챌린지에 다시 참여할 수 없어요. 진행 이력이 있으면 탈퇴 패널티가 적용될 수 있어요.",
                confirmLabel = "나가기",
                onConfirm = {
                    confirmAction = null
                    onIntent(ChallengeDetailIntent.LeaveChallenge)
                },
                onDismiss = { confirmAction = null },
            )

        MemberConfirm.DELETE ->
            MemberConfirmDialog(
                title = "챌린지를 삭제할까요?",
                body = "삭제하면 되돌릴 수 없어요. 진행 이력이 있으면 패널티가 적용될 수 있어요.",
                confirmLabel = "삭제",
                onConfirm = {
                    confirmAction = null
                    onIntent(ChallengeDetailIntent.DeleteChallenge)
                },
                onDismiss = { confirmAction = null },
            )

        null -> Unit
    }
}

/** 방 홈 멤버 섹션의 파괴적 액션 확인 대상. */
private enum class MemberConfirm { LEAVE, DELETE }

@Composable
private fun MemberConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RuleUpTheme.colors.surface,
        title = { Text(title, color = RuleUpTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(body, color = RuleUpTheme.colors.textSecondary, fontSize = 13.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = RuleUpTheme.colors.danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = RuleUpTheme.colors.textSecondary)
            }
        },
    )
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.ui.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "챌린지",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailHero(detail: ChallengeDetail) {
    val accent = categoryAccentColor(detail.category)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = detail.category?.emoji ?: "🎯", fontSize = 26.sp)
        }
        Text(
            text = detail.title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${detail.owner.nickname} · ${detail.stats.participantCount}명 참여 중",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 12.sp,
        )
        detail.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = RuleUpTheme.colors.textSlate,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun DetailInfoCard(detail: ChallengeDetail) {
    val method =
        when (detail.verification?.selectedMethod) {
            SelectedMethod.AUTO -> "자동 인증"
            SelectedMethod.MANUAL -> "수동 인증"
            null -> "수동 인증"
        }
    val participation = if (detail.participationType == ParticipationType.GROUP) "그룹" else "솔로"
    val repeat = detail.repeatDays.joinToString(" · ") { it.label }.ifBlank { "—" }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoRow(label = "기간", value = "${detail.durationDays}일")
        InfoRow(label = "반복", value = repeat)
        InfoRow(label = "참여 형태", value = participation)
        InfoRow(label = "인증 방식", value = method)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textMuted,
            fontSize = 13.sp,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionBottomSheet(
    tokens: List<String>,
    onDismiss: () -> Unit,
    onAllow: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RuleUpTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = "권한 허용이 필요해요",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "이 챌린지는 자동 인증을 위해 아래 권한이 필요해요.\n허용하면 참여를 이어갈 수 있어요.",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            tokens.distinct().forEach { token ->
                PermissionRow(label = permissionLabel(token))
            }
            Spacer(Modifier.height(20.dp))
            PrimaryGradientButton(
                text = "허용하기",
                onClick = onAllow,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .singleClickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "다음에",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.brand),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
    }
}

private fun permissionLabel(token: String): String =
    when (token.uppercase()) {
        "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> "위치 (자동 위치 인증)"
        "ACTIVITY_RECOGNITION", "PHYSICAL_ACTIVITY" -> "신체 활동"
        "CAMERA", "PHOTO" -> "카메라"
        "HEALTH", "HEALTH_CONNECT" -> "건강 데이터"
        "USAGE", "USAGE_STATS", "PACKAGE_USAGE_STATS" -> "사용 기록 접근"
        else -> token
    }
