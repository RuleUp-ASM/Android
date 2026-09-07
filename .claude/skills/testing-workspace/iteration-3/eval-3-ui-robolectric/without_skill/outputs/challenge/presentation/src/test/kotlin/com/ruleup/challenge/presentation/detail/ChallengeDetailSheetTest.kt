package com.ruleup.challenge.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.verification.domain.entity.PermissionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * 화면 위에 덮이는 두 시트 — 권한 안내와 가입 차단 안내.
 *
 * 둘 다 "왜 못 하는가"를 설명하는 자리라, 문구가 곧 기능이다. 특히 권한 시트는 **얻는 경로가 다른
 * 권한을 한 버튼으로 묶지 않는 것**이 계약이고, 차단 시트는 사유마다 다음 행동이 다르다.
 */
@RunWith(AndroidJUnit4::class)
class ChallengeDetailSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `권한 시트는 요청 경로가 다른 권한을 각자의 버튼으로 가른다`() {
        // 걸음·사용정보 접근을 "허용하기" 하나로 묶으면 사용자가 OS 다이얼로그를 아무리 눌러도
        // 그 권한은 생기지 않는다.
        val env =
            ChallengeDetailEnv(
                challengeRepository =
                    FakeChallengeRepository(
                        setup = setupFixture(requiredPermissions = listOf(LOCATION_TOKEN, USAGE_TOKEN)),
                    ),
                permissions =
                    permissionsFixture(
                        location = PermissionState.DENIED,
                        usageStats = PermissionState.DENIED,
                    ),
            )

        compose.showChallengeDetail(env)
        compose.tapText("권한 허용하기")

        compose.onNodeWithText("권한 허용이 필요해요").assertExists()
        compose.onNodeWithText("위치 (자동 위치 인증)").assertExists()
        compose.onNodeWithText("사용 기록 접근").assertExists()
        // 런타임으로 받는 것과 설정 화면으로 보내는 것이 각자 버튼을 갖는다.
        compose.onNodeWithText("허용하기").assertExists()
        compose.onNodeWithText("사용 정보 접근 설정 열기").assertExists()
    }

    @Test
    fun `권한 시트는 요청 전에 수집 범위를 좁혀서 알린다`() {
        // "위치 권한"만 보면 사용자는 상시 추적을 상상한다.
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setup = setupFixture()),
                permissions = permissionsFixture(location = PermissionState.DENIED),
            )

        compose.showChallengeDetail(env)
        compose.tapText("권한 허용하기")

        compose.onNodeWithText("등록한 장소 도착만 확인해요 · 이동 경로는 저장하지 않아요").assertExists()
    }

    @Test
    fun `권한 시트는 다음에로 닫을 수 있다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setup = setupFixture()),
                permissions = permissionsFixture(location = PermissionState.DENIED),
            )

        compose.showChallengeDetail(env)
        compose.tapText("권한 허용하기")
        compose.onNodeWithText("권한 허용이 필요해요").assertExists()

        compose.tapText("다음에")

        compose.onNodeWithText("권한 허용이 필요해요").assertDoesNotExist()
    }

    @Test
    fun `동시 참여 한도에 막히면 정리할 곳으로 데려간다`() {
        val env = blockedEnv(JoinBlockedException(JoinBlockReason.FREE_LIMIT))

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")

        compose.onNodeWithText("동시에 3개까지 참여할 수 있어요").assertExists()
        compose.onNodeWithText("참여 중인 챌린지를 정리하면 새로 들어올 수 있어요").assertExists()

        compose.tapText("참여 중인 챌린지 보기")
        assertEquals(AppRoutes.HOME, env.navigation.routes.last().path)
    }

    @Test
    fun `티어에 막히면 필요한 티어와 내 티어를 나란히 보여준다`() {
        val env =
            blockedEnv(
                error = JoinBlockedException(JoinBlockReason.TIER_GATE),
                minTier = Tier.GOLD,
                myDisplayTier = Tier.SILVER,
            )

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")

        compose.onNodeWithText("티어 조건을 만족하지 않아요").assertExists()
        compose.onNodeWithText("필요한 티어 GOLD · 내 티어 SILVER").assertExists()

        compose.tapText("내 티어 보기")
        assertEquals(AppRoutes.MY_HOME, env.navigation.routes.last().path)
    }

    @Test
    fun `재입장 대기는 언제부터 되는지만 말하고 탈퇴인지 강퇴인지는 말하지 않는다`() {
        val env =
            blockedEnv(
                JoinBlockedException(
                    reason = JoinBlockReason.REJOIN_COOLDOWN,
                    rejoinAvailableAt = "2026-09-07T00:00:00+09:00",
                ),
            )

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")

        compose.onNodeWithText("아직 다시 들어올 수 없어요").assertExists()
        compose.onNodeWithText("2026-09-07 부터 다시 참여할 수 있어요").assertExists()
    }

    @Test
    fun `차단된 사용자에게 사유를 설명하지 않는다`() {
        val env = blockedEnv(JoinBlockedException(JoinBlockReason.BANNED))

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")

        compose.onNodeWithText("이 챌린지에는 참여할 수 없어요").assertExists()
        compose.onNodeWithText("자세한 내용은 안내드릴 수 없어요").assertExists()
    }

    @Test
    fun `앱이 모르는 사유는 일반 안내로 떨어뜨린다`() {
        // 서버가 사유를 추가해도 빈 시트가 뜨면 안 된다.
        val env = blockedEnv(JoinBlockedException(reason = null))

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")

        compose.onNodeWithText("지금은 참여할 수 없어요").assertExists()
        compose.onNodeWithText("잠시 후 다시 시도해 주세요").assertExists()
    }

    @Test
    fun `차단 시트를 닫으면 상세로 돌아온다`() {
        val env = blockedEnv(JoinBlockedException(JoinBlockReason.BANNED))

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")
        compose.tapText("닫기")

        compose.onNodeWithText("이 챌린지에는 참여할 수 없어요").assertDoesNotExist()
        compose.onNodeWithText("참여하기").assertExists()
    }

    /** 셋업을 못 받는 상태(=CTA 가 바로 "참여하기")에서 가입만 막히도록 세운다. */
    private fun blockedEnv(
        error: Throwable,
        minTier: Tier? = null,
        myDisplayTier: Tier? = Tier.SILVER,
    ) = ChallengeDetailEnv(
        challengeRepository =
            FakeChallengeRepository(
                detail = detailFixture(minTier = minTier, myDisplayTier = myDisplayTier),
                setupError = IllegalStateException("setup 없음"),
                joinError = error,
            ),
    )
}
