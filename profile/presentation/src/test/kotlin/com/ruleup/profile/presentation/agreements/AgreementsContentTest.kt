package com.ruleup.profile.presentation.agreements

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.presentation.agreements.viewmodel.AgreementsIntent
import com.ruleup.profile.presentation.agreements.viewmodel.AgreementsState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 약관 · 개인정보. **필수 약관은 철회할 수 없다** — 토글을 그리면 눌러 놓고 서버에 거절당하는
 * 스위치가 되므로, 아예 두지 않고 왜 없는지를 말한다.
 *
 * "동의 안 함"과 "받은 적 없음"은 다른 사실이라 문구도 갈라야 한다.
 */
@RunWith(RobolectricTestRunner::class)
class AgreementsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `필수 약관에는 토글 대신 필수 표기를 둔다`() {
        render(AgreementsState.initial.copy(isLoading = false, status = status()))

        compose.onNodeWithText("서비스 이용약관").assertExists()
        compose.onNodeWithText("필수").assertExists()
        compose.onNodeWithText("필수 약관은 철회할 수 없어요", substring = true).assertExists()
    }

    @Test
    fun `한 번도 동의한 적 없는 항목은 철회와 다르게 말한다`() {
        render(
            AgreementsState.initial.copy(
                isLoading = false,
                status = status(healthVersion = null),
            ),
        )

        compose.onNodeWithText("아직 받지 않았어요").assertExists()
    }

    @Test
    fun `재동의가 필요하면 몇 건인지와 함께 배너를 띄운다`() {
        render(
            AgreementsState.initial.copy(
                isLoading = false,
                status = status(reconsent = listOf(AgreementType.TERMS_OF_SERVICE)),
            ),
        )

        compose.onNodeWithText("다시 동의가 필요한 약관이 1건 있어요").assertExists()
        compose.onNodeWithText("다시 동의하기").assertExists()
    }

    @Test
    fun `재동의할 게 없으면 배너를 띄우지 않는다`() {
        render(AgreementsState.initial.copy(isLoading = false, status = status()))

        compose.onNodeWithText("다시 동의하기").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(AgreementsState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    private fun status(
        healthVersion: String? = "1.0",
        reconsent: List<AgreementType> = emptyList(),
    ) = AgreementStatus(
        agreements =
            listOf(
                AgreementState(AgreementType.TERMS_OF_SERVICE, required = true, agreed = true, version = "1.2", agreedAt = null),
                AgreementState(AgreementType.MARKETING, required = false, agreed = false, version = "1.0", agreedAt = null),
                AgreementState(AgreementType.HEALTH_INFO, required = false, agreed = false, version = healthVersion, agreedAt = null),
            ),
        reconsentRequired = reconsent,
    )

    private fun render(
        state: AgreementsState,
        onIntent: (AgreementsIntent) -> Unit = {},
    ) {
        compose.renderScreen { AgreementsContent(state = state, onIntent = onIntent) }
    }
}
