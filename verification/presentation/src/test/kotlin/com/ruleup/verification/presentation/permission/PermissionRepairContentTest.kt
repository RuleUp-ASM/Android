package com.ruleup.verification.presentation.permission

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.ui.helper.LocalObservability
import com.ruleup.verification.domain.entity.PermissionRequestKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 권한 재연결. 이 화면이 못 하면 **인증이 조용히 멈추다 강퇴로 간다** — 사용자는 스스로
 * 알아챌 방법이 없다. 그래서 "언제까지 무엇을 하지 않으면 어떻게 되는가"를 시점과 함께 말하는
 * 것이 곧 기능이다. "권한이 필요해요"로는 급한 줄 모른다.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionRepairContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `끊긴 권한이 있으면 강퇴 시점을 함께 경고한다`() {
        // 시점이 없으면 급한 줄 모르고 넘긴다.
        render(rows = listOf(row(granted = false)))

        compose.onNodeWithText("오늘 밤 12시까지 다시 허용하지 않으면 챌린지에서 나가게 돼요").assertExists()
        compose.onNodeWithText("그동안 인증이 되지 않아 실패로 기록될 수 있어요").assertExists()
    }

    @Test
    fun `끊긴 권한이 없으면 경고하지 않는다`() {
        // 멀쩡한데 경고하면 사용자가 없던 사고를 복구하러 설정을 헤맨다.
        render(rows = listOf(row(granted = true)))

        compose.onNodeWithText("오늘 밤 12시까지 다시 허용하지 않으면 챌린지에서 나가게 돼요").assertDoesNotExist()
    }

    @Test
    fun `아직 권한을 못 물었으면 아무것도 끊겼다고 하지 않는다`() {
        // 모른다고 "다 끊겼다"로 그리면 없던 사고가 된다.
        render(rows = emptyList())

        compose.onNodeWithText("오늘 밤 12시까지 다시 허용하지 않으면 챌린지에서 나가게 돼요").assertDoesNotExist()
    }

    private fun row(granted: Boolean) =
        RepairRow(
            label = "위치",
            purpose = "헬스장 도착을 확인해요",
            granted = granted,
            kind = PermissionRequestKind.RUNTIME,
            runtimePermissions = listOf("android.permission.ACCESS_FINE_LOCATION"),
        )

    private fun render(rows: List<RepairRow>) {
        compose.setContent {
            RuleUpTheme {
                CompositionLocalProvider(LocalObservability provides testObservability()) {
                    PermissionRepairContent(
                        rows = rows,
                        broken = rows.filter { !it.granted },
                        onIntent = {},
                        onFix = {},
                    )
                }
            }
        }
    }
}
