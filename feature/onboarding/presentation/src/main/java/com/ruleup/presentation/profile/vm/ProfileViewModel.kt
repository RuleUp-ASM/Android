package com.ruleup.presentation.profile.vm

import androidx.lifecycle.viewModelScope
import com.ruleup.core.ui.mvi.MviViewModel
import com.ruleup.domain.auth.model.SignupForm
import com.ruleup.domain.auth.usecase.AuthUseCase
import com.ruleup.presentation.profile.util.NickNameUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val authUseCase: AuthUseCase,
    ) : MviViewModel<ProfileIntent, ProfileState, ProfileReducerEvent, ProfileEffect>(ProfileState.initial) {
        override fun onIntent(intent: ProfileIntent) {
            when (intent) {
                is ProfileIntent.SetSignupToken -> {
                    dispatch(event = ProfileReducerEvent.SetSignupToken(intent.token))
                }

                is ProfileIntent.SetNickName -> {
                    dispatch(ProfileReducerEvent.NicknameEntered(intent.name))
                }

                is ProfileIntent.SetProfileIcon -> {
                    dispatch(ProfileReducerEvent.ProfileImageSelected(intent.img))
                }

                is ProfileIntent.SetProfileInterest -> {
                    dispatch(ProfileReducerEvent.InterestsSelected(intent.interestCategory))
                }

                is ProfileIntent.SetAgreements -> {
                    dispatch(ProfileReducerEvent.AgreementsUpdated(intent.agreements))
                }

                ProfileIntent.NextStep -> {
                    when {
                        currentState.step == 1 -> checkNickName(currentState.nickname)
                        currentState.step >= ProfileState.LAST_STEP -> submit()
                        else -> dispatch(ProfileReducerEvent.StepChanged(currentState.step + 1))
                    }
                }

                ProfileIntent.PrevStep -> {
                    if (currentState.step > 0) {
                        dispatch(ProfileReducerEvent.StepChanged(currentState.step - 1))
                    }
                }

                else -> {}
            }
        }

        override fun reduce(
            state: ProfileState,
            event: ProfileReducerEvent,
        ): ProfileState =
            when (event) {
                is ProfileReducerEvent.SetSignupToken -> {
                    state.copy(signupToken = event.token)
                }

                is ProfileReducerEvent.NicknameEntered -> {
                    state.copy(nickname = event.nickname)
                }

                is ProfileReducerEvent.AgreementsUpdated -> {
                    state.copy(agreements = event.agreements)
                }

                is ProfileReducerEvent.ProfileImageSelected -> {
                    state.copy(profileImageUrl = event.url)
                }

                is ProfileReducerEvent.InterestsSelected -> {
                    if (event.interest in state.interests) {
                        state.copy(interests = state.interests - event.interest)
                    } else {
                        state.copy(interests = state.interests + event.interest)
                    }
                }

                is ProfileReducerEvent.Submitting -> {
                    state.copy(isSubmitting = true)
                }

                is ProfileReducerEvent.StepChanged -> {
                    state.copy(step = event.step)
                }

                is ProfileReducerEvent.SubmitFailed -> {
                    state.copy(isSubmitting = false)
                }
            }

        private fun checkNickName(name: String) {
            val validation = NickNameUtil.validate(name)
            if (!validation.isValid) {
                emitEffect(ProfileEffect.ShowError(NickNameUtil.message(validation)))
                return
            }
            viewModelScope.launch {
                runCatching {
                    authUseCase.checkNicknameAvailability(name)
                }.onSuccess { valid ->
                    if (valid) {
                        dispatch(ProfileReducerEvent.StepChanged(currentState.step + 1))
                    } else {
                        emitEffect(ProfileEffect.ShowError("닉네임이 중복됐어요"))
                    }
                }.onFailure {
                    emitEffect(ProfileEffect.ShowError("닉네임 체크 실패"))
                }
            }
        }

        private fun submit() {
            val state = currentState
            if (state.isSubmitting) return

            val token =
                state.signupToken ?: run {
                    emitEffect(ProfileEffect.ShowError("가입 토큰이 없습니다."))
                    return
                }

            // 필수 약관(이용약관·개인정보) 미동의 시 제출을 막는다.
            if (!state.agreements.terms || !state.agreements.privacy) {
                emitEffect(ProfileEffect.ShowError("필수 약관에 동의해주세요"))
                return
            }

            viewModelScope.launch {
                dispatch(ProfileReducerEvent.Submitting)

                runCatching {
                    authUseCase.signUp(
                        SignupForm(
                            signupToken = token,
                            nickname = state.nickname,
                            interestCategories = state.interests,
                            localImageUri = state.profileImageUrl,
                            agreements = state.agreements,
                        ),
                    )
                }.onSuccess {
                    emitEffect(ProfileEffect.NavigateToHome)
                }.onFailure {
                    dispatch(ProfileReducerEvent.SubmitFailed)
                    emitEffect(ProfileEffect.ShowError(it.message ?: "가입 실패"))
                }
            }
        }
    }
