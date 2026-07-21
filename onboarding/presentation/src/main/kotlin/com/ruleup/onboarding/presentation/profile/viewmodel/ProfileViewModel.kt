package com.ruleup.onboarding.presentation.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.onboarding.domain.auth.NickNameUtil
import com.ruleup.onboarding.domain.auth.model.SignupForm
import com.ruleup.onboarding.domain.auth.usecase.CheckNicknameUseCase
import com.ruleup.onboarding.domain.auth.usecase.SignupUseCase
import com.ruleup.onboarding.domain.auth.usecase.SubmitOnboardingInfoUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.ProfileInterestPage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

/**
 * 프로필 설정 플로우 공유 ViewModel.
 *
 * 6개 페이지(아이콘→닉네임→관심사→권한→나이·성별→약관)가 같은 인스턴스를 공유해 입력값을 누적한다.
 * 페이지 간 단순 전진/후진은 화면이 [NavigationHelper] 로 직접 처리하고,
 * 비동기 분기(닉네임 중복검사, 가입 제출)만 본 ViewModel 이 담당한다.
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val signupUseCase: SignupUseCase,
        private val checkNicknameUseCase: CheckNicknameUseCase,
        private val submitOnboardingInfoUseCase: SubmitOnboardingInfoUseCase,
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

                is ProfileIntent.SetAge -> {
                    dispatch(ProfileReducerEvent.AgeEntered(intent.age))
                }

                is ProfileIntent.SetGender -> {
                    dispatch(ProfileReducerEvent.GenderSelected(intent.gender))
                }

                ProfileIntent.DeclineGender -> {
                    dispatch(ProfileReducerEvent.GenderDeclined)
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

                is ProfileReducerEvent.AgeEntered -> {
                    state.copy(age = event.age)
                }

                is ProfileReducerEvent.GenderSelected -> {
                    // 같은 카드 재선택은 해제. 카드 선택 시 "응답 안 함" 은 자동 해제.
                    if (state.gender == event.gender) {
                        state.copy(gender = null)
                    } else {
                        state.copy(gender = event.gender, genderDeclined = false)
                    }
                }

                ProfileReducerEvent.GenderDeclined -> {
                    val declined = !state.genderDeclined
                    state.copy(genderDeclined = declined, gender = if (declined) null else state.gender)
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
                    // 가입 성공(토큰 확보) 후 선택 입력한 나이·성별을 전송한다.
                    // 추천 개인화 보조 정보라 실패해도 홈 진입을 막지 않는다.
                    runCatching {
                        submitOnboardingInfoUseCase(
                            birthDate = state.age?.let { age -> "${Year.now().value - age}-01-01" },
                            gender = if (state.genderDeclined) null else state.gender?.value,
                        )
                    }
                    navigationHelper.navigateTo(HomePage)
                }.onFailure {
                    dispatch(ProfileReducerEvent.SubmitFailed)
                    emitEffect(ProfileEffect.ShowError(it.message ?: "가입 실패"))
                }
            }
        }
    }
