package com.ruleup.onboarding.presentation.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.auth.NickNameUtil
import com.ruleup.onboarding.domain.auth.SessionBootstrap
import com.ruleup.onboarding.domain.auth.model.SignupForm
import com.ruleup.onboarding.domain.auth.usecase.BirthDateValidation
import com.ruleup.onboarding.domain.auth.usecase.CheckNicknameUseCase
import com.ruleup.onboarding.domain.auth.usecase.SignupUseCase
import com.ruleup.onboarding.domain.auth.usecase.ValidateBirthDateUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.ProfileInterestPage
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.NicknameCheckReason
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 프로필 설정 플로우 공유 ViewModel.
 *
 * 페이지들이 같은 인스턴스를 공유해 입력값을 누적한다. 단순 전진/후진은 화면이
 * [NavigationHelper] 로 직접 처리하고, 비동기 분기(닉네임 검사·가입 제출)만 여기서 담당한다.
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val signupUseCase: SignupUseCase,
        private val checkNicknameUseCase: CheckNicknameUseCase,
        private val validateBirthDateUseCase: ValidateBirthDateUseCase,
        private val sessionBootstrap: SessionBootstrap,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ProfileIntent, ProfileState, ProfileReducerEvent, ProfileEffect>(ProfileState.initial) {
        override fun onIntent(intent: ProfileIntent) {
            when (intent) {
                is ProfileIntent.SetSignupToken -> dispatch(ProfileReducerEvent.SetSignupToken(intent.token))
                is ProfileIntent.SetNickName -> dispatch(ProfileReducerEvent.NicknameEntered(intent.name))
                is ProfileIntent.SetProfileIcon -> dispatch(ProfileReducerEvent.ProfileImageSelected(intent.img))
                is ProfileIntent.SetProfileInterest -> dispatch(ProfileReducerEvent.InterestsSelected(intent.interestCategory))
                is ProfileIntent.SetBirthDate -> enterBirthDate(intent.digits)
                is ProfileIntent.SetGender -> dispatch(ProfileReducerEvent.GenderSelected(intent.gender))
                is ProfileIntent.ToggleAgreement -> dispatch(ProfileReducerEvent.AgreementToggled(intent.type))
                ProfileIntent.ToggleAllAgreements -> dispatch(ProfileReducerEvent.AllAgreementsToggled)
                ProfileIntent.CheckNickname -> checkNickname(currentState.nickname)
                ProfileIntent.Submit -> submit()
            }
        }

        override fun reduce(
            state: ProfileState,
            event: ProfileReducerEvent,
        ): ProfileState =
            when (event) {
                is ProfileReducerEvent.SetSignupToken -> state.copy(signupToken = event.token)

                is ProfileReducerEvent.NicknameEntered -> state.copy(nickname = event.nickname)

                is ProfileReducerEvent.ProfileImageSelected -> state.copy(profileImageUri = event.uri)

                is ProfileReducerEvent.InterestsSelected ->
                    if (event.interest in state.interests) {
                        state.copy(interests = state.interests - event.interest)
                    } else {
                        state.copy(interests = state.interests + event.interest)
                    }

                is ProfileReducerEvent.BirthDateEntered ->
                    state.copy(
                        birthDateInput = event.digits,
                        birthDate = event.birthDate,
                        birthDateError = event.error,
                    )

                // 같은 카드를 다시 고르면 해제한다. 미선택은 제출 시 NON_BINARY 로 나간다.
                is ProfileReducerEvent.GenderSelected ->
                    state.copy(gender = if (state.gender == event.gender) null else event.gender)

                is ProfileReducerEvent.AgreementToggled ->
                    state.copy(
                        agreements =
                            if (event.type in state.agreements) {
                                state.agreements - event.type
                            } else {
                                state.agreements + event.type
                            },
                    )

                ProfileReducerEvent.AllAgreementsToggled ->
                    state.copy(
                        agreements =
                            if (state.agreements.containsAll(AgreementType.entries)) {
                                emptySet()
                            } else {
                                AgreementType.entries.toSet()
                            },
                    )

                ProfileReducerEvent.Submitting -> state.copy(isSubmitting = true)

                ProfileReducerEvent.SubmitFailed -> state.copy(isSubmitting = false)
            }

        /** 8자리가 차기 전에는 검증하지 않는다 — 입력 도중 "잘못된 날짜"를 띄우면 계속 깜빡인다. */
        private fun enterBirthDate(digits: String) {
            val trimmed = digits.filter { it.isDigit() }.take(BIRTH_DATE_LENGTH)
            if (trimmed.length < BIRTH_DATE_LENGTH) {
                dispatch(ProfileReducerEvent.BirthDateEntered(trimmed, birthDate = null, error = null))
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
                    dispatch(ProfileReducerEvent.BirthDateEntered(trimmed, validation.birthDate, error = null))

                BirthDateValidation.Invalid ->
                    dispatch(ProfileReducerEvent.BirthDateEntered(trimmed, birthDate = null, error = "생년월일을 다시 확인해주세요"))

                BirthDateValidation.Underage ->
                    dispatch(
                        ProfileReducerEvent.BirthDateEntered(
                            trimmed,
                            birthDate = null,
                            error = "만 ${ValidateBirthDateUseCase.MIN_AGE}세 미만은 가입할 수 없어요",
                        ),
                    )
            }
        }

        private fun checkNickname(name: String) {
            val validation = NickNameUtil.validate(name)
            if (!validation.isValid) {
                emitEffect(ProfileEffect.ShowError(NickNameUtil.message(validation)))
                return
            }
            viewModelScope.launch {
                runCatching { checkNicknameUseCase(name) }
                    .onSuccess { check ->
                        if (check.available) {
                            navigationHelper.navigateTo(ProfileInterestPage)
                        } else {
                            emitEffect(ProfileEffect.ShowError(check.message()))
                        }
                    }.onFailure {
                        emitEffect(ProfileEffect.ShowError("닉네임을 확인하지 못했어요. 잠시 후 다시 시도해주세요"))
                    }
            }
        }

        private fun submit() {
            val state = currentState
            if (state.isSubmitting) return

            val token =
                state.signupToken ?: run {
                    emitEffect(ProfileEffect.ShowError("가입 정보가 만료됐어요. 로그인부터 다시 해주세요"))
                    return
                }
            val birthDate =
                state.birthDate ?: run {
                    emitEffect(ProfileEffect.ShowError("생년월일을 입력해주세요"))
                    return
                }
            if (!state.requiredAgreementsSatisfied) {
                emitEffect(ProfileEffect.ShowError("필수 약관에 동의해주세요"))
                return
            }

            // 인트로 응답이 없으면(페일오픈) 폴백 버전으로 기록하고 서버 재검증에 맡긴다.
            val versions = sessionBootstrap.termsVersions ?: TermsVersions(emptyMap())

            viewModelScope.launch {
                dispatch(ProfileReducerEvent.Submitting)
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
                }.onSuccess {
                    navigationHelper.navigateTo(HomePage)
                }.onFailure {
                    dispatch(ProfileReducerEvent.SubmitFailed)
                    emitEffect(ProfileEffect.ShowError(it.message ?: "가입에 실패했어요"))
                }
            }
        }

        private companion object {
            const val BIRTH_DATE_LENGTH = 8
        }
    }

private fun NicknameCheck.message(): String =
    when (reason) {
        NicknameCheckReason.DUPLICATED -> "이미 사용 중인 닉네임이에요"
        NicknameCheckReason.FORMAT -> "사용할 수 없는 닉네임이에요"
        // 사칭 방지로 해제 후 1주간 잠긴다. 언제 풀리는지 모르면 계속 다른 닉네임을 시도하게 된다.
        NicknameCheckReason.RECENTLY_RELEASED ->
            availableAt?.let { "최근에 해제된 닉네임이에요. $it 부터 쓸 수 있어요" }
                ?: "최근에 해제된 닉네임이라 잠시 쓸 수 없어요"
        null -> "사용할 수 없는 닉네임이에요"
    }
