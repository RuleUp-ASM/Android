package com.ruleup.onboarding.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileEffect
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileViewModel
import com.ruleup.ui.helper.LocalMessageHelper
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * 프로필 설정 플로우의 페이지별 화면. 5개 화면이 Activity 스코프의 단일 [ProfileViewModel] 을
 * 공유하므로 입력값이 페이지 이동에도 누적된다. 단순 전진/후진은 각 Content 가 LocalNavigationHelper 로
 * 직접 처리하고, 비동기 분기(닉네임 검사·가입 제출)는 ViewModel 이 처리한다.
 */
@Composable
private fun sharedProfileViewModel(): ProfileViewModel =
    metroViewModel(viewModelStoreOwner = rememberActivityViewModelStoreOwner())

/** 01 · 프로필 아이콘. signupToken 은 진입 시 args 로 전달받아 ViewModel 에 저장한다. */
@Composable
fun ProfileIconScreen(
    signupToken: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(signupToken) {
        viewModel.onIntent(ProfileIntent.SetSignupToken(signupToken))
    }

    ProfileIconContent(
        modifier = modifier,
        imageUri = state.profileImageUrl,
        onIntent = viewModel::onIntent,
    )
}

/** 02 · 닉네임. "다음" 은 ViewModel 의 형식·중복 검사를 거쳐 통과 시 관심사 페이지로 이동한다. */
@Composable
fun ProfileNicknameScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is ProfileEffect.ShowError) {
                messageHelper.showToast(effect.message)
            }
        }
    }

    NicknameContent(
        modifier = modifier,
        nickname = state.nickname,
        imageUri = state.profileImageUrl,
        onIntent = viewModel::onIntent,
    )
}

/** 03 · 관심사. */
@Composable
fun ProfileInterestScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InterestContent(
        modifier = modifier,
        selected = state.interests,
        onIntent = viewModel::onIntent,
    )
}

/** 04 · 권한. */
@Composable
fun ProfilePermissionScreen(modifier: Modifier = Modifier) {
    PermissionContent(modifier = modifier)
}

/** 05 · 약관 동의. "시작하기" 는 가입을 제출하고 성공 시 ViewModel 이 홈으로 이동시킨다. */
@Composable
fun ProfileAgreementScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is ProfileEffect.ShowError) {
                messageHelper.showToast(effect.message)
            }
        }
    }

    AgreementsContent(
        modifier = modifier,
        agreements = state.agreements,
        onIntent = viewModel::onIntent,
    )
}
