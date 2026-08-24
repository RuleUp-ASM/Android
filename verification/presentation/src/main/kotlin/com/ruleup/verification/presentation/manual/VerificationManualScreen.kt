package com.ruleup.verification.presentation.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.rememberSingleClick
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.presentation.manual.viewmodel.VerificationManualEffect
import com.ruleup.verification.presentation.manual.viewmodel.VerificationManualIntent
import com.ruleup.verification.presentation.manual.viewmodel.VerificationManualViewModel

/**
 * 수동 인증 제출 화면(명세 §6.5, VF-04). SELF_CHECK 가 주(强), PHOTO 는 비강조.
 * 이미지 업로드는 기존 챌린지 이미지 패턴 재사용(후속).
 */
@Composable
fun VerificationManualScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: VerificationManualViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VerificationManualEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("오늘 인증", fontWeight = FontWeight.Bold)
        Text("자동으로 판정할 수 없는 인증은 직접 체크해요. 오늘 안에만 할 수 있어요.")

        Button(
            onClick =
                rememberSingleClick {
                    viewModel.onIntent(VerificationManualIntent.Submit(challengeId))
                },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("자가 체크로 인증")
        }
    }
}
