package com.ruleup.challenge.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.presentation.create.component.rememberPermissionRequester
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeEffect
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeViewModel
import com.ruleup.ui.helper.LocalMessageHelper

/**
 * 챌린지 생성 플로우의 페이지별 화면. 입력(01)·AI 추천 확인(02)이 Activity 스코프의 단일
 * [CreateChallengeViewModel] 을 공유하므로 입력값이 페이지 이동에도 누적된다.
 */
@Composable
private fun sharedCreateChallengeViewModel(): CreateChallengeViewModel =
    hiltViewModel(viewModelStoreOwner = rememberActivityViewModelStoreOwner())

/** 권한 요청은 OS 다이얼로그가 끝날 때까지 suspend 되며, 허용 결과를 [CreateChallengeIntent.PermissionsResult] 로 되돌린다. */
@Composable
private fun CollectEffects(viewModel: CreateChallengeViewModel) {
    val messageHelper = LocalMessageHelper.current
    val permissionRequester = rememberPermissionRequester()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateChallengeEffect.ShowError -> messageHelper.showToast(effect.message)
                is CreateChallengeEffect.RequestPermissions -> {
                    val granted = permissionRequester.request(effect.tokens)
                    viewModel.onIntent(CreateChallengeIntent.PermissionsResult(granted))
                }
            }
        }
    }
}

/** 01 · 챌린지 입력. */
@Composable
fun ChallengeCreateScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedCreateChallengeViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CollectEffects(viewModel)
    LaunchedEffect(Unit) { viewModel.onIntent(CreateChallengeIntent.Load) }

    ChallengeInputContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}

/** 02 · AI 추천 확인. */
@Composable
fun ChallengeConfirmScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedCreateChallengeViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CollectEffects(viewModel)

    ChallengeConfirmContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}
