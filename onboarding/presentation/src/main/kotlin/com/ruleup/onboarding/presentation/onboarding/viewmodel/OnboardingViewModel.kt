package com.ruleup.onboarding.presentation.onboarding.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.onboarding.domain.auth.NickNameUtil
import com.ruleup.onboarding.domain.auth.SignupSession
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.usecase.BirthDateValidation
import com.ruleup.onboarding.domain.auth.usecase.SignupUseCase
import com.ruleup.onboarding.domain.auth.usecase.ValidateBirthDateUseCase
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.onboarding.domain.observability.OnboardingEvents
import com.ruleup.onboarding.domain.observability.SignupTimer
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.onboarding.presentation.common.toAuthFailureUi
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

/**
 * 온보딩 6단계 공유 ViewModel.
 *
 * 페이지들이 같은 인스턴스를 공유해 입력값을 누적한다. 단순 전진/후진은 화면이 [NavigationHelper]
 * 로 직접 처리하고, 비동기 분기(닉네임 확인·가입 제출)와 실패 안내만 여기서 담당한다.
 */
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
        private val observability: Observability,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<OnboardingIntent, OnboardingState, OnboardingReducerEvent, OnboardingEffect>(OnboardingState.initial) {
        /**
         * 닉네임 입력 스트림.
         *
         * 타이핑마다 확인 API 를 부르면 무인증 엔드포인트에 부하가 걸리고 응답이 뒤바뀐 순서로
         * 도착한다. 500ms 로 묶고 같은 값은 걸러 마지막 입력만 확인한다.
         */
        private val nicknameInput = MutableSharedFlow<String>(extraBufferCapacity = 1)

        init {
            viewModelScope.launch {
                nicknameInput
                    .debounce(NICKNAME_DEBOUNCE_MS)
                    .distinctUntilChanged()
                    .collect { checkNickname(it) }
            }
            // IdP 닉네임을 채워 준다. 자동 제출은 하지 않는다 — 남이 이미 쓰는 이름일 수 있어
            // check API 를 통과해야 다음 단계로 간다. 통과하면 사용자는 그냥 "다음"만 누르면 된다.
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
                // 입력이 바뀌면 직전 확인 결과는 무효다. 남겨 두면 이전 닉네임의 "사용 가능"으로 통과한다.
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
                        // 6개를 넘기면 서버가 INTEREST_LIMIT_EXCEEDED 로 튕긴다. 아예 담지 않는다.
                        else -> state
                    }

                is OnboardingReducerEvent.BirthDateEntered ->
                    state.copy(
                        birthDateInput = event.digits,
                        birthDate = event.birthDate,
                        birthDateError = event.error,
                    )

                // 같은 카드를 다시 고르면 해제한다. 미선택은 제출 시 NON_BINARY 로 나간다.
                is OnboardingReducerEvent.GenderSelected ->
                    state.copy(gender = if (state.gender == event.gender) null else event.gender)

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
                            if (state.agreements.containsAll(AgreementType.entries)) {
                                emptySet()
                            } else {
                                AgreementType.entries.toSet()
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
                    observability.log(Channel.BUSINESS) {
                        OnboardingEvents.nicknameCheck(
                            valid = check.valid,
                            available = check.available,
                            reason = check.reason?.name,
                        )
                    }
                    dispatch(
                        OnboardingReducerEvent.NicknameChecked(
                            available = check.available,
                            message = if (check.available) "사용 가능한 닉네임이에요" else check.message(),
                        ),
                    )
                }.onFailure {
                    // 확인 실패는 "쓸 수 없음"이 아니다. 통과로 두면 제출에서 튕기므로 미확인으로 남긴다.
                    dispatch(
                        OnboardingReducerEvent.NicknameChecked(
                            available = null,
                            message = "닉네임을 확인하지 못했어요. 잠시 후 다시 시도해주세요",
                        ),
                    )
                }
        }

        /** 8자리가 차기 전에는 검증하지 않는다 — 입력 도중 "잘못된 날짜"를 띄우면 계속 깜빡인다. */
        private fun enterBirthDate(digits: String) {
            val trimmed = digits.filter { it.isDigit() }.take(BIRTH_DATE_LENGTH)
            if (trimmed.length < BIRTH_DATE_LENGTH) {
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
                            // 건너뛴 경우에도 필드는 필수라 논바이너리로 저장된다.
                            gender = state.gender ?: Gender.NON_BINARY,
                            agreements = AgreementConsents.of(state.agreements, versions),
                            localImageUri = state.profileImageUri,
                        ),
                    )
                }.onSuccess { user ->
                    // 가입이 끝났다. 남겨 두면 다음 시도가 만료된 토큰을 물고 시작한다.
                    signupSession.clear()
                    observability.log(Channel.BUSINESS) {
                        OnboardingEvents.signupComplete(
                            interestCount = state.interests.size,
                            hasGender = state.gender != null,
                            optionalAgreements = state.agreements.count { !it.required },
                            durationMs = signupTimer.consumeElapsedMillis(),
                        )
                    }
                    if (state.profileImageUri != null) {
                        observability.log(Channel.BUSINESS) {
                            // 업로드 실패는 UseCase 가 삼키므로 결과는 URL 이 붙었는지로 판정한다.
                            OnboardingEvents.profileImageUploadResult(success = user.profileImageUrl != null)
                        }
                    }
                    navigationHelper.navigateTo(HomePage)
                }.onFailure { error ->
                    dispatch(OnboardingReducerEvent.SubmitFailed)
                    observability.log(Channel.BUSINESS) {
                        OnboardingEvents.signupFailed((error as? AuthException)?.failure?.name ?: "UNKNOWN")
                    }
                    // 토큰이 만료됐으면 되돌아갈 단계가 없다. 로그인부터 다시 시작한다.
                    if ((error as? AuthException)?.failure == AuthFailure.INVALID_SIGNUP_TOKEN) {
                        restartFromLogin("시간이 초과됐어요. 처음부터 다시 해주세요")
                    } else {
                        emitEffect(OnboardingEffect.ShowFailure(error.toAuthFailureUi()))
                    }
                }
            }
        }

        private fun restartFromLogin(message: String) {
            signupSession.clear()
            emitEffect(OnboardingEffect.ShowFailure(AuthFailureUi.Dialog(message, restartFromLogin = true)))
            navigationHelper.navigateTo(LoginPage)
        }

        private companion object {
            const val BIRTH_DATE_LENGTH = 8
            const val NICKNAME_DEBOUNCE_MS = 500L
        }
    }

private fun NicknameCheck.message(): String =
    when (reason) {
        NicknameCheckReason.DUPLICATED -> "이미 사용 중인 닉네임이에요"
        NicknameCheckReason.FORMAT -> "사용할 수 없는 닉네임이에요"
        // 사칭 방지로 해제 후 1주간 잠긴다. 언제 풀리는지 모르면 계속 다른 닉네임만 시도하게 된다.
        NicknameCheckReason.RECENTLY_RELEASED ->
            availableAt?.let { "최근에 해제된 닉네임이에요. $it 부터 쓸 수 있어요" }
                ?: "최근에 해제된 닉네임이라 잠시 쓸 수 없어요"
        null -> "사용할 수 없는 닉네임이에요"
    }
