package com.ruleup.profile.presentation.member

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MemberProfile
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.common.SuspendedBlock
import com.ruleup.profile.presentation.member.viewmodel.MemberProfileIntent
import com.ruleup.profile.presentation.member.viewmodel.MemberProfileState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 타인 프로필. */
@RunWith(RobolectricTestRunner::class)
class MemberProfileContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `남의 통계는 숫자 대신 공개되지 않는다고 말한다`() {
        render(state(profile = profile()))

        compose.onNodeWithText("진행 중인 챌린지·통계·티어 점수는 공개되지 않아요").assertExists()
    }

    @Test
    fun `신고가 제재로 막히면 진입점 대신 이유를 말한다`() {
        render(state(profile = profile(), reportBlock = SuspendedBlock(until = "2026. 10. 15 00:00")))

        compose.onNodeWithText("지금은 신고할 수 없어요").assertExists()
    }

    @Test
    fun `정지 안내를 확인하면 시트를 닫는 의도가 올라간다`() {
        val intents = mutableListOf<MemberProfileIntent>()
        render(state(profile = profile(), reportBlock = SuspendedBlock(until = null)), onIntent = { intents += it })

        compose.onNodeWithText("확인").clickPastGuard()

        assertEquals(listOf<MemberProfileIntent>(MemberProfileIntent.DismissReportBlock), intents)
    }

    @Test
    fun `제재 이력 보기는 사유의 원본 화면으로 보낸다`() {
        val intents = mutableListOf<MemberProfileIntent>()
        render(state(profile = profile(), reportBlock = SuspendedBlock(until = null)), onIntent = { intents += it })

        compose.onNodeWithText("제재 이력 보기").clickPastGuard()

        assertTrue(intents.contains(MemberProfileIntent.OpenSanctionHistory))
    }

    private fun profile(blocked: Boolean = false) =
        MemberProfile(
            user =
                com.ruleup.domain.entity.user
                    .User("u-2", "지현", null),
            tier = Tier.entries.first(),
            completedChallengeCount = 3,
            withdrawn = false,
            blocked = blocked,
        )

    private fun state(
        profile: MemberProfile? = null,
        reportBlock: SuspendedBlock? = null,
    ) = MemberProfileState.initial.copy(isLoading = false, profile = profile, reportBlock = reportBlock)

    private fun render(
        state: MemberProfileState,
        onIntent: (MemberProfileIntent) -> Unit = {},
    ) = compose.renderScreen { MemberProfileContent(state = state, onIntent = onIntent) }
}
