package com.ruleup.challenge.presentation.detail

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.VerificationType
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
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.challenge.presentation.watcher.WatcherInviteSharer
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalMessageHelper
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
    // 심사 중에도 모집·입장에 제한이 없다 — 구 명세의 이미지 검수 모집 차단은 폐기됐다.
    val recruitBlocked = false
    val ctaLabel =
        if (recruitBlocked) {
            ""
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
                            style = RuleUpTheme.typography.labelMedium,
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

                            // 수정은 방장 전용이다 — 공동 관리자는 설정을 바꿀 수 없다.
                            if (room.myRole == MemberRole.OWNER) {
                                RoomManageEntry(
                                    label = "챌린지 수정",
                                    onClick = { onIntent(ChallengeDetailIntent.OpenSettings) },
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
                                    maxParticipants = members.capacity,
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
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(RuleUpTheme.colors.surface)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 복제는 공개 그룹만 가능하다 — 불가능하면 눌러보게 두지 않고 사전 비활성한다.
                if (state.detail.cloneable) {
                    CloneButton(
                        isCloning = state.isCloning,
                        enabled = state.canClone,
                        onClick = { onIntent(ChallengeDetailIntent.CloneChallenge) },
                    )
                }
                // 비공개 방은 초대 링크가 유일한 입장 경로라 참여 버튼 자체를 노출하지 않는다.
                if (state.hideJoinButton) {
                    Text(
                        text = "초대 링크로만 들어올 수 있는 챌린지예요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.small,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    RuleUpPrimaryButton(
                        text = if (state.isJoining) "참여하는 중…" else ctaLabel,
                        enabled = !state.isJoining,
                        onClick = onCta,
                    )
                }
            }
        }
    }

    state.joinBlock?.let { block ->
        JoinBlockedSheet(
            block = block,
            myTier =
                state.detail
                    ?.gate
                    ?.myDisplayTier
                    ?.value,
            requiredTier =
                state.detail
                    ?.gate
                    ?.minTier
                    ?.value,
            onAction = { onIntent(ChallengeDetailIntent.FollowJoinBlockAction) },
            onDismiss = { onIntent(ChallengeDetailIntent.DismissJoinBlock) },
        )
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
        text = { Text(body, color = RuleUpTheme.colors.textSecondary) },
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
    RuleUpTopBar(title = "챌린지", onBack = onBack)
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
            // 장식용 글리프라 타입 스케일(최대 22)에 넣으면 확 줄어든다. 그리는 크기로 잡는다.
            Text(text = detail.category?.let(::categoryEmoji) ?: "🎯", fontSize = 26.sp)
        }
        Text(
            text = detail.title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
        )
        Text(
            text = "${detail.owner?.nickname ?: "방장 없음"} · ${detail.participantCount}명 참여 중",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
        detail.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = RuleUpTheme.colors.textSlate,
                style = RuleUpTheme.typography.body,
            )
        }
    }
}

@Composable
private fun DetailInfoCard(detail: ChallengeDetail) {
    val method = if (detail.verification.type == VerificationType.AUTO) "자동 인증" else "직접 체크"
    val participation = if (detail.mode == ChallengeMode.GROUP) "그룹" else "솔로"

    RuleUpCard {
        InfoRow(label = "기간", value = "${detail.period.start} ~ ${detail.period.end}")
        InfoRow(label = "정원", value = "${detail.participantCount} / ${detail.capacity}명")
        InfoRow(label = "참여 형태", value = participation)
        InfoRow(label = "인증 방식", value = detail.verification.detail ?: method)
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
            style = RuleUpTheme.typography.body,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
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
                style = RuleUpTheme.typography.section,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "이 챌린지는 자동 인증을 위해 아래 권한이 필요해요.\n허용하면 참여를 이어갈 수 있어요.",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.body,
            )
            Spacer(Modifier.height(16.dp))
            tokens.distinct().forEach { token ->
                PermissionRow(label = permissionLabel(token))
            }
            Spacer(Modifier.height(20.dp))
            RuleUpPrimaryButton(
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
                    style = RuleUpTheme.typography.labelMedium,
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
            style = RuleUpTheme.typography.labelMedium,
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

/** "이 템플릿으로 만들기" — 복제 초안을 만들어 생성 확인 화면으로 보낸다. */
@Composable
private fun CloneButton(
    isCloning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(RuleUpTheme.colors.brandSoft)
                .singleClickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isCloning) "초안을 만드는 중…" else "이 템플릿으로 만들기",
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.cardTitle,
        )
    }
}

/**
 * 가입 차단 안내 (명세 409 `JOIN_BLOCKED` reason 8종).
 *
 * 사유마다 문구와 다음 행동이 다르다. **탈퇴·강퇴를 구분하는 문구는 쓰지 않고**(REJOIN_COOLDOWN),
 * 차단 사유도 설명하지 않는다(BANNED) — 둘 다 알려서 얻는 것보다 잃는 게 크다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinBlockedSheet(
    block: JoinBlock,
    myTier: String?,
    requiredTier: String?,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, body) =
        when (block.reason) {
            JoinBlockReason.REJOIN_COOLDOWN ->
                "아직 다시 들어올 수 없어요" to
                    (block.rejoinAvailableAt?.take(10)?.let { "$it 부터 다시 참여할 수 있어요" } ?: "조금 뒤에 다시 시도해 주세요")

            JoinBlockReason.FREE_LIMIT ->
                "동시에 3개까지 참여할 수 있어요" to "참여 중인 챌린지를 정리하면 새로 들어올 수 있어요"

            JoinBlockReason.FULL ->
                "정원이 찼어요" to "자리가 나면 다시 참여할 수 있어요"

            JoinBlockReason.TIER_GATE ->
                "티어 조건을 만족하지 않아요" to
                    "필요한 티어 ${requiredTier ?: "-"} · 내 티어 ${myTier ?: "-"}"

            JoinBlockReason.BANNED ->
                "이 챌린지에는 참여할 수 없어요" to "자세한 내용은 안내드릴 수 없어요"

            JoinBlockReason.CHALLENGE_COMPLETED ->
                "이미 끝난 챌린지예요" to "비슷한 챌린지를 찾아볼까요?"

            else ->
                "지금은 참여할 수 없어요" to "잠시 후 다시 시도해 주세요"
        }
    val actionLabel =
        when (block.reason) {
            JoinBlockReason.FREE_LIMIT -> "참여 중인 챌린지 보기"
            JoinBlockReason.TIER_GATE -> "내 티어 보기"
            JoinBlockReason.CHALLENGE_COMPLETED -> "다른 챌린지 찾기"
            else -> null
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RuleUpTheme.colors.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.section)
            Text(text = body, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
            Spacer(Modifier.height(6.dp))
            if (actionLabel != null) {
                RuleUpPrimaryButton(text = actionLabel, onClick = onAction)
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .singleClickable(onClick = onDismiss)
                        .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "닫기", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
