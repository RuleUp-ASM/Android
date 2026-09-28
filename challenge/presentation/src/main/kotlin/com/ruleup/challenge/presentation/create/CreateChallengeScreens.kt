package com.ruleup.challenge.presentation.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.presentation.common.rememberVerificationPermissionRequester
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeEffect
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.ui.helper.LocalMessageHelper

/** 챌린지 생성 플로우의 페이지별 화면. */
@Composable
private fun sharedCreateChallengeViewModel(): CreateChallengeViewModel =
    hiltViewModel(viewModelStoreOwner = rememberActivityViewModelStoreOwner())

/** 시스템 권한창·설정에서 돌아오면 기기 권한을 다시 조회한다. */
@Composable
private fun CollectEffects(viewModel: CreateChallengeViewModel) {
    val messageHelper = LocalMessageHelper.current
    val requestPermissions =
        rememberVerificationPermissionRequester {
            viewModel.onIntent(CreateChallengeIntent.VerificationPermissionsReturned)
        }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateChallengeEffect.ShowError -> messageHelper.showToast(effect.message)
                is CreateChallengeEffect.RequestPermissions -> {
                    requestPermissions(effect.tokens)
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
    TtiScreenEffect(loading = state.isLoadingTemplates)

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
    TtiScreenEffect(loading = state.isLoadingTemplates)

    CollectEffects(viewModel)
    LaunchedEffect(Unit) { viewModel.onIntent(CreateChallengeIntent.ConfirmOpened) }

    ChallengeConfirmContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}
