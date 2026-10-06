package com.ruleup.challenge.presentation.detail

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeCalendarDay
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.presentation.common.VerificationAccessSheet
import com.ruleup.challenge.presentation.common.rememberVerificationPermissionRequester
import com.ruleup.challenge.presentation.detail.component.AppealSheet
import com.ruleup.challenge.presentation.detail.component.AppealTarget
import com.ruleup.challenge.presentation.detail.component.DetailCover
import com.ruleup.challenge.presentation.detail.component.DetailInfoPage
import com.ruleup.challenge.presentation.detail.component.ReportDoneSheet
import com.ruleup.challenge.presentation.detail.component.ReportReasonSheet
import com.ruleup.challenge.presentation.detail.component.RoomAppBar
import com.ruleup.challenge.presentation.detail.component.RoomContentSheet
import com.ruleup.challenge.presentation.detail.component.RoomCoverHeader
import com.ruleup.challenge.presentation.detail.component.RoomFeedTab
import com.ruleup.challenge.presentation.detail.component.RoomMemberSection
import com.ruleup.challenge.presentation.detail.component.RoomMenuItem
import com.ruleup.challenge.presentation.detail.component.RoomMenuSheet
import com.ruleup.challenge.presentation.detail.component.RoomPillTabs
import com.ruleup.challenge.presentation.detail.component.RoomRankingTab
import com.ruleup.challenge.presentation.detail.component.RoomSheetEntry
import com.ruleup.challenge.presentation.detail.component.SoloMonthCalendar
import com.ruleup.challenge.presentation.detail.component.SoloRoomBody
import com.ruleup.challenge.presentation.detail.component.TodayQuickActions
import com.ruleup.challenge.presentation.detail.component.TodayVerificationCard
import com.ruleup.challenge.presentation.detail.component.VerificationResultModal
import com.ruleup.challenge.presentation.detail.component.periodRange
import com.ruleup.challenge.presentation.detail.component.toAppealTarget
import com.ruleup.challenge.presentation.detail.component.toPercentText
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailEffect
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailViewModel
import com.ruleup.challenge.presentation.detail.viewmodel.DetailSetupAction
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.challenge.presentation.invite.MemberInviteSharer
import com.ruleup.challenge.presentation.watcher.WatcherInviteSharer
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.TodayResult
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
                DetailSetupAction.JOIN -> "가입하기"
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

/** 화면 단계. 모두 표지에서 시작해 상세 내용·방으로 들어간다(Figma 시안 C). */
private enum class DetailView { COVER, INFO, ROOM }

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
    var view by rememberSaveable { mutableStateOf(DetailView.COVER) }
    var appeal by remember { mutableStateOf<SoloAppeal?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }
    var membersOpen by remember { mutableStateOf(false) }
    val appealImagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onIntent(ChallengeDetailIntent.PickAppealImage(it.toString())) }
        }
    val detail = state.detail
    val room = state.room
    val isMember = detail?.myRole?.isMember == true

    // 이 화면에서 가입을 마쳤으면 바로 방으로 들어간다
    var wasMember by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isMember, detail != null) {
        if (detail == null) return@LaunchedEffect
        if (wasMember == false && isMember) view = DetailView.ROOM
        wasMember = isMember
    }
    // 가입 전에는 들어갈 방이 없다
    val shown = if (view == DetailView.ROOM && !isMember) DetailView.COVER else view
    BackHandler(enabled = shown != DetailView.COVER) { view = DetailView.COVER }

    // 표지·상세 내용의 오른쪽 버튼. 멤버는 방으로, 아니면 셋업 단계 → 가입.
    val primaryLabel: String? =
        when {
            detail == null -> null
            isMember -> "들어가기"
            state.hideJoinButton -> null
            state.isJoining -> "참여하는 중…"
            else -> ctaLabel
        }
    val onPrimary: () -> Unit = { if (isMember) view = DetailView.ROOM else onCta() }

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

            detail == null ->
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    RoomAppBar(title = "챌린지", menuItems = emptyList(), onBack = onBack)
                    Box(Modifier.fillMaxSize().ttiContentDrawn(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.errorMessage ?: "챌린지를 불러오지 못했어요",
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }
                }

            shown == DetailView.COVER ->
                DetailCover(
                    detail = detail,
                    members = state.members,
                    primaryLabel = primaryLabel,
                    primaryEnabled = !state.isJoining,
                    // 비공개 방은 초대 링크가 유일한 입장 경로라 참여 버튼 자체를 노출하지 않는다.
                    blockedNotice = "초대 링크로만 들어올 수 있는 챌린지예요".takeIf { !isMember && state.hideJoinButton },
                    menuItems =
                        if ((room?.myRole ?: detail.myRole).isOwner) {
                            listOf(RoomMenuItem("챌린지 수정") { onIntent(ChallengeDetailIntent.OpenSettings) })
                        } else {
                            listOf(RoomMenuItem("챌린지 신고") { onIntent(ChallengeDetailIntent.OpenReport) })
                        },
                    onPrimary = onPrimary,
                    onOpenInfo = { view = DetailView.INFO },
                    onBack = onBack,
                    modifier = Modifier.ttiContentDrawn(),
                )

            shown == DetailView.INFO ->
                DetailInfoPage(
                    detail = detail,
                    primaryLabel = primaryLabel,
                    primaryEnabled = !state.isJoining,
                    onPrimary = onPrimary,
                    onBack = { view = DetailView.COVER },
                    modifier = Modifier.ttiContentDrawn(),
                    // 복제는 공개 그룹만 가능하다
                    extraBottom =
                        if (!isMember && detail.cloneable) {
                            {
                                CloneButton(
                                    isCloning = state.isCloning,
                                    enabled = state.canClone,
                                    onClick = { onIntent(ChallengeDetailIntent.CloneChallenge) },
                                )
                            }
                        } else {
                            null
                        },
                )

            else ->
                RoomView(
                    state = state,
                    detail = detail,
                    room = room,
                    onIntent = onIntent,
                    onBack = { view = DetailView.COVER },
                    onOpenMenu = { menuOpen = true },
                    onOpenCalendar = { calendarOpen = true },
                    onOpenTodayAppeal = { appeal = SoloAppeal.Today },
                    onOpenDayAppeal = { day -> appeal = SoloAppeal.Day(day) },
                )
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

    // 미확인 판정은 로딩이 끝난 뒤에 올린다
    val unacknowledged = state.todayResult?.takeIf { !state.isLoading && !state.resultAcknowledged && it.unacknowledged != null }
    if (unacknowledged != null) {
        VerificationResultModal(
            today = unacknowledged,
            onConfirm = { onIntent(ChallengeDetailIntent.AcknowledgeResult) },
        )
    }

    if (menuOpen && detail != null) {
        RoomMenuSheet(
            entries =
                roomMenuEntries(
                    state = state,
                    detail = detail,
                    onIntent = onIntent,
                    onOpenCalendar = { calendarOpen = true },
                    onOpenInfo = { view = DetailView.INFO },
                    onOpenMembers = { membersOpen = true },
                    onLeave = { confirmAction = MemberConfirm.LEAVE },
                ),
            onDismiss = { menuOpen = false },
        )
    }

    if (calendarOpen) {
        RoomContentSheet(onDismiss = { calendarOpen = false }) {
            SoloMonthCalendar(
                month = state.calendarMonth.orEmpty(),
                calendar = state.calendar,
                isLoading = state.isCalendarLoading,
                // 지난 건은 캘린더가 유일한 이의 진입점이다
                onAppealDay = { day ->
                    calendarOpen = false
                    appeal = SoloAppeal.Day(day)
                },
                onPrevMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(-1)) },
                onNextMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(1)) },
            )
        }
    }

    val members = state.members
    if (membersOpen && members != null && room != null) {
        RoomContentSheet(onDismiss = { membersOpen = false }) {
            RoomMemberSection(
                members = members.members,
                participantCount = members.participantCount,
                maxParticipants = members.capacity,
                myUserId = state.myUserId,
                actionEnabled = !state.isMemberActionLoading,
                // 초대 링크 발급은 비공개 그룹 방의 방장만 된다(서버도 같은 조건으로 막는다).
                canInviteMember = room.myRole.isOwner && detail?.visibility?.isPrivate == true && detail.mode.isGroup,
                onInviteMember = { onIntent(ChallengeDetailIntent.InviteMember) },
                onLeave = {
                    membersOpen = false
                    confirmAction = MemberConfirm.LEAVE
                },
                onReportMember = { onIntent(ChallengeDetailIntent.OpenUserReport(it)) },
                onOpenProfile = { onIntent(ChallengeDetailIntent.OpenMemberProfile(it)) },
            )
        }
    }

    appeal?.let { pending ->
        val target = pending.target(state.todayResult)
        // 낼 대상(verificationId)이 없으면 시트를 열지 않는다
        if (target == null) {
            appeal = null
        } else {
            AppealSheet(
                target = target,
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
                    appeal = null
                    onIntent(ChallengeDetailIntent.SubmitAppeal(target.verificationId, reason))
                },
                onDismiss = {
                    appeal = null
                    onIntent(ChallengeDetailIntent.DismissAppeal)
                },
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

/**
 * 들어간 뒤 화면. 그룹은 피드·랭킹 2탭이고 피드 맨 위에 오늘 내 인증을 고정한다.
 * 솔로는 방 데이터·피드가 없어 오늘 인증과 캘린더를 한 화면에 둔다.
 */
@Composable
private fun RoomView(
    state: ChallengeDetailState,
    detail: ChallengeDetail,
    room: ChallengeRoom?,
    onIntent: (ChallengeDetailIntent) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTodayAppeal: () -> Unit,
    onOpenDayAppeal: (ChallengeCalendarDay) -> Unit,
) {
    val subtitle =
        if (room != null) {
            listOfNotNull(
                "D-${room.summary.remainingDays}",
                state.myProgressRate?.let { "내 달성률 ${it.toPercentText()}%" },
            ).joinToString(" · ")
        } else {
            detail.periodRange()
        }
    // 감시자 벌칙이 켜진 챌린지만 감시자를 둘 수 있다
    val watcherCount =
        state.watchers
            ?.takeIf { detail.penalties?.watcher == true }
            ?.watchers
            ?.count { it.status.isActive }
    val missingPermissions = state.missingPermissionTokens().isNotEmpty()
    val todayBlock: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TodayVerificationCard(
                // 솔로는 room 이 없어 오늘 상태의 원천이 인증 모듈 응답 하나뿐이다.
                roomStatus = room?.myTodayStatus,
                today = state.todayResult,
                // 이의는 서버가 낼 수 있다고 한 건에만, 대상 인증 건 ID 를 알 때만 낸다.
                onAppealClick =
                    onOpenTodayAppeal.takeIf {
                        state.todayResult?.appeal?.eligible == true && state.todayResult.verificationId != null
                    },
                onOpenManualCheck = { onIntent(ChallengeDetailIntent.OpenManualCheck) }.takeIf { detail.manualCheckable },
                onRegisterAnchor = { onIntent(ChallengeDetailIntent.RegisterAnchor) }.takeIf { state.setup?.requiresAnchors == true },
                onOpenPermissionRepair = { onIntent(ChallengeDetailIntent.OpenPermissionRepair) },
            )
            TodayQuickActions(
                onManualCheck = { onIntent(ChallengeDetailIntent.OpenManualCheck) }.takeIf { detail.manualCheckable },
                onPermissionRepair = { onIntent(ChallengeDetailIntent.OpenPermissionRepair) }.takeIf { missingPermissions },
                // 솔로는 캘린더를 본문에 바로 편다
                onCalendar = onOpenCalendar.takeIf { room != null },
            )
        }
    }
    Column(modifier = Modifier.fillMaxSize().ttiContentDrawn()) {
        RoomCoverHeader(
            detail = detail,
            subtitle = subtitle,
            watcherCount = watcherCount,
            onBack = onBack,
            onOpenMenu = onOpenMenu,
            onOpenWatchers = { onIntent(ChallengeDetailIntent.OpenWatchers) },
        )
        PermissionBanners(state = state, missingPermissions = missingPermissions, onIntent = onIntent)
        if (room != null) {
            RoomPillTabs(selected = state.selectedTab, onSelect = { onIntent(ChallengeDetailIntent.SelectTab(it)) })
            when (state.selectedTab) {
                RoomTab.FEED ->
                    RoomFeedTab(
                        state = state,
                        onLoadMore = { onIntent(ChallengeDetailIntent.LoadMoreThreads) },
                        onRetry = { onIntent(ChallengeDetailIntent.RetryThreads) },
                        header = todayBlock,
                    )

                RoomTab.RANKING ->
                    RoomRankingTab(
                        state = state,
                        onSelectScope = { onIntent(ChallengeDetailIntent.SelectRankingScope(it)) },
                        onLoadMoreCross = { onIntent(ChallengeDetailIntent.LoadMoreCrossRanking) },
                    )
            }
        } else {
            SoloRoomBody {
                todayBlock()
                SoloMonthCalendar(
                    month = state.calendarMonth.orEmpty(),
                    calendar = state.calendar,
                    isLoading = state.isCalendarLoading,
                    onAppealDay = onOpenDayAppeal,
                    onPrevMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(-1)) },
                    onNextMonth = { onIntent(ChallengeDetailIntent.ShiftCalendarMonth(1)) },
                )
            }
        }
    }
}

/** 참여 중인데 권한이 끊겼거나 기기 위치가 꺼졌으면 배너로 알린다. */
@Composable
private fun PermissionBanners(
    state: ChallengeDetailState,
    missingPermissions: Boolean,
    onIntent: (ChallengeDetailIntent) -> Unit,
) {
    Column {
        if (missingPermissions) {
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
        if (state.locationServiceOff()) {
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
    }
}

/** 방 ⋯ 메뉴. 가끔 쓰는 설정·기록·관리 동작을 모은다(공지는 제품에서 빠졌다). */
private fun roomMenuEntries(
    state: ChallengeDetailState,
    detail: ChallengeDetail,
    onIntent: (ChallengeDetailIntent) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenMembers: () -> Unit,
    onLeave: () -> Unit,
): List<RoomSheetEntry> =
    buildList {
        val myRole = state.room?.myRole ?: detail.myRole
        if (state.setup?.requiresTargetPackages == true) {
            add(RoomSheetEntry("대상 앱 설정") { onIntent(ChallengeDetailIntent.RegisterApps) })
        }
        if (state.setup?.requiresAnchors == true) {
            add(RoomSheetEntry("인증 장소 설정") { onIntent(ChallengeDetailIntent.RegisterAnchor) })
        }
        // 솔로는 캘린더가 본문에 이미 있다
        if (state.room != null) add(RoomSheetEntry("캘린더 · 지난 기록", value = "이의 제기도 여기서", onClick = onOpenCalendar))
        if (detail.penalties?.watcher == true) {
            val watchers = state.watchers
            add(
                RoomSheetEntry(
                    "감시자 관리",
                    value = watchers?.let { w -> "${w.watchers.count { it.status.isActive }} / ${w.limit?.toString() ?: "무제한"}" },
                ) { onIntent(ChallengeDetailIntent.OpenWatchers) },
            )
        }
        // 상태를 모르면 그리지 않는다
        state.isMuted?.let { muted ->
            add(
                RoomSheetEntry(
                    "이 챌린지 알림 끄기",
                    toggle = muted,
                    toggleEnabled = !state.isMuteSubmitting,
                ) { onIntent(ChallengeDetailIntent.ToggleMute(!muted)) },
            )
        }
        add(RoomSheetEntry("상세 내용 보기", onClick = onOpenInfo))
        state.members
            ?.takeIf {
                state.room != null
            }?.let { add(RoomSheetEntry("멤버 보기", value = "${it.participantCount}명", onClick = onOpenMembers)) }
        if (myRole.isOwner) {
            add(RoomSheetEntry("챌린지 수정") { onIntent(ChallengeDetailIntent.OpenSettings) })
        } else {
            add(RoomSheetEntry("챌린지 신고") { onIntent(ChallengeDetailIntent.OpenReport) })
        }
        add(RoomSheetEntry("챌린지 나가기", danger = true, onClick = onLeave))
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
