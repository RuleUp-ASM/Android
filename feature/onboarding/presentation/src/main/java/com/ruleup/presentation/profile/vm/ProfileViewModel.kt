package com.ruleup.presentation.profile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruleup.core.ui.mvi.MviViewModel
import com.ruleup.domain.auth.usecase.AuthUseCase
import com.ruleup.domain.model.Agreements
import com.ruleup.domain.model.SignupForm
import com.ruleup.presentation.util.NickNameUtil
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

                ProfileIntent.NextStep -> {
                    if (currentState.step == 1 && !NickNameUtil.isValid(currentState.nickname)) {
                        emitEffect(ProfileEffect.ShowError("닉네임은 한글 영문 숫자 2 ~ 12자만 가능해요"))
                        return
                    }
                    if (currentState.step >= ProfileState.LAST_STEP) {
                        submit()
                    } else {
                        dispatch(ProfileReducerEvent.StepChanged(currentState.step + 1))
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

        private fun submit() {
            val state = currentState
            if (state.isSubmitting) return

            val token =
                state.signupToken ?: run {
                    emitEffect(ProfileEffect.ShowError("가입 토큰이 없습니다."))
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
                            agreements = state.agreements ?: Agreements(terms = true, privacy = true),
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
