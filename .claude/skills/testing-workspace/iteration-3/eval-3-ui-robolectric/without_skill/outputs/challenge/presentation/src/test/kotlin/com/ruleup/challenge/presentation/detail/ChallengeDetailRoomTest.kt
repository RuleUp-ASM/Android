package com.ruleup.challenge.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.verification.domain.entity.PermissionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 방 홈이 내려온 뒤의 화면 계약 — 같은 라우트가 3탭 방 상세로 바뀐다.
 *
 * 방 홈은 그룹 챌린지의 ACTIVE 멤버에게만 내려오므로, 여기서는 `room` 이 있는 경우만 다룬다.
 * `room` 이 없는 멤버(솔로·시작 전 그룹)는 [ChallengeDetailScreenTest] 가 맡는다.
 */
@RunWith(AndroidJUnit4::class)
class ChallengeDetailRoomTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun memberEnv(
        myRole: MemberRole = MemberRole.MEMBER,
        permissionState: PermissionState = PermissionState.GRANTED,
    ) = ChallengeDetailEnv(
        challengeRepository =
            FakeChallengeRepository(
                detail = detailFixture(myRole = myRole),
                setup = setupFixture(),
            ),
        roomRepository =
            FakeRoomRepository(
                room = roomFixture(myRole = myRole),
                ranking = rankingFixture(),
            ),
        permissions = permissionsFixture(location = permissionState),
    )

    @Test
    fun `방에 들어오면 제목이 방 이름이 되고 세 탭이 선다`() {
        val env = memberEnv()

        compose.showChallengeDetail(env)

        compose.onNodeWithText("아침 6시 30분 기상").assertIsDisplayed()
        compose.onNodeWithText("정보").assertIsDisplayed()
        compose.onNodeWithText("피드").assertIsDisplayed()
        compose.onNodeWithText("랭킹").assertIsDisplayed()
        // 방 안에서는 공개 상세 상단바("챌린지")를 더 쓰지 않는다.
        compose.onNodeWithText("챌린지").assertDoesNotExist()
        // 이미 멤버라 하단 CTA 는 없다.
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `정보 탭 헤더는 카테고리와 남은 일수, 내 달성률을 편다`() {
        val env = memberEnv()

        compose.showChallengeDetail(env)

        compose.onNodeWithText("기상·수면").assertIsDisplayed()
        compose.onNodeWithText("D-10").assertIsDisplayed()
        // 방 안 랭킹의 내 성공률 0.75 가 곧 헤더의 달성률이다.
        compose.onNodeWithText("75%").assertIsDisplayed()
        compose.onNodeWithText("오늘 내 인증").assertExists()
    }

    @Test
    fun `랭킹이 미등재면 달성률 자리에 0퍼센트 대신 하이픈을 둔다`() {
        // 0% 로 채우면 아직 집계되지 않은 것이 실패로 보인다.
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(detail = detailFixture(myRole = MemberRole.MEMBER)),
                roomRepository = FakeRoomRepository(room = roomFixture(), ranking = rankingFixture(mySuccessRate = null)),
            )

        compose.showChallengeDetail(env)

        assertTrue(compose.onAllNodesWithText("-").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("0%").assertDoesNotExist()
        compose.onNodeWithText("75%").assertDoesNotExist()
    }

    @Test
    fun `종료일이 지나면 D-0 대신 D-day 로 적는다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(detail = detailFixture(myRole = MemberRole.MEMBER)),
                roomRepository = FakeRoomRepository(room = roomFixture(remainingDays = 0), ranking = rankingFixture()),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("D-day").assertIsDisplayed()
    }

    @Test
    fun `피드 탭은 소식이 없으면 기다리는 중임을 알린다`() {
        val env = memberEnv()

        compose.showChallengeDetail(env)
        compose.tapText("피드")

        compose.onNodeWithText("곧 멤버들의 인증 소식이 올라와요").assertIsDisplayed()
        // 피드로 옮기면 정보 탭 헤더는 접힌다 — 목록이 화면을 꽉 채워야 한다.
        compose.onNodeWithText("D-10").assertDoesNotExist()
    }

    @Test
    fun `봇방장 방의 빈 피드는 방장 되기로 유도한다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(detail = detailFixture(myRole = MemberRole.MEMBER)),
                roomRepository =
                    FakeRoomRepository(
                        room = roomFixture(ownerType = OwnerType.BOT),
                        ranking = rankingFixture(),
                    ),
            )

        compose.showChallengeDetail(env)
        compose.tapText("피드")

        compose.onNodeWithText("아직 소식이 없어요\n이 방은 방장 자리가 비어 있어요").assertIsDisplayed()
        compose.onNodeWithText("방장 되기").assertIsDisplayed()
    }

    @Test
    fun `랭킹 탭은 비교 단위를 고르게 하고 등재자가 없으면 그렇게 말한다`() {
        val env = memberEnv()

        compose.showChallengeDetail(env)
        compose.tapText("랭킹")

        compose.onNodeWithText("방 순위").assertIsDisplayed()
        compose.onNodeWithText("아직 순위에 오른 멤버가 없어요").assertIsDisplayed()
    }

    @Test
    fun `참여 중인데 권한이 꺼져 있으면 배너로 알리고 복구 화면으로 보낸다`() {
        // 자동 인증은 조용히 멈춘다 — 배너가 없으면 매일 실패가 쌓이다 강퇴로 간다.
        val env = memberEnv(permissionState = PermissionState.DENIED)

        compose.showChallengeDetail(env)
        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertIsDisplayed()
        compose.tapText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기")

        assertEquals(AppRoutes.VERIFICATION_PERMISSION_REPAIR, env.navigation.routes.last().path)
    }

    @Test
    fun `권한이 멀쩡하면 배너를 띄우지 않는다`() {
        val env = memberEnv()

        compose.showChallengeDetail(env)

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `방장에게만 상단바 메뉴를 열고 챌린지 수정으로 보낸다`() {
        val env = memberEnv(myRole = MemberRole.OWNER)

        compose.showChallengeDetail(env)
        compose.tapIcon("더 보기")
        compose.onNodeWithText("챌린지 수정").assertIsDisplayed()
        compose.tapText("챌린지 수정")

        assertEquals(AppRoutes.CHALLENGE_SETTINGS, env.navigation.routes.last().path)
    }

    @Test
    fun `방장이 아니면 관리 메뉴 자리를 만들지 않는다`() {
        // 공동 관리자도 규칙을 바꿀 수 없다 — 눌러서 알게 하지 않는다.
        val env = memberEnv(myRole = MemberRole.MANAGER)

        compose.showChallengeDetail(env)

        compose.onNodeWithContentDescription("더 보기").assertDoesNotExist()
    }

    @Test
    fun `방 안에서도 뒤로는 뒤로 보낸다`() {
        val env = memberEnv()

        compose.showChallengeDetail(env)
        compose.tapIcon("뒤로")

        assertEquals(1, env.navigation.backCount)
    }
}
