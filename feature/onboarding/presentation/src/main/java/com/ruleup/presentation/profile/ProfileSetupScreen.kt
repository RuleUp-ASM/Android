package com.ruleup.presentation.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.presentation.profile.util.createImageUri
import com.ruleup.presentation.profile.vm.ProfileEffect
import com.ruleup.presentation.profile.vm.ProfileIntent
import com.ruleup.presentation.profile.vm.ProfileState
import com.ruleup.presentation.profile.vm.ProfileViewModel

/**
 * @param signupToken 가입 토큰
 * @param onFinish 마지막 단계 완료 → 온보딩 종료(홈으로). cross-feature 이동은 콜백으로 위임.
 */
@Composable
fun ProfileSetupScreen(
    signupToken: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnFinish by rememberUpdatedState(onFinish)
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val camera =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success) viewModel.onIntent(ProfileIntent.SetProfileIcon(cameraUri?.toString() ?: ""))
        }
    val gallery =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri -> viewModel.onIntent(ProfileIntent.SetProfileIcon(uri?.toString() ?: "")) }

    LaunchedEffect(signupToken) {
        viewModel.onIntent(ProfileIntent.SetSignupToken(signupToken))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToHome -> currentOnFinish()
                is ProfileEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    BackHandler(enabled = state.step > 0) {
        viewModel.onIntent(ProfileIntent.PrevStep)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ProfileSetupContent(
            onIntent = viewModel::onIntent,
            uiState = state,
            modifier = Modifier.padding(padding),
            onPickGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCameraClick = {
                val uri = createImageUri(context)
                cameraUri = uri
                camera.launch(uri)
            },
        )
    }
}

@Composable
fun ProfileSetupContent(
    onIntent: (ProfileIntent) -> Unit,
    uiState: ProfileState,
    modifier: Modifier = Modifier,
    onPickGallery: () -> Unit = {},
    onCameraClick: () -> Unit = {},
) {
    when (uiState.step) {
        0 -> {
            ProfileIconContent(
                modifier = modifier,
                imageUri = uiState.profileImageUrl,
                onNext = { onIntent(ProfileIntent.NextStep) },
                onBack = { onIntent(ProfileIntent.PrevStep) },
                onGalleryClick = onPickGallery,
                onCameraClick = onCameraClick,
            )
        }

        1 -> {
            NicknameContent(
                modifier = modifier,
                nickname = uiState.nickname,
                onNext = { onIntent(ProfileIntent.NextStep) },
                onBack = { onIntent(ProfileIntent.PrevStep) },
                onNickNameChange = { onIntent(ProfileIntent.SetNickName(it)) },
            )
        }

        2 -> {
            InterestContent(
                modifier = modifier,
                selected = uiState.interests,
                onNext = { onIntent(ProfileIntent.NextStep) },
                onBack = { onIntent(ProfileIntent.PrevStep) },
                onClick = { onIntent(ProfileIntent.SetProfileInterest(it)) },
            )
        }

        else -> {
            PermissionContent(
                modifier = modifier,
                onNext = { onIntent(ProfileIntent.NextStep) },
                onBack = { onIntent(ProfileIntent.PrevStep) },
            )
        }
    }
}
