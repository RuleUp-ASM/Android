package com.ruleup.challenge.presentation.create.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DraftExpiredException
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.durationMinutes
import com.ruleup.challenge.domain.entity.toEntries
import com.ruleup.challenge.domain.logging.ChallengeEvents
import com.ruleup.challenge.domain.logging.CreateEntry
import com.ruleup.challenge.domain.logging.CreatePath
import com.ruleup.challenge.domain.logging.DraftField
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeTargetsPage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.logging.domain.BizLogger
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.VerificationAccess
import com.ruleup.verification.domain.navigation.VerificationPermissionRepairPage
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import com.ruleup.verification.domain.usecase.AgreeVerificationConsentUseCase
import com.ruleup.verification.domain.usecase.CheckVerificationAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject

/** 챌린지 생성 플로우 공유 ViewModel. */
@HiltViewModel
class CreateChallengeViewModel
    @Inject
    constructor(
        private val createChallengeUseCase: CreateChallengeUseCase,
        private val challengeRepository: ChallengeRepository,
        private val myChallengeStore: MyChallengeStore,
        private val navigationHelper: NavigationHelper,
        private val bizLogger: BizLogger,
        private val savedStateHandle: SavedStateHandle,
        private val permissionStatusProvider: PermissionStatusProvider,
        private val checkVerificationAccess: CheckVerificationAccessUseCase,
        private val agreeVerificationConsent: AgreeVerificationConsentUseCase,
        private val pendingDraft: com.ruleup.challenge.presentation.create.PendingChallengeDraft =
            com.ruleup.challenge.presentation.create
                .PendingChallengeDraft(),
    ) : MviViewModel<CreateChallengeIntent, CreateChallengeState, CreateChallengeReducerEvent, CreateChallengeEffect>(
            CreateChallengeState.initial,
        ) {
        init {
            savedStateHandle.get<String>(KEY_ROUTINE_DESCRIPTION)?.let {
                dispatch(CreateChallengeReducerEvent.RoutineDescriptionEntered(it))
            }
        }

        override fun onIntent(intent: CreateChallengeIntent) {
            when (intent) {
                CreateChallengeIntent.ConfirmOpened ->
                    pendingDraft.consume()?.let { draft ->
                        editedFields.clear()
                        dispatch(CreateChallengeReducerEvent.DraftReceived(draft, UUID.randomUUID().toString()))
                    }
                CreateChallengeIntent.Load -> {
                    // 생성 전환율의 분모.
                    if (!createStartLogged) {
                        createStartLogged = true
                        // TODO(entry): 라우트 인자 확정 후 생성 진입점 구분.
                        bizLogger.record(ChallengeEvents.createStart(CreateEntry.UNKNOWN))
                    }
                    loadTemplates()
                }

                CreateChallengeIntent.Exit -> exit()

                CreateChallengeIntent.RetryTemplates -> loadTemplates()

                is CreateChallengeIntent.SetRoutineDescription -> {
                    savedStateHandle[KEY_ROUTINE_DESCRIPTION] = intent.description
                    dispatch(CreateChallengeReducerEvent.RoutineDescriptionEntered(intent.description))
                }

                CreateChallengeIntent.SubmitDescription -> submitDescription()

                CreateChallengeIntent.CancelDrafting -> cancelDrafting()

                CreateChallengeIntent.DismissFallback ->
                    dispatch(CreateChallengeReducerEvent.FallbackDismissed)

                is CreateChallengeIntent.SelectTemplate -> selectTemplate(intent.templateId)

                is CreateChallengeIntent.SetTitle ->
                    dispatch(CreateChallengeReducerEvent.TitleEntered(intent.title))

                is CreateChallengeIntent.SetDescription ->
                    dispatch(CreateChallengeReducerEvent.DescriptionEntered(intent.description))

                is CreateChallengeIntent.SetCoverImage -> {
                    logDraftEdit(DraftField.IMAGE)
                    dispatch(CreateChallengeReducerEvent.CoverImageSelected(intent.uri))
                }

                is CreateChallengeIntent.SetMode -> {
                    if (intent.mode != currentState.original?.mode) logDraftEdit(DraftField.MODE)
                    dispatch(CreateChallengeReducerEvent.ModeSelected(intent.mode))
                }

                is CreateChallengeIntent.SetVisibility -> {
                    if (intent.visibility != currentState.original?.visibility) logDraftEdit(DraftField.VISIBILITY)
                    dispatch(CreateChallengeReducerEvent.VisibilitySelected(intent.visibility))
                }

                is CreateChallengeIntent.SetRankingVisible -> {
                    if (intent.visible != currentState.original?.rankingVisible) logDraftEdit(DraftField.RANKING_VISIBLE)
                    dispatch(CreateChallengeReducerEvent.RankingVisibleChanged(intent.visible))
                }

                is CreateChallengeIntent.SetCapacity -> {
                    if (intent.capacity != currentState.original?.capacity) logDraftEdit(DraftField.CAPACITY)
                    dispatch(CreateChallengeReducerEvent.CapacityChanged(intent.capacity))
                }

                is CreateChallengeIntent.SetMinTier -> {
                    if (intent.tier != currentState.original?.minTier) logDraftEdit(DraftField.MIN_TIER)
                    dispatch(CreateChallengeReducerEvent.MinTierChanged(intent.tier))
                }

                is CreateChallengeIntent.SetPeriod -> {
                    val origin = currentState.original?.period
                    if (intent.start != origin?.start || intent.end != origin.end) logDraftEdit(DraftField.PERIOD)
                    setPeriod(intent.start, intent.end)
                }

                is CreateChallengeIntent.SetWeeklyCount -> {
                    if (intent.count != currentState.original?.weeklyCount) logDraftEdit(DraftField.WEEKLY_COUNT)
                    dispatch(CreateChallengeReducerEvent.WeeklyCountChanged(intent.count))
                }

                is CreateChallengeIntent.EditParam -> {
                    val origin =
                        currentState.original

                            ?.params
                            ?.firstOrNull { it.key == intent.key }
                            ?.value
                    if (intent.value != origin) logDraftEdit(DraftField.PARAMS)
                    dispatch(CreateChallengeReducerEvent.ParamEdited(intent.key, intent.value))
                }

                is CreateChallengeIntent.SetVerificationType -> setVerificationType(intent.type)

                is CreateChallengeIntent.SetWatcherPenalty -> {
                    if (intent.enabled !=
                        currentState.original

                            ?.penalties
                            ?.watcher
                    ) {
                        logDraftEdit(DraftField.PENALTIES)
                    }
                    dispatch(CreateChallengeReducerEvent.WatcherPenaltyChanged(intent.enabled))
                }

                CreateChallengeIntent.VerificationPermissionsReturned ->
                    if (currentState.pendingAccess != null) checkAccessThenCreate()

                is CreateChallengeIntent.ConfirmTextEdit -> confirmTextEdit(intent.field)

                CreateChallengeIntent.Create -> create()
                CreateChallengeIntent.ConfirmVerificationAccess ->
                    if (currentState.pendingAccess != null) checkAccessThenCreate(agreeAndRequest = true)
                CreateChallengeIntent.DismissVerificationAccess ->
                    if (!currentState.isAccessSubmitting) dispatch(CreateChallengeReducerEvent.VerificationAccessRequested(null))
            }
        }

        override fun reduce(
            state: CreateChallengeState,
            event: CreateChallengeReducerEvent,
        ): CreateChallengeState =
            when (event) {
                CreateChallengeReducerEvent.DraftExpired -> CreateChallengeState.initial

                CreateChallengeReducerEvent.Reset -> CreateChallengeState.initial
                is CreateChallengeReducerEvent.RoutineDescriptionEntered ->
                    state.copy(
                        routineDescription = event.description.take(RoutineDescription.MAX_LENGTH),
                        // 다시 입력하기 시작하면 지난 폴백 안내는 치운다.
                        fallbackMessage = null,
                    )

                CreateChallengeReducerEvent.TemplatesLoading ->
                    state.copy(isLoadingTemplates = true, templatesFailed = false)

                is CreateChallengeReducerEvent.TemplatesLoaded ->
                    state.copy(
                        templates = event.templates,
                        isLoadingTemplates = false,
                        templatesFailed = false,
                    )

                CreateChallengeReducerEvent.TemplatesFailed ->
                    state.copy(isLoadingTemplates = false, templatesFailed = true)

                CreateChallengeReducerEvent.Drafting ->
                    state.copy(isDrafting = true, fallbackMessage = null)

                CreateChallengeReducerEvent.DraftFailed ->
                    state.copy(isDrafting = false)

                is CreateChallengeReducerEvent.DraftFellBack ->
                    // 입력은 그대로 둔다
                    state.copy(isDrafting = false, fallbackMessage = event.message)

                is CreateChallengeReducerEvent.DraftRateLimited ->
                    state.copy(isDrafting = false, retryAfterSeconds = event.retryAfterSeconds?.takeIf { it > 0 } ?: 60)

                CreateChallengeReducerEvent.RateLimitTicked ->
                    state.copy(retryAfterSeconds = (state.retryAfterSeconds ?: 0).minus(1).coerceAtLeast(0))

                CreateChallengeReducerEvent.RateLimitCleared ->
                    state.copy(retryAfterSeconds = null)

                CreateChallengeReducerEvent.FallbackDismissed ->
                    state.copy(fallbackMessage = null)

                is CreateChallengeReducerEvent.DraftReceived -> {
                    val draft = event.draft.draft
                    state.copy(
                        isDrafting = false,
                        fallbackMessage = null,
                        draftId = event.draft.draftId,
                        original = draft,
                        title = draft.title.take(CreateChallengeState.TITLE_MAX),
                        description = draft.description,
                        category = draft.category,
                        mode = draft.mode,
                        visibility = draft.visibility,
                        rankingVisible = draft.rankingVisible,
                        // 초안은 서버가 준 값이라 통제할 수 없다
                        capacity = ChallengeLimits.createCapacityStepAtLeast(draft.capacity),
                        minTier = draft.minTier,
                        // 상한은 초안이 준 기본값(= 생성자 표시 티어)으로 고정한다.
                        ownerTierCap = draft.minTier,
                        period = draft.period,
                        weeklyCount =
                            draft.weeklyCount.coerceIn(
                                ChallengeLimits.WEEKLY_COUNT_MIN,
                                ChallengeLimits.WEEKLY_COUNT_MAX,
                            ),
                        params = draft.params,
                        verification = draft.verification,
                        penalties = draft.penalties,
                        coverImageUri = null,
                        idempotencyKey = event.idempotencyKey,
                        pendingAccess = null,
                        isAccessSubmitting = false,
                        createdChallengeId = null,
                    )
                }

                is CreateChallengeReducerEvent.TitleEntered ->
                    state.copy(title = event.title.take(CreateChallengeState.TITLE_MAX))

                is CreateChallengeReducerEvent.DescriptionEntered ->
                    state.copy(description = event.description)

                is CreateChallengeReducerEvent.CoverImageSelected ->
                    state.copy(coverImageUri = event.uri)

                is CreateChallengeReducerEvent.ModeSelected ->
                    // 파생 필드는 서버가 정규화하지만, 화면이 엉뚱한 입력부를 열지 않도록 여기서도 맞춘다.
                    if (event.mode.isGroup) {
                        state.copy(
                            mode = event.mode,
                            visibility = state.visibility ?: ChallengeVisibility.PUBLIC,
                            rankingVisible = null,
                        )
                    } else {
                        state.copy(
                            mode = event.mode,
                            visibility = null,
                            rankingVisible = state.rankingVisible ?: true,
                        )
                    }

                is CreateChallengeReducerEvent.VisibilitySelected ->
                    state.copy(visibility = event.visibility)

                is CreateChallengeReducerEvent.RankingVisibleChanged ->
                    state.copy(rankingVisible = event.visible)

                // 경계에서는 ± 버튼이 눌리지 않으므로(ConfirmEditSheet) 범위 밖 값이 올라오지 않는다.
                is CreateChallengeReducerEvent.CapacityChanged ->
                    state.copy(capacity = event.capacity)

                is CreateChallengeReducerEvent.MinTierChanged ->
                    // 상한은 생성자 표시 티어
                    state.copy(
                        minTier =
                            state.ownerTierCap?.let { cap ->
                                if (event.tier.ordinal > cap.ordinal) cap else event.tier
                            } ?: event.tier,
                    )

                is CreateChallengeReducerEvent.PeriodChanged ->
                    state.copy(period = state.period.copy(start = event.start, end = event.end))

                // 슬라이더가 1~7 로 고정돼 있어 범위 밖 값이 올라오지 않는다.
                is CreateChallengeReducerEvent.WeeklyCountChanged ->
                    state.copy(weeklyCount = event.count)

                is CreateChallengeReducerEvent.ParamEdited ->
                    state.copy(
                        params = state.params.map { if (it.key == event.key) it.copy(value = event.value) else it },
                    )

                is CreateChallengeReducerEvent.VerificationTypeSelected ->
                    state.copy(
                        verification = state.verification?.copy(type = event.type),
                        // 인증 방식을 바꾸면 서버가 score 패널티를 재계산한다.
                        penalties = state.penalties.copy(score = event.type.isAuto),
                    )

                is CreateChallengeReducerEvent.WatcherPenaltyChanged ->
                    state.copy(penalties = state.penalties.copy(watcher = event.enabled))

                is CreateChallengeReducerEvent.VerificationAccessRequested -> state.copy(pendingAccess = event.access)
                is CreateChallengeReducerEvent.VerificationAccessSubmitting -> state.copy(isAccessSubmitting = event.submitting)

                CreateChallengeReducerEvent.Creating ->
                    state.copy(isCreating = true)

                CreateChallengeReducerEvent.CreateFailed ->
                    state.copy(isCreating = false)

                is CreateChallengeReducerEvent.Created ->
                    state.copy(isCreating = false, createdChallengeId = event.challengeId)
            }

        private fun loadTemplates() {
            if (currentState.isLoadingTemplates) return
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.TemplatesLoading)
                runCatching { challengeRepository.getRoutineTemplates() }
                    .onSuccess { dispatch(CreateChallengeReducerEvent.TemplatesLoaded(it)) }
                    // 추천이 실패해도 설명 입력 경로는 살아 있어야 하므로 화면 전체를 에러로 만들지 않는다.
                    .onFailure { dispatch(CreateChallengeReducerEvent.TemplatesFailed) }
            }
        }

        /** 경로 B */
        private fun submitDescription() {
            val state = currentState
            if (!state.canSubmitDescription) return
            bizLogger.record(ChallengeEvents.createPathSelect(CreatePath.PROMPT))
            draftJob =
                viewModelScope.launch {
                    dispatch(CreateChallengeReducerEvent.Drafting)
                    runCatching { challengeRepository.createDraft(RoutineDescription.of(state.routineDescription)) }
                        .onSuccess { result ->
                            when (result) {
                                is DraftResult.Ok -> applyDraft(result)
                                is DraftResult.Fallback ->
                                    dispatch(CreateChallengeReducerEvent.DraftFellBack(result.message))
                            }
                        }.onFailure { error ->
                            when (error) {
                                is RecommendationRateLimitedException -> {
                                    dispatch(CreateChallengeReducerEvent.DraftRateLimited(error.retryAfterSeconds))
                                    startRateLimitCountdown()
                                }

                                else -> {
                                    dispatch(CreateChallengeReducerEvent.DraftFailed)
                                    emitEffect(
                                        CreateChallengeEffect.ShowError(error.userFacingMessage("초안을 만들지 못했어요. 다시 시도해 주세요")),
                                    )
                                }
                            }
                        }
                }
        }

        /** 초안 생성은 최대 10초까지 걸릴 수 있어 화면을 잠근다 */
        private fun cancelDrafting() {
            if (!currentState.isDrafting) return
            draftJob?.cancel()
            dispatch(CreateChallengeReducerEvent.DraftFailed)
        }

        /** 남은 제한 시간을 1초씩 깎는다. */
        private fun startRateLimitCountdown() {
            countdownJob?.cancel()
            countdownJob =
                viewModelScope.launch {
                    while (true) {
                        val remaining = currentState.retryAfterSeconds ?: break
                        if (remaining <= 0) {
                            dispatch(CreateChallengeReducerEvent.RateLimitCleared)
                            break
                        }
                        delay(COUNTDOWN_TICK_MS)
                        dispatch(CreateChallengeReducerEvent.RateLimitTicked)
                    }
                }
        }

        /** 경로 A */
        private fun selectTemplate(templateId: Long) {
            if (currentState.isDrafting) return
            bizLogger.record(ChallengeEvents.createPathSelect(CreatePath.TEMPLATE))
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Drafting)
                runCatching { challengeRepository.createDraftFromTemplate(templateId) }
                    .onSuccess { applyDraft(it) }
                    .onFailure { error ->
                        dispatch(CreateChallengeReducerEvent.DraftFailed)
                        emitEffect(CreateChallengeEffect.ShowError(error.userFacingMessage("루틴 초안을 불러오지 못했어요")))
                    }
            }
        }

        /** 두 경로 공통 */
        private fun applyDraft(draft: DraftResult.Ok) {
            editedFields.clear()
            dispatch(
                CreateChallengeReducerEvent.DraftReceived(
                    draft = draft,
                    idempotencyKey = UUID.randomUUID().toString(),
                ),
            )
            navigationHelper.navigateTo(ChallengeConfirmPage)
        }

        private fun setPeriod(
            start: String,
            end: String,
        ) {
            if (!isValidPeriod(start, end)) {
                emitEffect(CreateChallengeEffect.ShowError("종료일은 시작일보다 뒤여야 해요"))
                return
            }
            dispatch(CreateChallengeReducerEvent.PeriodChanged(start, end))
        }

        private fun isValidPeriod(
            start: String,
            end: String,
        ): Boolean =
            try {
                !LocalDate.parse(end).isBefore(LocalDate.parse(start))
            } catch (_: DateTimeParseException) {
                false
            }

        /** 인증 방식 선택. */
        private fun setVerificationType(type: VerificationType) {
            val state = currentState
            if (type.isAuto && !state.canUseAuto) {
                emitEffect(CreateChallengeEffect.ShowError("이 루틴은 자동 인증을 쓸 수 없어요"))
                return
            }
            if (type !=
                state.original

                    ?.verification
                    ?.type
            ) {
                logDraftEdit(
                    DraftField.VERIFICATION,
                    autoToManual = !type.isAuto && state.canUseAuto,
                )
            }
            dispatch(CreateChallengeReducerEvent.VerificationTypeSelected(type))
        }

        /** 포커스가 빠진 시점에 원본과 비교한다. */
        private fun confirmTextEdit(field: TextEditField) {
            val state = currentState
            val origin = state.original ?: return
            when (field) {
                TextEditField.TITLE -> if (state.title != origin.title) logDraftEdit(DraftField.TITLE)
                TextEditField.DESCRIPTION ->
                    if (state.description != origin.description) logDraftEdit(DraftField.DESCRIPTION)
            }
        }

        private fun create() {
            val state = currentState
            if (state.isCreating || state.isAccessSubmitting) return
            if (state.createdChallengeId != null) {
                checkAccessThenCreate()
                return
            }

            val draftId = state.draftId ?: return
            val category =
                state.category ?: run {
                    emitEffect(CreateChallengeEffect.ShowError("카테고리를 분류하지 못했어요. 초안을 다시 만들어 주세요"))
                    return
                }
            val verification =
                state.verification ?: run {
                    emitEffect(CreateChallengeEffect.ShowError("인증 방식을 불러오지 못했어요. 초안을 다시 만들어 주세요"))
                    return
                }
            val idempotencyKey = state.idempotencyKey ?: return
            val access = checkedAccess
            if (access == null) {
                checkAccessThenCreate()
                return
            }
            // 통과 표시는 여기서 소비한다.
            checkedAccess = null

            val command =
                CreateChallengeCommand(
                    draftId = draftId,
                    title = state.title.trim(),
                    description = state.description.trim(),
                    category = category,
                    mode = state.mode,
                    visibility = state.visibility.takeIf { state.isGroup },
                    rankingVisible = state.rankingVisible.takeIf { !state.isGroup },
                    capacity = state.capacity.takeIf { state.isGroup },
                    minTier = state.minTier,
                    period = state.period,
                    weeklyCount = state.weeklyCount,
                    params = state.params.toEntries(),
                    verification = verification,
                    watcherPenalty = state.penalties.watcher,
                    imageUrl = null,
                )

            val coverImageUri = state.coverImageUri?.takeIf { it.isNotBlank() }
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Creating)
                runCatching {
                    // 이미지 업로드가 실패해도 생성은 막지 않는다
                    val imageUrl =
                        coverImageUri?.let { uri ->
                            runCatching { challengeRepository.uploadImage(uri) }
                                .onFailure { emitEffect(CreateChallengeEffect.ShowError("이미지 업로드에 실패해 기본 이미지로 만들어요")) }
                                .getOrNull()
                        }
                    createChallengeUseCase(command.copy(imageUrl = imageUrl), idempotencyKey)
                }.onSuccess { created ->
                    // 진행률 API 반영 전이라도 홈에 즉시 노출되도록 로컬 스토어에 반영한다.
                    myChallengeStore.add(
                        MyChallengeSummary(
                            challengeId = created.challengeId,
                            title = command.title,
                            category = category,
                            mode = state.mode,
                            durationDays = durationDays(state.period.start, state.period.end),
                        ),
                    )
                    dispatch(CreateChallengeReducerEvent.Created(created.challengeId))
                    lastCreated = created

                    // 생성 응답에서 요구 권한이 달라지면 같은 설정 시트에서 추가 항목을 확인한다.
                    val missing = created.verification.requiredPermissions.filter { access.permissions.isGranted(it) != true }
                    if (created.verification.type.isAuto && missing.isNotEmpty()) {
                        checkAccessThenCreate()
                    } else {
                        goAfterCreate(created)
                    }
                }.onFailure { error ->
                    dispatch(CreateChallengeReducerEvent.CreateFailed)
                    if (error is DraftExpiredException) {
                        checkedAccess = null
                        lastCreated = null
                        savedStateHandle.remove<String>(KEY_ROUTINE_DESCRIPTION)
                        dispatch(CreateChallengeReducerEvent.DraftExpired)
                        navigationHelper.navigateTo(com.ruleup.challenge.domain.navigation.ChallengeCreatePage)
                    }
                    val message =
                        when (error) {
                            is DraftExpiredException -> "초안이 만료됐어요. 처음부터 다시 만들어 주세요"
                            else -> error.userFacingMessage("챌린지 생성에 실패했어요")
                        }
                    emitEffect(CreateChallengeEffect.ShowError(message))
                }
            }
        }

        // 매 생성 시도마다 다시 확인한다
        private var checkedAccess: VerificationAccess? = null
        private var lastCreated: CreatedChallenge? = null

        private fun checkAccessThenCreate(agreeAndRequest: Boolean = false) {
            if (currentState.isAccessSubmitting) return
            val created = lastCreated?.takeIf { it.challengeId == currentState.createdChallengeId }
            val verification = created?.verification ?: currentState.verification ?: return
            val requiredPermissions = verification.requiredPermissions.takeIf { verification.type.isAuto }.orEmpty()
            val consents = currentState.pendingAccess?.missingConsents.orEmpty()
            dispatch(CreateChallengeReducerEvent.VerificationAccessSubmitting(true))
            viewModelScope.launch {
                runCatching {
                    if (agreeAndRequest) agreeVerificationConsent(consents)
                    checkVerificationAccess(requiredPermissions)
                }.onSuccess { access ->
                    dispatch(CreateChallengeReducerEvent.VerificationAccessSubmitting(false))
                    if (access.missingConsents.isEmpty() && access.missingPermissions.isEmpty()) {
                        dispatch(CreateChallengeReducerEvent.VerificationAccessRequested(null))
                        if (created != null) {
                            goAfterCreate(created)
                        } else {
                            checkedAccess = access
                            create()
                        }
                    } else {
                        dispatch(CreateChallengeReducerEvent.VerificationAccessRequested(access))
                        if (agreeAndRequest && access.missingConsents.isEmpty()) {
                            emitEffect(CreateChallengeEffect.RequestPermissions(access.missingPermissions))
                        }
                    }
                }.onFailure {
                    dispatch(CreateChallengeReducerEvent.VerificationAccessSubmitting(false))
                    emitEffect(CreateChallengeEffect.ShowError("권한과 동의 상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요"))
                }
            }
        }

        /**
         * 만들지 않고 나간다. 이 ViewModel 은 생성 화면들이 함께 쓰려고 액티비티 범위라,
         * 지우지 않으면 다음에 들어왔을 때 지난 설명과 초안이 그대로 남는다(#589).
         */
        private fun exit() {
            savedStateHandle.remove<String>(KEY_ROUTINE_DESCRIPTION)
            pendingDraft.consume()
            editedFields.clear()
            checkedAccess = null
            lastCreated = null
            // 다시 들어오면 새 생성 시도로 센다
            createStartLogged = false
            dispatch(CreateChallengeReducerEvent.Reset)
            navigationHelper.navigateToBack()
        }

        /** 만든 방으로 보내고, 인증에 필요한 설정이 남았으면 그 화면을 위에 연다. */
        private fun goAfterCreate(created: CreatedChallenge) {
            val id = created.challengeId
            navigationHelper.replaceStackWith(ChallengeDetailPage(id).toRoute())
            viewModelScope.launch {
                // 사용기록 접근은 OS 다이얼로그로 못 받는 특수 권한이라 권한 요청을 통과해 버린다
                val usageMissing =
                    "PACKAGE_USAGE_STATS" in created.verification.requiredPermissions &&
                        runCatching { permissionStatusProvider.capture().usageStats != PermissionState.GRANTED }.getOrDefault(true)
                when {
                    usageMissing ->
                        navigationHelper.navigateByRoute(
                            VerificationPermissionRepairPage.forPermissions(currentState.verification?.requiredPermissions.orEmpty()),
                        )
                    !created.personalSetupRequired -> Unit
                    created.verification.method.needsTargetApps -> navigationHelper.navigateByRoute(ChallengeTargetsPage(id).toRoute())
                    created.verification.method.needsAnchor ->
                        navigationHelper.navigateByRoute(
                            NavRoute(
                                AppRoutes.VERIFICATION_LOCATION,
                                mapOf(
                                    "challengeId" to id,
                                    "defaultRadiusM" to "500.0",
                                    // 목표 체류 시간이 곧 OS 지오펜스의 loiteringDelay 다.
                                    "dwellMinutes" to (currentState.params.durationMinutes() ?: DEFAULT_DWELL_MINUTES).toString(),
                                    "targetPackages" to "",
                                ),
                            ),
                        )
                    else -> Unit
                }
            }
        }

        private var countdownJob: Job? = null
        private var draftJob: Job? = null
        private var createStartLogged = false

        // 초안 수정률은 필드별 1회로 센다
        private val editedFields = mutableSetOf<DraftField>()

        /** 원본과 달라진 항목을 필드당 한 번만 기록한다. */
        private fun logDraftEdit(
            field: DraftField,
            autoToManual: Boolean? = null,
        ) {
            if (!editedFields.add(field)) return
            bizLogger.record(ChallengeEvents.draftEdit(field, autoToManual))
        }

        private fun durationDays(
            start: String,
            end: String,
        ): Int =
            try {
                (LocalDate.parse(end).toEpochDay() - LocalDate.parse(start).toEpochDay()).toInt().coerceAtLeast(0)
            } catch (_: DateTimeParseException) {
                0
            }

        private companion object {
            const val COUNTDOWN_TICK_MS = 1_000L
        }
    }

private const val KEY_ROUTINE_DESCRIPTION = "routineDescription"

/** 목표값에 체류 시간이 없을 때의 지오펜스 대기(분). */
private const val DEFAULT_DWELL_MINUTES = 60

private val VerificationMethod.needsTargetApps: Boolean
    get() = this == VerificationMethod.SCREEN_TIME_MAX || this == VerificationMethod.SCREEN_TIME_MIN

private val VerificationMethod.needsAnchor: Boolean
    get() = this == VerificationMethod.GPS_PRESENCE || this == VerificationMethod.GPS_AVOID
