package com.ruleup.challenge.presentation.detail

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeCalendarDay
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.presentation.common.VerificationAccessSheet
import com.ruleup.challenge.presentation.common.capacityLabel
import com.ruleup.challenge.presentation.common.rememberVerificationPermissionRequester
import com.ruleup.challenge.presentation.detail.component.AppealSheet
import com.ruleup.challenge.presentation.detail.component.AppealTarget
import com.ruleup.challenge.presentation.detail.component.ChallengeCoverBackground
import com.ruleup.challenge.presentation.detail.component.MySetupCard
import com.ruleup.challenge.presentation.detail.component.ReportDoneSheet
import com.ruleup.challenge.presentation.detail.component.ReportReasonSheet
import com.ruleup.challenge.presentation.detail.component.RoomAppBar
import com.ruleup.challenge.presentation.detail.component.RoomFeedTab
import com.ruleup.challenge.presentation.detail.component.RoomInfoHeader
import com.ruleup.challenge.presentation.detail.component.RoomInfoTab
import com.ruleup.challenge.presentation.detail.component.RoomMemberSection
import com.ruleup.challenge.presentation.detail.component.RoomMenuItem
import com.ruleup.challenge.presentation.detail.component.RoomMuteSection
import com.ruleup.challenge.presentation.detail.component.RoomRankingTab
import com.ruleup.challenge.presentation.detail.component.RoomTabRow
import com.ruleup.challenge.presentation.detail.component.SoloMonthCalendar
import com.ruleup.challenge.presentation.detail.component.TodayVerificationCard
import com.ruleup.challenge.presentation.detail.component.VerificationResultModal
import com.ruleup.challenge.presentation.detail.component.WatcherSection
import com.ruleup.challenge.presentation.detail.component.toAppealTarget
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailEffect
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailViewModel
import com.ruleup.challenge.presentation.detail.viewmodel.DetailSetupAction
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.challenge.presentation.invite.MemberInviteSharer
import com.ruleup.challenge.presentation.watcher.WatcherInviteSharer
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 챌린지 상세/참여 화면. */
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ChallengeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    val context = LocalContext.current
    val messageHelper = LocalMessageHelper.current
    val requestPermissions =
        rememberVerificationPermissionRequester {
            viewModel.onIntent(ChallengeDetailIntent.VerificationPermissionsReturned)
        }

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

                is ChallengeDetailEffect.ShareMemberInvite -> {
                    val shared =
                        MemberInviteSharer.share(
                            context = context,
                            challengeTitle = effect.challengeTitle,
                            inviteUrl = effect.inviteUrl,
                        )
                    if (!shared) messageHelper.showToast("카카오톡 공유를 열지 못했어요")
                }

                is ChallengeDetailEffect.ShowMessage -> messageHelper.showToast(effect.message)
                is ChallengeDetailEffect.RequestPermissions -> requestPermissions(effect.tokens)
            }
        }
    }

    // 앱 등록 화면 등에서 돌아오면 등록 상태를 재확인해 버튼 모드를 갱신한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ChallengeDetailIntent.RefreshSetup)
    }

    val setup = state.setup
    // 권한 현황은 저장하지 않고 매번 OS 에 다시 묻는다
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ChallengeDetailIntent.RefreshPermissions)
    }
    // 아직 못 물었으면(null) 막지 않는다
    val missingTokens = state.missingPermissionTokens()
    val permissionGranted = missingTokens.isEmpty()

    // 설정 순서: 권한 → 대상 앱 → 위치 → 참여.
    val action =
        when {
            state.detail == null || setup?.manual == true -> DetailSetupAction.JOIN
            !permissionGranted -> DetailSetupAction.GRANT_PERMISSION
            setup == null -> DetailSetupAction.JOIN
            setup.requiresTargetPackages && !state.targetAppsRegistered -> DetailSetupAction.REGISTER_APPS
            setup.requiresAnchors && !setup.anchorsConfigured -> DetailSetupAction.REGISTER_ANCHOR
            else -> DetailSetupAction.JOIN
        }
    // 심사 중에도 모집·입장에 제한이 없다
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
                    DetailSetupAction.GRANT_PERMISSION -> viewModel.onIntent(ChallengeDetailIntent.OpenVerificationAccess)
                    DetailSetupAction.REGISTER_APPS -> viewModel.onIntent(ChallengeDetailIntent.RegisterApps)
                    DetailSetupAction.REGISTER_ANCHOR -> viewModel.onIntent(ChallengeDetailIntent.RegisterAnchor)
                    DetailSetupAction.JOIN -> viewModel.onIntent(ChallengeDetailIntent.Proceed)
                }
            }
        },
    )
}

/** 서버가 요구한 권한 중 실제로 꺼져 있는 것. */
internal fun ChallengeDetailState.missingPermissionTokens(): List<String> {
    val snapshot = permissions ?: return emptyList()
    return requiredPermissionTokens().filter { snapshot.isGranted(it) == false }
}

private fun ChallengeDetailState.requiredPermissionTokens(): List<String> =
    setup?.requiredPermissions ?: detail
        ?.verification
        ?.requiredPermissions
        .orEmpty()

/** 위치 인증 챌린지인데 기기 위치(GPS)가 꺼져 있는가. */
internal fun ChallengeDetailState.locationServiceOff(): Boolean = permissions?.locationServiceOff(requiredPermissionTokens()) == true

private fun Context.openLocationSettings() {
    runCatching { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
}

@Composable
// 테스트에서 상태를 직접 넣어 렌더하려고 연다.
internal fun ChallengeDetailContent(
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
        val detail = state.detail
        val room = state.room
        // 솔로 상세의 이의 시트.
        var soloAppeal by remember { mutableStateOf<SoloAppeal?>(null) }
        val soloAppealImagePicker =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { onIntent(ChallengeDetailIntent.PickAppealImage(it.toString())) }
            }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            // 참여 중인 그룹 방이면 방 이름이 제목이고 관리 동작은 ⋯ 로 모은다.
            if (detail != null && room != null) {
                RoomAppBar(
                    title = detail.title,
                    menuItems = roomMenuItems(room.myRole, detail.penalties?.watcher == true, onIntent),
                    onBack = onBack,
                )
            } else if (detail != null && detail.myRole.isMember) {
                // 솔로 방·시작 전 방은 room 이 오지 않는다.
                RoomAppBar(
                    title = detail.title,
                    menuItems =
                        roomMenuItems(detail.myRole, detail.penalties?.watcher == true, onIntent) +
                            RoomMenuItem("챌린지 나가기") { confirmAction = MemberConfirm.LEAVE },
                    onBack = onBack,
                )
            } else {
                // 비멤버도 ⋯ 를 갖는다
                RoomAppBar(
                    title = detail?.title ?: "챌린지",
                    menuItems = listOf(RoomMenuItem("챌린지 신고") { onIntent(ChallengeDetailIntent.OpenReport) }),
                    onBack = onBack,
                )
            }

            // 참여 중인데 필요한 권한이 끊겼으면 배너로 알린다.
            if (!state.isLoading && detail?.myRole?.isMember == true && state.missingPermissionTokens().isNotEmpty()) {
                Text(
                    text = "인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기",
                    color = RuleUpTheme.colors.danger,
                    style = RuleUpTheme.typography.caption,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(RuleUpTheme.colors.dangerContainer)
                            .singleClickable { onIntent(ChallengeDetailIntent.OpenPermissionRepair) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            // 권한은 있어도 기기 위치가 꺼져 있으면 장소 신호가 모이지 않아 실패로 판정될 수 있다.
            if (!state.isLoading && detail?.myRole?.isMember == true && state.locationServiceOff()) {
                val context = LocalContext.current
                Text(
                    text = "휴대폰 위치(GPS)가 꺼져 있어 장소 인증이 되지 않아요 · 위치 켜기",
                    color = RuleUpTheme.colors.danger,
                    style = RuleUpTheme.typography.caption,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(RuleUpTheme.colors.dangerContainer)
                            .singleClickable { context.openLocationSettings() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            // 미확인 판정은 로딩이 끝난 뒤에 올린다
            val unacknowledged = state.todayResult?.takeIf { !state.isLoading && !state.resultAcknowledged && it.unacknowledged != null }
            if (unacknowledged != null) {
                VerificationResultModal(
                    today = unacknowledged,
                    onConfirm = { onIntent(ChallengeDetailIntent.AcknowledgeResult) },
                )
            }

            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                    }

                detail == null ->
                    Box(Modifier.fillMaxSize().ttiContentDrawn(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.errorMessage ?: "챌린지를 불러오지 못했어요",
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }

                // 방 상세(3탭).
                room != null ->
                    RoomDetailTabs(
                        state = state,
                        detail = detail,
                        room = room,
                        onIntent = onIntent,
                        onConfirmLeave = { confirmAction = MemberConfirm.LEAVE },
                    )

                else ->
                    PublicDetailBody(
                        state = state,
                        detail = detail,
                        onIntent = onIntent,
                        onOpenTodayAppeal = { soloAppeal = SoloAppeal.Today },
                        onOpenDayAppeal = { day -> soloAppeal = SoloAppeal.Day(day) },
                    )
            }
        }

        // 하단 고정 CTA.
        if (state.detail != null && !state.detail.myRole.isMember) {
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
                // 복제는 공개 그룹만 가능하다
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

        soloAppeal?.let { pending ->
            val target = pending.target(state.todayResult)
            // 낼 대상(verificationId)이 없으면 시트를 열지 않는다
            if (target == null) {
                soloAppeal = null
            } else {
                AppealSheet(
                    target = target,
                    submitting = state.isSubmittingAppeal,
                    imageUrl = state.appealImageUrl,
                    uploadingImage = state.isUploadingAppealImage,
                    reasonError = state.appealReasonError,
                    onPickImage = {
                        soloAppealImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onSubmit = { reason ->
                        soloAppeal = null
                        onIntent(ChallengeDetailIntent.SubmitAppeal(target.verificationId, reason))
                    },
                    onDismiss = {
                        soloAppeal = null
                        onIntent(ChallengeDetailIntent.DismissAppeal)
                    },
                )
            }
        }

        // 연결 실패는 화면에 남겨 둔다
        if (state.joinRetryable) {
            JoinRetrySnackbar(
                onRetry = { onIntent(ChallengeDetailIntent.RetryJoin) },
                onDismiss = { onIntent(ChallengeDetailIntent.DismissJoinRetry) },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 88.dp),
            )
        }
    }

    if (state.isReportSheetOpen) {
        val result = state.reportResult
        if (result == null) {
            val userReport = state.reportUserId != null
            ReportReasonSheet(
                title = if (userReport) "이 사용자를 신고할까요?" else "이 챌린지를 신고할까요?",
                // 부정 인증 의심은 사람의 행위라 사용자 신고에만 있다.
                reasons = if (userReport) ReportReason.forUser else ReportReason.forChallenge,
                selected = state.selectedReportReason,
                submitting = state.isSubmittingReport,
                onSelect = { onIntent(ChallengeDetailIntent.SelectReportReason(it)) },
                onSubmit = { onIntent(ChallengeDetailIntent.SubmitReport) },
                onDismiss = { onIntent(ChallengeDetailIntent.DismissReport) },
            )
        } else {
            ReportDoneSheet(
                effectMessage = result.hiddenEffect.doneMessage(),
                onDismiss = { onIntent(ChallengeDetailIntent.DismissReport) },
            )
        }
    }

    state.pendingAccess?.let { access ->
        VerificationAccessSheet(
            access = access,
            isSubmitting = state.isAccessSubmitting,
            onContinue = { onIntent(ChallengeDetailIntent.ConfirmVerificationAccess) },
            onDismiss = { onIntent(ChallengeDetailIntent.DismissVerificationAccess) },
        )
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
            capacity = state.detail?.capacity,
            onAction = { onIntent(ChallengeDetailIntent.FollowJoinBlockAction) },
            onDismiss = { onIntent(ChallengeDetailIntent.DismissJoinBlock) },
        )
    }

    when (confirmAction) {
        MemberConfirm.LEAVE ->
            MemberConfirmDialog(
                title = "챌린지에서 나갈까요?",
                body = "나가면 1주 동안 이 챌린지에 다시 참여할 수 없어요. 진행 이력이 있으면 탈퇴 패널티가 적용될 수 있어요.",
                confirmLabel = "나가기",
                onConfirm = {
                    confirmAction = null
                    onIntent(ChallengeDetailIntent.LeaveChallenge)
                },
                onDismiss = { confirmAction = null },
            )

        null -> Unit
    }
}

/** 방 상세 3탭. */
@Composable
private fun RoomDetailTabs(
    state: ChallengeDetailState,
    detail: ChallengeDetail,
    room: ChallengeRoom,
    onIntent: (ChallengeDetailIntent) -> Unit,
    onConfirmLeave: () -> Unit,
) {
    // 이의 증빙 사진 선택.
    val appealImagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onIntent(ChallengeDetailIntent.PickAppealImage(it.toString())) }
        }
    // 캘린더에서 고른 지난 건.
    var calendarAppeal by remember { mutableStateOf<ChallengeCalendarDay?>(null) }
    Column(modifier = Modifier.fillMaxSize().ttiContentDrawn()) {
        if (state.selectedTab == RoomTab.INFO) {
            RoomInfoHeader(
                categoryLabel = detail.category?.label,
                imageUrl = detail.imageUrl,
                remainingDays = room.summary.remainingDays,
                myProgressRate = state.myProgressRate,
            )
        }
        RoomTabRow(
            selected = state.selectedTab,
            onSelect = { onIntent(ChallengeDetailIntent.SelectTab(it)) },
        )

        when (state.selectedTab) {
            RoomTab.INFO ->
                RoomInfoTab(
                    detail = detail,
                    room = room,
                    today = state.todayResult,
                    // 등록할 게 있는 인증 방식일 때만 진입점을 만든다
                    onRegisterApps =
                        { onIntent(ChallengeDetailIntent.RegisterApps) }
                            .takeIf { state.setup?.requiresTargetPackages == true },
                    onRegisterAnchor =
                        { onIntent(ChallengeDetailIntent.RegisterAnchor) }
                            .takeIf { state.setup?.requiresAnchors == true },
                    onSubmitAppeal = { id, reason -> onIntent(ChallengeDetailIntent.SubmitAppeal(id, reason)) },
                    onOpenPermissionRepair = { onIntent(ChallengeDetailIntent.OpenPermissionRepair) },
                    isSubmittingAppeal = state.isSubmittingAppeal,
                    appealImageUrl = state.appealImageUrl,
                    isUploadingAppealImage = state.isUploadingAppealImage,
                    appealReasonError = state.appealReasonError,
                    onPickAppealImage = {
                        appealImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onDismissAppeal = { onIntent(ChallengeDetailIntent.DismissAppeal) },
                    // 수동 방에서만 체크 CTA 를 넘긴다
                    onOpenManualCheck =
                        { onIntent(ChallengeDetailIntent.OpenManualCheck) }
                            .takeIf { detail.manualCheckable },
                    extraSections = {
                        // 상태를 모르면 그리지 않는다
                        state.isMuted?.let { muted ->
                            RoomMuteSection(
                                muted = muted,
                                enabled = !state.isMuteSubmitting,
                                onToggle = { onIntent(ChallengeDetailIntent.ToggleMute(it)) },
                            )
                        }
                        // 솔로 방에만 월 캘린더를 편다
                        if (!detail.mode.isGroup) {
                            SoloMonthCalendar(
                                month = state.calendarMonth.orEmpty(),
                                calendar = state.calendar,
                                isLoading = state.isCalendarLoading,
                                // 지난 건은 여기가 유일한 이의 진입점이다
                                onAppealDay = { day -> calendarAppeal = day },
                                onPrevMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(-1)) },
                                onNextMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(1)) },
                            )
                        }
                        val myWatchers = state.watchers
                        if (myWatchers != null && detail.penalties?.watcher == true) {
                            WatcherSection(
                                watchers = myWatchers.watchers,
                                limit = myWatchers.limit,
                                isInviting = state.isInvitingWatcher,
                                onInvite = { onIntent(ChallengeDetailIntent.InviteWatcher) },
                            )
                        }
                        val members = state.members
                        if (members != null) {
                            RoomMemberSection(
                                members = members.members,
                                participantCount = members.participantCount,
                                maxParticipants = members.capacity,
                                myUserId = state.myUserId,
                                actionEnabled = !state.isMemberActionLoading,
                                // 초대 링크 발급은 비공개 그룹 방의 방장만 된다(서버도 같은 조건으로 막는다).
                                canInviteMember =
                                    room.myRole.isOwner &&
                                        state.detail

                                            ?.visibility
                                            ?.isPrivate == true &&
                                        state.detail.mode.isGroup,
                                onInviteMember = { onIntent(ChallengeDetailIntent.InviteMember) },
                                onLeave = onConfirmLeave,
                                onReportMember = { onIntent(ChallengeDetailIntent.OpenUserReport(it)) },
                                onOpenProfile = { onIntent(ChallengeDetailIntent.OpenMemberProfile(it)) },
                            )
                        }
                    },
                )

            RoomTab.FEED ->
                RoomFeedTab(
                    state = state,
                    onLoadMore = { onIntent(ChallengeDetailIntent.LoadMoreThreads) },
                    onRetry = { onIntent(ChallengeDetailIntent.RetryThreads) },
                )

            RoomTab.RANKING ->
                RoomRankingTab(
                    state = state,
                    onSelectScope = { onIntent(ChallengeDetailIntent.SelectRankingScope(it)) },
                    onLoadMoreCross = { onIntent(ChallengeDetailIntent.LoadMoreCrossRanking) },
                )
        }
    }

    calendarAppeal?.let { day ->
        val verificationId = day.verificationId
        if (verificationId == null) {
            calendarAppeal = null
        } else {
            AppealSheet(
                target =
                    AppealTarget(
                        verificationId = verificationId,
                        date = day.date,
                        // 캘린더는 사유·기한을 주지 않는다.
                        failureReason = null,
                        eligibleUntil = null,
                    ),
                submitting = state.isSubmittingAppeal,
                imageUrl = state.appealImageUrl,
                uploadingImage = state.isUploadingAppealImage,
                reasonError = state.appealReasonError,
                onPickImage = {
                    appealImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onSubmit = { reason ->
                    calendarAppeal = null
                    onIntent(ChallengeDetailIntent.SubmitAppeal(verificationId, reason))
                },
                onDismiss = {
                    calendarAppeal = null
                    onIntent(ChallengeDetailIntent.DismissAppeal)
                },
            )
        }
    }
}

/** 솔로 상세에서 연 이의 시트의 대상. */
private sealed interface SoloAppeal {
    data object Today : SoloAppeal

    data class Day(
        val day: ChallengeCalendarDay,
    ) : SoloAppeal

    fun target(today: TodayResult?): AppealTarget? =
        when (this) {
            Today -> today?.toAppealTarget()
            is Day ->
                day.verificationId?.let {
                    AppealTarget(verificationId = it, date = day.date, failureReason = null, eligibleUntil = null)
                }
        }
}

/** 공지는 제품에서 빠져 진입점을 두지 않는다 */
private fun roomMenuItems(
    myRole: MemberRole,
    // 감시자 벌칙이 켜진 챌린지만 감시자를 둘 수 있다
    watcherEnabled: Boolean,
    onIntent: (ChallengeDetailIntent) -> Unit,
): List<RoomMenuItem> =
    buildList {
        // 확인 대기함은 없앴다
        if (myRole.isOwner) {
            add(RoomMenuItem("챌린지 수정") { onIntent(ChallengeDetailIntent.OpenSettings) })
        }
        if (watcherEnabled) add(RoomMenuItem("감시자 등록") { onIntent(ChallengeDetailIntent.InviteWatcher) })
        if (!myRole.isOwner) add(RoomMenuItem("챌린지 신고") { onIntent(ChallengeDetailIntent.OpenReport) })
    }

private enum class MemberConfirm { LEAVE }

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

/** 비참여자가 보는 공개 상세 본문. */
@Composable
private fun PublicDetailBody(
    state: ChallengeDetailState,
    detail: ChallengeDetail,
    onIntent: (ChallengeDetailIntent) -> Unit,
    onOpenTodayAppeal: () -> Unit = {},
    onOpenDayAppeal: (ChallengeCalendarDay) -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 120.dp)
                .ttiContentDrawn(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DetailHero(detail)
        DetailInfoCard(detail)
        // 솔로 방은 방 홈(`/room`)이 내려오지 않아 방 정보 탭 전체가 없다.
        if (detail.myRole.isMember) {
            TodayVerificationCard(
                // 솔로는 room 이 없어 오늘 상태의 원천이 인증 모듈 응답 하나뿐이다.
                roomStatus = null,
                today = state.todayResult,
                onOpenManualCheck = { onIntent(ChallengeDetailIntent.OpenManualCheck) }.takeIf { detail.manualCheckable },
                onRegisterAnchor =
                    { onIntent(ChallengeDetailIntent.RegisterAnchor) }
                        .takeIf { state.setup?.requiresAnchors == true },
                onOpenPermissionRepair = { onIntent(ChallengeDetailIntent.OpenPermissionRepair) },
                // 이의는 서버가 낼 수 있다고 한 건에만, 대상 인증 건 ID 를 알 때만 낸다.
                onAppealClick =
                    { onOpenTodayAppeal() }
                        .takeIf { state.todayResult?.appeal?.eligible == true && state.todayResult.verificationId != null },
            )
            MySetupCard(
                onRegisterApps = { onIntent(ChallengeDetailIntent.RegisterApps) }.takeIf { state.setup?.requiresTargetPackages == true },
                onRegisterAnchor = { onIntent(ChallengeDetailIntent.RegisterAnchor) }.takeIf { state.setup?.requiresAnchors == true },
            )
        }
        // 솔로 수동 방의 유일한 인증 동선.
        if (detail.manualCheckable) {
            ManualCheckCard(
                checked = state.todayResult?.status == TodayResultStatus.DONE,
                onClick = { onIntent(ChallengeDetailIntent.OpenManualCheck) },
            )
        }
        // 지난 건은 캘린더가 유일한 이의 진입점이다
        if (detail.myRole.isMember) {
            SoloMonthCalendar(
                month = state.calendarMonth.orEmpty(),
                calendar = state.calendar,
                isLoading = state.isCalendarLoading,
                onAppealDay = onOpenDayAppeal,
                onPrevMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(-1)) },
                onNextMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(1)) },
            )
        }
        // 감시자는 챌린지 × 참여자 단위
        val myWatchers = state.watchers
        if (myWatchers != null && detail.penalties?.watcher == true) {
            WatcherSection(
                watchers = myWatchers.watchers,
                limit = myWatchers.limit,
                isInviting = state.isInvitingWatcher,
                onInvite = { onIntent(ChallengeDetailIntent.InviteWatcher) },
            )
        }
    }
}

/** 솔로 수동 방의 오늘 인증 진입 카드. */
@Composable
private fun ManualCheckCard(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (checked) "오늘 인증을 마쳤어요" else "오늘 인증이 아직 남았어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        RuleUpPrimaryButton(
            text = if (checked) "인증 수정" else "오늘 인증 체크",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DetailHero(detail: ChallengeDetail) {
    val accent = categoryAccentColor(detail.category)
    // 대표 사진이 있으면 카드 배경으로 깐다.
    ChallengeCoverBackground(
        imageUrl = detail.imageUrl,
        scrim = RuleUpTheme.colors.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
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
                // 장식용 글리프라 타입 스케일(최대 22)에 넣으면 확 줄어든다.
                Text(text = detail.category?.let(::categoryEmoji) ?: "🎯", fontSize = 26.sp)
            }
            Text(
                text = detail.title,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.title,
            )
            Text(
                // 방장이 나가면 봇이 자리를 지킨다(owner 가 null 이 된다).
                text = "${detail.ownerLabel()} · ${detail.participantCount}명 참여 중",
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
}

@Composable
private fun DetailInfoCard(detail: ChallengeDetail) {
    val method = if (detail.verification.type.isAuto) "자동 인증" else "직접 체크"
    val participation = if (detail.mode.isGroup) "그룹" else "솔로"

    RuleUpCard {
        InfoRow(label = "기간", value = "${detail.period.start} ~ ${detail.period.end}")
        InfoRow(label = "정원", value = "${detail.participantCount} / ${capacityLabel(detail.capacity)}")
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

/** "이 템플릿으로 만들기" */
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

/** 참여가 연결 문제로 실패했을 때의 스낵바. */
@Composable
private fun JoinRetrySnackbar(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 5초 뒤 스스로 걷힌다.
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) {
        delay(SNACKBAR_DURATION_MS)
        dismiss()
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.textPrimary, RuleUpTheme.shapes.medium)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "연결이 불안정해요",
            color = RuleUpTheme.colors.surface,
            style = RuleUpTheme.typography.small,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(
                text = "다시 시도",
                color = RuleUpTheme.colors.surface,
                style = RuleUpTheme.typography.smallBold,
            )
        }
    }
}

private const val SNACKBAR_DURATION_MS = 5_000L

/** 방장 표기. */
private fun ChallengeDetail.ownerLabel(): String = owner?.nickname ?: if (ownerType == OwnerType.BOT) "봇 방장" else "방장 없음"

/** 가입 차단 안내. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinBlockedSheet(
    block: JoinBlock,
    myTier: String?,
    requiredTier: String?,
    capacity: Int?,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, body) =
        when (block.reason) {
            JoinBlockReason.REJOIN_COOLDOWN ->
                "아직 다시 들어올 수 없어요" to
                    (block.rejoinAvailableAt?.take(10)?.let { "$it 부터 다시 참여할 수 있어요" } ?: "조금 뒤에 다시 시도해 주세요")

            JoinBlockReason.FREE_LIMIT ->
                "무료로는 3개까지 함께할 수 있어요" to "지금 3개에 참여 중이에요. 하나를 마치거나 정리하면 새로 시작할 수 있어요."

            JoinBlockReason.FULL ->
                "정원이 다 찼어요" to
                    (
                        capacity
                            ?.let { "이 방은 ${it}명이 모두 모였어요. 비슷한 챌린지를 둘러보세요." }
                            ?: "정원이 모두 찼어요. 비슷한 챌린지를 둘러보세요."
                    )

            JoinBlockReason.TIER_GATE ->
                // 조건을 나열하는 대신 "무엇부터 되는지"를 말한다
                (requiredTier?.let { "$it 티어부터 참여할 수 있어요" } ?: "티어 조건을 만족하지 않아요") to
                    (myTier?.let { "지금은 $it 예요. 내 티어에서 남은 점수를 볼 수 있어요." } ?: "내 티어를 확인해 주세요.")

            JoinBlockReason.BANNED ->
                "이 챌린지에는 참여할 수 없어요" to "자세한 내용은 안내드릴 수 없어요"

            JoinBlockReason.CHALLENGE_COMPLETED ->
                "이미 끝난 챌린지예요" to "비슷한 챌린지를 찾아볼까요?"

            // 모르는 사유다.
            else ->
                "지금은 참여할 수 없어요" to "다른 챌린지를 둘러보세요"
        }
    val actionLabel =
        when (block.reason) {
            JoinBlockReason.FREE_LIMIT -> "내 챌린지 관리"
            JoinBlockReason.TIER_GATE -> "내 티어 보기"
            JoinBlockReason.FULL -> "비슷한 챌린지 보기"
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

/** 서버가 내려준 가림 효과를 완료 문구로 옮긴다. */
private fun HiddenEffect?.doneMessage(): String =
    when (this) {
        HiddenEffect.USER_CONTENT_MASKED -> "이 사람의 글과 프로필이 임시 이름으로 가려졌어요."
        HiddenEffect.CHALLENGE_HIDDEN -> "이 챌린지가 탐색 목록에서 빠졌어요."
        HiddenEffect.CHALLENGE_MASKED -> "참여 중인 챌린지라 이름과 이미지만 가렸어요. 나가려면 방에서 직접 나가주세요."
        null -> "접수됐어요. 검토에 참고할게요."
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ChallengeDetailContentPreview() {
    RuleUpTheme {
        ChallengeDetailContent(
            state =
                com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState.initial.copy(
                    isLoading = false,
                    detail = com.ruleup.challenge.presentation.common.previewChallenge,
                ),
            ctaLabel = "미리보기",
            onIntent = {
            },
            onBack = { },
            onCta = { },
        )
    }
}
