package com.ruleup.presentation.profile

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.domain.ProfileAgreementPage
import com.ruleup.domain.ProfileNicknamePage
import com.ruleup.domain.ProfilePermissionPage
import com.ruleup.presentation.profile.viewmodel.ProfileEffect
import com.ruleup.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.presentation.profile.viewmodel.ProfileViewModel
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 프로필 설정 플로우의 페이지별 화면. 5개 화면이 Activity 스코프의 단일 [ProfileViewModel] 을
 * 공유하므로 입력값이 페이지 이동에도 누적된다. 단순 전진/후진은 [LocalNavigationHelper] 로,
 * 비동기 분기(닉네임 검사·가입 제출)는 ViewModel 이 처리한다.
 */
@Composable
private fun sharedProfileViewModel(): ProfileViewModel {
    val activity = LocalActivity.current as ComponentActivity
    return hiltViewModel(viewModelStoreOwner = activity)
}

/** 01 · 프로필 아이콘. signupToken 은 진입 시 args 로 전달받아 ViewModel 에 저장한다. */
@Composable
fun ProfileIconScreen(
    signupToken: String,
    modifier: Modifier = Modifier,
) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavigationHelper.current

    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.onIntent(ProfileIntent.SetProfileIcon(uri.toString()))
        }

    LaunchedEffect(signupToken) {
        viewModel.onIntent(ProfileIntent.SetSignupToken(signupToken))
    }

    val pickImage = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    ProfileIconContent(
        modifier = modifier,
        imageUri = state.profileImageUrl,
        onGalleryClick = pickImage,
        onCameraClick = pickImage,
        onNext = { nav.navigateTo(ProfileNicknamePage) },
        onBack = { nav.navigateToBack() },
    )
}

/** 02 · 닉네임. "다음" 은 ViewModel 의 형식·중복 검사를 거쳐 통과 시 관심사 페이지로 이동한다. */
@Composable
fun ProfileNicknameScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavigationHelper.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is ProfileEffect.ShowError) {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NicknameContent(
        modifier = modifier,
        nickname = state.nickname,
        imageUri = state.profileImageUrl,
        onNickNameChange = { viewModel.onIntent(ProfileIntent.SetNickName(it)) },
        onNext = { viewModel.onIntent(ProfileIntent.CheckNickname) },
        onBack = { nav.navigateToBack() },
    )
}

/** 03 · 관심사. */
@Composable
fun ProfileInterestScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavigationHelper.current

    InterestContent(
        modifier = modifier,
        selected = state.interests,
        onClick = { viewModel.onIntent(ProfileIntent.SetProfileInterest(it)) },
        onNext = { nav.navigateTo(ProfilePermissionPage) },
        onBack = { nav.navigateToBack() },
    )
}

/** 04 · 권한. */
@Composable
fun ProfilePermissionScreen(modifier: Modifier = Modifier) {
    val nav = LocalNavigationHelper.current
    PermissionContent(
        modifier = modifier,
        onNext = { nav.navigateTo(ProfileAgreementPage) },
        onBack = { nav.navigateToBack() },
    )
}

/** 05 · 약관 동의. "시작하기" 는 가입을 제출하고 성공 시 홈으로 이동한다. */
@Composable
fun ProfileAgreementScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedProfileViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavigationHelper.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToHome -> {
                    // TODO: 메인(홈) route 가 정의되면 nav.navigateTo(HomePage) 로 연결한다.
                }
                is ProfileEffect.ShowError ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AgreementsContent(
        modifier = modifier,
        agreements = state.agreements,
        onAgreementsChange = { viewModel.onIntent(ProfileIntent.SetAgreements(it)) },
        onNext = { viewModel.onIntent(ProfileIntent.Submit) },
        onBack = { nav.navigateToBack() },
    )
}
