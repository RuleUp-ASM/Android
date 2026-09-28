package com.ruleup.challenge.presentation.common

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.VerificationAccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/** 수집 동의와 기기 권한의 통합 안내. */
@RunWith(RobolectricTestRunner::class)
class VerificationAccessContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `위치와 건강 동의 및 필요한 권한이 한 안내에 표시된다`() {
        compose.renderScreen { VerificationAccessContent(access(), false, {}, {}) }

        compose.onNodeWithText("자동 인증 설정").assertExists()
        compose.onNodeWithText("위치정보 수집·이용 동의", substring = true).assertExists()
        compose.onNodeWithText("건강정보 수집·이용 동의", substring = true).assertExists()
        compose.onNodeWithText("위치 접근", substring = true).assertExists()
        compose.onNodeWithText("동의하고 계속하기").assertExists()
    }

    @Test
    fun `동의를 마치고 건강 권한만 남으면 같은 안내에서 권한을 요청한다`() {
        var continued = 0
        compose.renderScreen {
            VerificationAccessContent(access().copy(missingConsents = emptyList(), missingPermissions = listOf("READ_STEPS")), false, {
                continued++
            }, {})
        }

        compose.onNodeWithText("동의하고 계속하기").assertDoesNotExist()
        compose.onNodeWithText("헬스 커넥트 권한 허용").clickPastGuard()
        assertEquals(1, continued)
    }

    @Test
    fun `저장 중에는 동의 버튼을 다시 누를 수 없다`() {
        compose.renderScreen { VerificationAccessContent(access(), true, {}, {}) }

        compose.onNodeWithText("확인 중…").assertIsNotEnabled()
    }

    @Test
    fun `다음에 하기를 누르면 진행하지 않고 닫는다`() {
        var continued = 0
        var dismissed = 0
        compose.renderScreen { VerificationAccessContent(access(), false, { continued++ }, { dismissed++ }) }

        compose.onNodeWithText("다음에 할게요").clickPastGuard()
        assertEquals(0, continued)
        assertEquals(1, dismissed)
    }

    private fun access() =
        VerificationAccess(
            permissions =
                PermissionSnapshot(
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                    PermissionState.DENIED,
                ),
            missingPermissions = listOf("ACCESS_FINE_LOCATION", "READ_STEPS"),
            missingConsents = listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO),
        )
}
