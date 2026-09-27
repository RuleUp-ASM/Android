package com.ruleup.challenge.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.verification.domain.entity.PermissionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * 비멤버가 보는 **공개 상세**(방 홈이 없는 상태)의 화면 계약.
 *
 * 이 화면의 고유 로직은 거의 전부 하단 CTA 한 줄에 몰려 있다 — 서버 셋업 요구사항과 기기 권한
 * 현황을 합쳐 "다음에 뭘 시켜야 하는가"를 정하는 사다리(권한 → 앱 등록 → 장소 등록 → 참여)다.
 * 사다리의 각 칸과, 칸을 건너뛰게 만드는 예외(수동 인증·조회 실패)를 모두 지난다.
 */
@RunWith(AndroidJUnit4::class)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `상세를 못 불러오면 서버 문구를 그대로 두고 참여 버튼을 만들지 않는다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(detailError = RuntimeException("네트워크가 불안정해요")),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("네트워크가 불안정해요").assertIsDisplayed()
        // 상세가 없으면 무엇에 참여하는지 알 수 없다 — 버튼 자체를 만들지 않는다.
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `공개 상세는 방장과 참여 인원, 기간·정원·참여 형태·인증 방식을 편다`() {
        val env = ChallengeDetailEnv()

        compose.showChallengeDetail(env)

        compose.onNodeWithText("아침 6시 30분 기상").assertIsDisplayed()
        compose.onNodeWithText("규칙왕 · 4명 참여 중").assertIsDisplayed()
        compose.onNodeWithText("일어나서 물 한 잔 마시기").assertIsDisplayed()
        compose.onNodeWithText("2026-08-01 ~ 2026-08-31").assertExists()
        compose.onNodeWithText("4 / 10명").assertExists()
        compose.onNodeWithText("그룹").assertExists()
        // 서버가 준 표시 문구가 있으면 "자동 인증" 같은 요약으로 덮지 않는다.
        compose.onNodeWithText("기상 06:30 ±10분 내 10걸음").assertExists()
    }

    @Test
    fun `봇방장 방은 방장 자리가 비어 있음을 숨기지 않는다`() {
        val env = ChallengeDetailEnv(FakeChallengeRepository(detail = detailFixture(ownerNickname = null)))

        compose.showChallengeDetail(env)

        compose.onNodeWithText("방장 없음 · 4명 참여 중").assertIsDisplayed()
    }

    @Test
    fun `필요한 권한이 꺼져 있으면 CTA 가 권한 허용하기가 된다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setup = setupFixture()),
                permissions = permissionsFixture(location = PermissionState.DENIED),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("권한 허용하기").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `권한이 끝나면 다음 칸은 대상 앱 등록이고 누르면 등록 화면으로 보낸다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setup = setupFixture(requiresTargetPackages = true)),
            )

        compose.showChallengeDetail(env)
        compose.onNodeWithText("앱 등록하기").assertIsDisplayed()
        compose.tapText("앱 등록하기")

        val route = env.navigation.routes.last()
        assertEquals(AppRoutes.CHALLENGE_TARGETS, route.path)
        assertEquals(TEST_CHALLENGE_ID, route.args["challengeId"])
    }

    @Test
    fun `앱까지 등록됐으면 남은 칸은 인증 장소이고 등록한 앱을 함께 실어 보낸다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository =
                    FakeChallengeRepository(
                        setup = setupFixture(requiresTargetPackages = true, requiresAnchors = true),
                    ),
                targetAppStore = FakeTargetAppStore(listOf("com.instagram.android")),
            )

        compose.showChallengeDetail(env)
        compose.onNodeWithText("인증 장소 등록하기").assertIsDisplayed()
        compose.tapText("인증 장소 등록하기")

        val route = env.navigation.routes.last()
        assertEquals(AppRoutes.VERIFICATION_LOCATION, route.path)
        assertEquals(TEST_CHALLENGE_ID, route.args["challengeId"])
        // 앵커 등록 화면이 setup 제출에 함께 담아야 해서 여기서 넘긴다.
        assertEquals("com.instagram.android", route.args["targetPackages"])
    }

    @Test
    fun `앵커가 이미 바인딩돼 있으면 장소 등록을 다시 시키지 않는다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository =
                    FakeChallengeRepository(
                        setup = setupFixture(requiresAnchors = true, anchorsConfigured = true),
                    ),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("참여하기").assertIsDisplayed()
        compose.onNodeWithText("인증 장소 등록하기").assertDoesNotExist()
    }

    @Test
    fun `수동 인증 방은 권한을 묻지 않고 바로 참여시킨다`() {
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setup = setupFixture(manual = true)),
                // 자동 인증이었다면 막혔을 상태여도 수동 방은 권한을 쓸 일이 없다.
                permissions = permissionsFixture(location = PermissionState.DENIED),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("참여하기").assertIsDisplayed()
        compose.onNodeWithText("권한 허용하기").assertDoesNotExist()
    }

    @Test
    fun `셋업 요구사항을 못 받아도 참여를 막지 않는다`() {
        // 조회 실패가 곧 차단이 되면, 서버가 잠깐 흔들릴 때 아무도 못 들어온다.
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setupError = IllegalStateException("501")),
                permissions = permissionsFixture(location = PermissionState.DENIED),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("참여하기").assertIsDisplayed()
    }

    @Test
    fun `권한 현황을 아직 모르면 잠그지 않는다`() {
        // "꺼져 있다"와 "못 물어봤다"는 다르다. 후자를 차단으로 접으면 조회 실패가 곧 차단이 된다.
        val env =
            ChallengeDetailEnv(
                challengeRepository = FakeChallengeRepository(setup = setupFixture()),
                permissions = null,
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("참여하기").assertIsDisplayed()
        compose.onNodeWithText("권한 허용하기").assertDoesNotExist()
    }

    @Test
    fun `비공개 방은 참여 버튼 대신 초대 링크 안내를 둔다`() {
        val env =
            ChallengeDetailEnv(
                FakeChallengeRepository(detail = detailFixture(joinBlockReason = JoinBlockReason.PRIVATE_INVITE_ONLY)),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("초대 링크로만 들어올 수 있는 챌린지예요").assertIsDisplayed()
        // 눌러봐야 막히는 버튼을 두면 사용자가 원인을 오해한다.
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `복제 가능한 방에만 템플릿 버튼을 둔다`() {
        val env = ChallengeDetailEnv(FakeChallengeRepository(detail = detailFixture(cloneable = true)))

        compose.showChallengeDetail(env)

        compose.onNodeWithText("이 템플릿으로 만들기").assertIsDisplayed()
    }

    @Test
    fun `복제할 수 없는 방에는 템플릿 버튼을 만들지 않는다`() {
        val env = ChallengeDetailEnv()

        compose.showChallengeDetail(env)

        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `이미 멤버면 방 홈을 못 받아도 참여 버튼이 다시 뜨지 않는다`() {
        // 방 홈은 GROUP·ACTIVE 일 때만 내려온다 — 그것으로 참여 여부를 판단하면 시작 전 그룹 방에서
        // 멤버에게 "참여하기"가 다시 뜬다. 판단 근거는 myRole 하나뿐이다.
        val env =
            ChallengeDetailEnv(
                FakeChallengeRepository(detail = detailFixture(myRole = MemberRole.MEMBER, cloneable = true)),
            )

        compose.showChallengeDetail(env)

        compose.onNodeWithText("아침 6시 30분 기상").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `비멤버 상단바는 챌린지이고 뒤로가 뒤로 보낸다`() {
        val env = ChallengeDetailEnv()

        compose.showChallengeDetail(env)
        compose.onNodeWithText("챌린지").assertIsDisplayed()
        compose.tapIcon("뒤로")

        assertEquals(1, env.navigation.backCount)
    }

    @Test
    fun `참여에 실패하면 서버 문구를 알리고 버튼을 되돌린다`() {
        val env = ChallengeDetailEnv(FakeChallengeRepository(joinError = RuntimeException("이미 끝난 챌린지예요")))

        compose.showChallengeDetail(env)
        compose.tapText("참여하기")

        assertEquals(1, env.challengeRepository.joinCount)
        assertEquals(listOf("이미 끝난 챌린지예요"), env.messages.toasts)
        // 실패는 막다른 길이 아니다 — 다시 누를 수 있어야 한다.
        compose.onNodeWithText("참여하기").assertIsDisplayed()
    }
}
