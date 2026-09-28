package com.ruleup.onboarding.presentation.onboarding.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.NickNameUtil
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.logging.domain.BizLogger
import com.ruleup.onboarding.domain.auth.SignupSession
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.usecase.BirthDateValidation
import com.ruleup.onboarding.domain.auth.usecase.SignupUseCase
import com.ruleup.onboarding.domain.auth.usecase.ValidateBirthDateUseCase
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.onboarding.domain.logging.OnboardingEvents
import com.ruleup.onboarding.domain.logging.SignupTimer
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.onboarding.presentation.common.toAuthFailureUi
import com.ruleup.onboarding.presentation.intro.viewmodel.goHomeOrPending
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.NicknameCheckReason
import com.ruleup.profile.domain.repository.ProfileRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 온보딩 6단계 공유 ViewModel. */
@OptIn(FlowPreview::class)
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val signupUseCase: SignupUseCase,
        private val validateBirthDateUseCase: ValidateBirthDateUseCase,
        private val profileRepository: ProfileRepository,
        private val introRepository: IntroRepository,
        private val signupSession: SignupSession,
        private val signupTimer: SignupTimer,
        private val bizLogger: BizLogger,
        private val navigationHelper: NavigationHelper,
        private val pendingDeepLink: PendingDeepLink,
    ) : MviViewModel<OnboardingIntent, OnboardingState, OnboardingReducerEvent, OnboardingEffect>(OnboardingState.initial) {
        /** 닉네임 입력 스트림. */
        private val nicknameInput = MutableSharedFlow<String>(extraBufferCapacity = 1)

        init {
            viewModelScope.launch {
                nicknameInput
                    .debounce(NICKNAME_DEBOUNCE_MS)
                    .distinctUntilChanged()
                    .collect { checkNickname(it) }
            }
            // IdP 닉네임을 채워 준다.
            signupSession
                .oauthProfile()
                ?.nicknameHint
                ?.takeIf { it.isNotBlank() }
                ?.let { enterNickname(it) }
        }

        override fun onIntent(intent: OnboardingIntent) {
            when (intent) {
                is OnboardingIntent.SetNickName -> enterNickname(intent.name)
                is OnboardingIntent.SetProfileIcon -> dispatch(OnboardingReducerEvent.ProfileImageSelected(intent.img))
                is OnboardingIntent.SetProfileInterest -> dispatch(OnboardingReducerEvent.InterestsSelected(intent.interestCategory))
                is OnboardingIntent.SetBirthDate -> enterBirthDate(intent.digits)
                is OnboardingIntent.SetGender -> dispatch(OnboardingReducerEvent.GenderSelected(intent.gender))
                is OnboardingIntent.ToggleAgreement -> dispatch(OnboardingReducerEvent.AgreementToggled(intent.type))
                OnboardingIntent.ToggleAllAgreements -> dispatch(OnboardingReducerEvent.AllAgreementsToggled)
                OnboardingIntent.BackFromFirstStep -> emitEffect(OnboardingEffect.ConfirmExit)
                OnboardingIntent.Submit -> submit()
            }
        }

        override fun reduce(
            state: OnboardingState,
            event: OnboardingReducerEvent,
        ): OnboardingState =
            when (event) {
                // 입력이 바뀌면 직전 확인 결과는 무효다.
                is OnboardingReducerEvent.NicknameEntered ->
                    state.copy(nickname = event.nickname, nicknameAvailable = null, nicknameMessage = null)

                is OnboardingReducerEvent.NicknameChecked ->
                    state.copy(nicknameAvailable = event.available, nicknameMessage = event.message)

                is OnboardingReducerEvent.ProfileImageSelected -> state.copy(profileImageUri = event.uri)

                is OnboardingReducerEvent.InterestsSelected ->
                    when {
                        event.interest in state.interests -> state.copy(interests = state.interests - event.interest)
                        state.interests.size < InterestLimits.MAX ->
                            state.copy(interests = state.interests + event.interest)
                        // 6개를 넘기면 서버가 INTEREST_LIMIT_EXCEEDED 로 튕긴다.
                        else -> state
                    }

                is OnboardingReducerEvent.BirthDateEntered ->
                    state.copy(
                        birthDateInput = event.digits,
                        birthDate = event.birthDate,
                        birthDateError = event.error,
                    )

                // 필수 입력이라 해제는 없다
                is OnboardingReducerEvent.GenderSelected -> state.copy(gender = event.gender)

                is OnboardingReducerEvent.AgreementToggled ->
                    state.copy(
                        agreements =
                            if (event.type in state.agreements) {
                                state.agreements - event.type
                            } else {
                                state.agreements + event.type
                            },
                    )

                OnboardingReducerEvent.AllAgreementsToggled ->
                    state.copy(
                        agreements =
                            if (state.agreements.containsAll(AgreementType.SIGNUP)) {
                                emptySet()
                            } else {
                                AgreementType.SIGNUP.toSet()
                            },
                    )

                OnboardingReducerEvent.Submitting -> state.copy(isSubmitting = true)

                OnboardingReducerEvent.SubmitFailed -> state.copy(isSubmitting = false)
            }

        private fun enterNickname(name: String) {
            dispatch(OnboardingReducerEvent.NicknameEntered(name))
            // 형식이 틀린 건 서버에 묻지 않고 바로 알려 준다.
            val validation = NickNameUtil.validate(name)
            if (!validation.isValid) {
                dispatch(
                    OnboardingReducerEvent.NicknameChecked(
                        available = false,
                        message = NickNameUtil.message(validation),
                    ),
                )
                return
            }
            nicknameInput.tryEmit(name)
        }

        private suspend fun checkNickname(name: String) {
            runCatching { profileRepository.checkNickname(name) }
                .onSuccess { check ->
                    // 확인을 보낸 뒤 입력이 바뀌었으면 버린다.
                    if (currentState.nickname != name) return@onSuccess
                    bizLogger.record(
                        OnboardingEvents.nicknameCheck(
                            valid = check.valid,
                            available = check.available,
                            reason = check.reason?.name,
                        ),
                    )
                    dispatch(
                        OnboardingReducerEvent.NicknameChecked(
                            available = check.available,
                            message = if (check.available) "사용 가능한 닉네임이에요" else check.message(),
                        ),
                    )
                }.onFailure {
                    if (currentState.nickname != name) return@onFailure
                    // 확인 실패는 "쓸 수 없음"이 아니다.
                    dispatch(
                        OnboardingReducerEvent.NicknameChecked(
                            available = null,
                            message = "닉네임을 확인하지 못했어요. 잠시 후 다시 시도해주세요",
                        ),
                    )
                }
        }

        /** 8자리가 차기 전에는 검증하지 않는다 */
        private fun enterBirthDate(digits: String) {
            val trimmed = digits.filter { it.isDigit() }.take(OnboardingState.BIRTH_DATE_LENGTH)
            if (trimmed.length < OnboardingState.BIRTH_DATE_LENGTH) {
                dispatch(OnboardingReducerEvent.BirthDateEntered(trimmed, birthDate = null, error = null))
                return
            }
            val validation =
                validateBirthDateUseCase(
                    year = trimmed.substring(0, 4).toInt(),
                    month = trimmed.substring(4, 6).toInt(),
                    day = trimmed.substring(6, 8).toInt(),
                )
            when (validation) {
                is BirthDateValidation.Valid ->
                    dispatch(OnboardingReducerEvent.BirthDateEntered(trimmed, validation.birthDate, error = null))

                BirthDateValidation.Invalid ->
                    dispatch(OnboardingReducerEvent.BirthDateEntered(trimmed, birthDate = null, error = "생년월일을 다시 확인해주세요"))

                BirthDateValidation.Underage ->
                    dispatch(
                        OnboardingReducerEvent.BirthDateEntered(
                            trimmed,
                            birthDate = null,
                            error = "만 ${ValidateBirthDateUseCase.MIN_AGE}세 미만은 가입할 수 없어요",
                        ),
                    )
            }
        }

        private fun submit() {
            val state = currentState
            if (state.isSubmitting) return

            val token = signupSession.token()
            if (token == null) {
                restartFromLogin("가입 정보가 만료됐어요. 로그인부터 다시 해주세요")
                return
            }
            val birthDate = state.birthDate
            if (birthDate == null) {
                emitEffect(OnboardingEffect.ShowFailure(AuthFailureUi.Toast("생년월일을 입력해주세요")))
                return
            }
            // 성별은 필수.
            val gender = state.gender
            if (gender == null) {
                emitEffect(OnboardingEffect.ShowFailure(AuthFailureUi.Toast("성별을 선택해주세요")))
                return
            }
            if (!state.requiredAgreementsSatisfied) {
                emitEffect(OnboardingEffect.ShowFailure(AuthFailureUi.Toast("필수 약관에 동의해주세요")))
                return
            }

            // 인트로 응답이 없으면(페일오픈) 폴백 버전으로 기록하고 서버 재검증에 맡긴다.
            val versions = introRepository.lastTermsVersions()

            viewModelScope.launch {
                dispatch(OnboardingReducerEvent.Submitting)
                runCatching {
                    signupUseCase(
                        SignupForm(
                            signupToken = token,
                            nickname = state.nickname,
                            interestCategories = state.interests,
                            birthDate = birthDate,
                            gender = gender,
                            agreements = AgreementConsents.of(state.agreements, versions),
                            localImageUri = state.profileImageUri,
                        ),
                    )
                }.onSuccess { user ->
                    // 가입이 끝났다.
                    signupSession.clear()
                    bizLogger.record(
                        OnboardingEvents.signupComplete(
                            interestCount = state.interests.size,
                            // 성별이 필수가 되면서 항상 true 다.
                            hasGender = true,
                            optionalAgreements = state.agreements.count { !it.required },
                            durationMs = signupTimer.consumeElapsedMillis(),
                        ),
                    )
                    if (state.profileImageUri != null) {
                        bizLogger.record( // 업로드 실패는 UseCase 가 삼키므로 결과는 URL 이 붙었는지로 판정한다.
                            OnboardingEvents.profileImageUploadResult(success = user.profileImageUrl != null),
                        )
                    }
                    navigationHelper.goHomeOrPending(pendingDeepLink)
                }.onFailure { error ->
                    dispatch(OnboardingReducerEvent.SubmitFailed)
                    bizLogger.record(OnboardingEvents.signupFailed((error as? AuthException)?.failure?.name ?: "UNKNOWN"))
                    // 토큰이 만료됐으면 되돌아갈 단계가 없다.
                    if ((error as? AuthException)?.failure == AuthFailure.INVALID_SIGNUP_TOKEN) {
                        restartFromLogin("시간이 초과됐어요. 처음부터 다시 해주세요")
                    } else {
                        emitEffect(OnboardingEffect.ShowFailure(error.toAuthFailureUi()))
                    }
                }
            }
        }

        // 로그인 화면 이동은 대화상자를 닫을 때 한다.
        private fun restartFromLogin(message: String) {
            signupSession.clear()
            emitEffect(OnboardingEffect.ShowFailure(AuthFailureUi.Dialog(message, restartFromLogin = true)))
        }

        private companion object {
            const val NICKNAME_DEBOUNCE_MS = 500L
        }
    }

private fun NicknameCheck.message(): String =
    when (reason) {
        NicknameCheckReason.DUPLICATED -> "이미 사용 중인 닉네임이에요"
        NicknameCheckReason.FORMAT -> "사용할 수 없는 닉네임이에요"
        // 사칭 방지로 해제 후 1주간 잠긴다.
        NicknameCheckReason.RECENTLY_RELEASED ->
            availableAt?.let { "최근에 해제된 닉네임이에요. $it 부터 쓸 수 있어요" }
                ?: "최근에 해제된 닉네임이라 잠시 쓸 수 없어요"
        null -> "사용할 수 없는 닉네임이에요"
    }
