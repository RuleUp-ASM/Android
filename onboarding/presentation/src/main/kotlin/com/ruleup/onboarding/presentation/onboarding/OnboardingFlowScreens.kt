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
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 온보딩 6단계 화면. 전부 액티비티 스코프의 단일 [OnboardingViewModel] 을 공유해 입력값이 페이지
 * 이동에도 누적된다.
 *
 * 단순 전진/후진은 각 Content 가 LocalNavigationHelper 로 직접 처리하고, 비동기 분기(닉네임 검사·
 * 가입 제출)와 실패 안내는 ViewModel 이 맡는다.
 */
@Composable
private fun sharedOnboardingViewModel(): OnboardingViewModel = hiltViewModel(viewModelStoreOwner = rememberActivityViewModelStoreOwner())

/**
 * 실패 안내를 화면에 붙인다.
 *
 * 6단계가 같은 처리를 반복하지 않도록 한곳에 모았다 — 토스트는 컴포지션 밖(MessageHelper)에서,
 * 다이얼로그·전체 화면은 컴포지션 안에서 그려야 해서 화면마다 따로 쓰면 금세 갈린다.
 */
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
    AuthFailureHost(ui = failure, onDismiss = { failure = null })

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("가입을 그만둘까요?") },
            // 5분짜리 가입 토큰이라 되돌아올 수 없다. 그 사실을 알려야 실수로 나가지 않는다.
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

/** 1/6 · 닉네임. 가입 토큰은 SignupSession 이 들고 있어 화면이 넘겨받지 않는다. */
@Composable
fun OnboardingNicknameScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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

/** 2/6 · 관심 분야. 0~6개이며 아무것도 안 고르고 넘어갈 수 있다. */
@Composable
fun OnboardingInterestScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InterestContent(
        modifier = modifier,
        selected = state.interests,
        onIntent = viewModel::onIntent,
    )
}

/** 3/6 · 생일. 필수이고 만 14세 미만은 진행할 수 없다. */
@Composable
fun OnboardingBirthScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BirthDateContent(
        modifier = modifier,
        birthDateInput = state.birthDateInput,
        birthDateError = state.birthDateError,
        birthDateValid = state.birthDate != null,
        onIntent = viewModel::onIntent,
    )
}

/** 4/6 · 성별. 필수이며 고르기 전에는 다음으로 넘어갈 수 없다. */
@Composable
fun OnboardingGenderScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GenderContent(
        modifier = modifier,
        gender = state.gender,
        onIntent = viewModel::onIntent,
    )
}

/** 5/6 · 프로필 사진. 선택이며 가입 후 별도로 업로드된다. */
@Composable
fun OnboardingPhotoScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PhotoContent(
        modifier = modifier,
        imageUri = state.profileImageUri,
        onIntent = viewModel::onIntent,
    )
}

/** 6/6 · 약관. "시작하기" 가 가입을 제출하고 성공 시 ViewModel 이 홈으로 보낸다. */
@Composable
fun OnboardingTermsScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedOnboardingViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TermsContent(
        modifier = modifier,
        checked = state.agreements,
        submitting = state.isSubmitting,
        onIntent = viewModel::onIntent,
    )
    OnboardingFailureHost(viewModel)
}
