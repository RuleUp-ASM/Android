package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.onboarding.presentation.common.AuthFailureHost
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingEffect
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper

/** 온보딩 6단계 화면. */
@Composable
private fun sharedOnboardingViewModel(): OnboardingViewModel = hiltViewModel(viewModelStoreOwner = rememberActivityViewModelStoreOwner())

/** 실패 안내를 화면에 붙인다. */
@Composable
private fun OnboardingFailureHost(viewModel: OnboardingViewModel) {
    val messageHelper = LocalMessageHelper.current
    val nav = LocalNavigationHelper.current
    var failure by remember { mutableStateOf<AuthFailureUi?>(null) }
    var confirmExit by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingEffect.ShowFailure ->
                    when (val ui = effect.ui) {
                        is AuthFailureUi.Toast -> messageHelper.showToast(ui.message)
                        else -> failure = ui
                    }

                OnboardingEffect.ConfirmExit -> confirmExit = true
            }
        }
    }
    AuthFailureHost(
        ui = failure,
        onDismiss = {
            val restart = (failure as? AuthFailureUi.Dialog)?.restartFromLogin == true
            failure = null
            if (restart) nav.navigateTo(LoginPage)
        },
    )

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("가입을 그만둘까요?") },
            // 5분짜리 가입 토큰이라 되돌아올 수 없다.
            text = { Text("지금 나가면 처음부터 다시 해야 해요") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    nav.navigateTo(LoginPage)
                }) { Text("그만두기") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("이어서 하기") }
            },
        )
    }
}

/** 1/6 · 닉네임. */
@Composable
fun OnboardingNicknameScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect()

    NicknameContent(
        modifier = modifier,
        nickname = state.nickname,
        nicknameMessage = state.nicknameMessage,
        nicknameAvailable = state.nicknameAvailable,
        imageUri = state.profileImageUri,
        onIntent = viewModel::onIntent,
    )
    OnboardingFailureHost(viewModel)
}

/** 2/6 · 관심 분야. */
@Composable
fun OnboardingInterestScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect()

    InterestContent(
        modifier = modifier,
        selected = state.interests,
        onIntent = viewModel::onIntent,
    )
}

/** 3/6 · 생일. */
@Composable
fun OnboardingBirthScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect()

    BirthDateContent(
        modifier = modifier,
        birthDateInput = state.birthDateInput,
        birthDateError = state.birthDateError,
        birthDateValid = state.birthDate != null,
        onIntent = viewModel::onIntent,
    )
}

/** 4/6 · 성별. */
@Composable
fun OnboardingGenderScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect()

    GenderContent(
        modifier = modifier,
        gender = state.gender,
        onIntent = viewModel::onIntent,
    )
}

/** 5/6 · 프로필 사진. */
@Composable
fun OnboardingPhotoScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect()

    PhotoContent(
        modifier = modifier,
        imageUri = state.profileImageUri,
        onIntent = viewModel::onIntent,
    )
}

/** 6/6 · 약관. */
@Composable
fun OnboardingTermsScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect()

    TermsContent(
        modifier = modifier,
        checked = state.agreements,
        submitting = state.isSubmitting,
        onIntent = viewModel::onIntent,
    )
    OnboardingFailureHost(viewModel)
}
