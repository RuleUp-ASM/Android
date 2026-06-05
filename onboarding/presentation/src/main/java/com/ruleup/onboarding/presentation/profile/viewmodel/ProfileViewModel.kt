package com.ruleup.onboarding.presentation.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.auth.NickNameUtil
import com.ruleup.domain.auth.model.SignupForm
import com.ruleup.domain.auth.usecase.CheckNicknameUseCase
import com.ruleup.domain.auth.usecase.SignupUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.ProfileInterestPage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 프로필 설정 플로우 공유 ViewModel.
 *
 * 5개 페이지(아이콘→닉네임→관심사→권한→약관)가 같은 인스턴스를 공유해 입력값을 누적한다.
 * 페이지 간 단순 전진/후진은 화면이 [NavigationHelper] 로 직접 처리하고,
 * 비동기 분기(닉네임 중복검사, 가입 제출)만 본 ViewModel 이 담당한다.
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val signupUseCase: SignupUseCase,
        private val checkNicknameUseCase: CheckNicknameUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ProfileIntent, ProfileState, ProfileReducerEvent, ProfileEffect>(ProfileState.initial) {
        override fun onIntent(intent: ProfileIntent) {
            when (intent) {
                is ProfileIntent.SetSignupToken -> {
                    dispatch(ProfileReducerEvent.SetSignupToken(intent.token))
                }

                is ProfileIntent.SetNickName -> {
                    dispatch(ProfileReducerEvent.NicknameEntered(intent.name))
                }

                is ProfileIntent.SetProfileIcon -> {
                    dispatch(
                        ProfileReducerEvent.ProfileImageSelected(
                            intent.img,
                        ),
                    )
                }

                is ProfileIntent.SetProfileInterest -> {
                    dispatch(
                        ProfileReducerEvent.InterestsSelected(
                            intent.interestCategory,
                        ),
                    )
                }

                is ProfileIntent.SetAgreements -> {
                    dispatch(ProfileReducerEvent.AgreementsUpdated(intent.agreements))
                }

                ProfileIntent.CheckNickname -> {
                    checkNickname(currentState.nickname)
                }

                ProfileIntent.Submit -> {
                    submit()
                }
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

                ProfileReducerEvent.Submitting -> {
                    state.copy(isSubmitting = true)
                }

                ProfileReducerEvent.SubmitFailed -> {
                    state.copy(isSubmitting = false)
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
                    .onSuccess { available ->
                        if (available) {
                            navigationHelper.navigateTo(ProfileInterestPage)
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
                    signupUseCase(
                        SignupForm(
                            signupToken = token,
                            nickname = state.nickname,
                            interestCategories = state.interests,
                            agreements = state.agreements,
                            localImageUri = state.profileImageUrl,
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
