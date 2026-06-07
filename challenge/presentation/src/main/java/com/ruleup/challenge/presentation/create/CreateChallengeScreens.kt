package com.ruleup.challenge.presentation.create

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeEffect
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeViewModel

/**
 * 챌린지 생성 플로우의 페이지별 화면. 입력(01)·AI 추천 확인(02)이 Activity 스코프의 단일
 * [CreateChallengeViewModel] 을 공유하므로 입력값이 페이지 이동에도 누적된다.
 */
@Composable
private fun sharedCreateChallengeViewModel(): CreateChallengeViewModel {
    val activity = LocalActivity.current as ComponentActivity
    return hiltViewModel(viewModelStoreOwner = activity)
}

/** ViewModel 의 단발성 에러를 토스트로 보여준다. */
@Composable
private fun CollectErrorEffect(viewModel: CreateChallengeViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is CreateChallengeEffect.ShowError) {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/** 01 · 챌린지 입력. */
@Composable
fun ChallengeCreateScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedCreateChallengeViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CollectErrorEffect(viewModel)

    ChallengeInputContent(
        modifier = modifier,
        title = state.title,
        description = state.description,
        isRecommending = state.isRecommending,
        onIntent = viewModel::onIntent,
    )
}

/** 02 · AI 추천 확인. */
@Composable
fun ChallengeConfirmScreen(modifier: Modifier = Modifier) {
    val viewModel = sharedCreateChallengeViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CollectErrorEffect(viewModel)

    ChallengeConfirmContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}
