package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.verification.domain.entity.PermissionState
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 상세 화면이 상태를 어떻게 그리고 조작을 어떤 의도로 올리는지.
 *
 * 기대 문구는 Figma 「🏁 23 · 최종 · 전체 화면」(`1134:2`)에서 가져왔다 —
 * 공개 상세 `1134:1291`, 방 상세 정보/피드/랭킹 `1134:143`·`1134:231`·`1134:326`.
 * 구현에서 문구를 베끼면 "오늘 이후의 변경"만 잡고 "처음부터 틀렸다"는 영영 못 잡는다.
 *
 * **디자인 미확정**: 권한 끊김 배너는 최종 페이지에 프레임이 없다(`1134:997` 은 재연결 *화면*이지
 * 방 안 배너가 아니다). 권한 배너 3건만 구현 문구로 임시 고정했고 `TEST_STRATEGY.md` 미검증 목록에 올렸다.
 *
 * 탭 안쪽(피드 카드·랭킹 행·오늘 인증 카드)은 각자의 Composable 이 그리며 이미 케이스 층 테스트가
 * 있다. 여기서는 **어느 탭이 열리는가**까지만 본다 — 같은 경로를 두 층에서 세면 테스트가 곱으로 는다.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun passGlobalClickGuard() {
        advanceClockPastGuard()
    }

    // ---- 상태 → 화면 ----

    @Test
    fun `불러오는 중에는 오류 문구를 먼저 보여주지 않는다`() {
        // 로딩과 실패는 다른 사실이다. 스켈레톤 자리에 "불러오지 못했어요"가 뜨면 사용자는
        // 아직 오지 않은 응답을 실패로 읽고 화면을 떠난다.
        compose.showDetail(state(detail = null, isLoading = true))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `상세를 못 받으면 서버가 준 이유를 그대로 보여준다`() {
        compose.showDetail(state(detail = null, errorMessage = "찾을 수 없는 챌린지예요."))

        compose.onNodeWithText("찾을 수 없는 챌린지예요.").assertIsDisplayed()
    }

    @Test
    fun `상세를 못 받았는데 이유도 없으면 빈 화면 대신 안내를 남긴다`() {
        // 이유 없는 실패에서 화면이 비면 사용자는 앱이 멈춘 줄 안다.
        compose.showDetail(state(detail = null, errorMessage = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `비멤버에게는 공개 상세와 참여하기 버튼을 보여준다`() {
        // Figma 공개 상세 프리뷰(1134:1291) — 하단 CTA 는 "참여하기" 하나다.
        compose.showDetail(state(detail = detail(title = "평일 아침 헬스장 출석")))

        compose.onNodeWithText("평일 아침 헬스장 출석").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertIsDisplayed()
    }

    @Test
    fun `참여 중인 방은 정보·피드·랭킹 세 탭으로 연다`() {
        // Figma 1134:181 Tabs — 정보 · 피드 · 랭킹. 비멤버 공개 상세에는 탭이 없다.
        compose.showDetail(state(detail = detail(myRole = MemberRole.MEMBER), room = room()))

        compose.onNodeWithText("정보").assertIsDisplayed()
        compose.onNodeWithText("피드").assertIsDisplayed()
        compose.onNodeWithText("랭킹").assertIsDisplayed()
    }

    @Test
    fun `정보 탭을 벗어나면 달성률 헤더를 접는다`() {
        // Figma 상세·피드(1134:243)의 Head 에는 AppBar 와 Tabs 만 있다 — 목록이 화면을 꽉 채워야 해서
        // D-day·달성률 블록을 접는다. 남겨 두면 첫 화면에 보이는 소식이 한 건으로 줄어든다.
        compose.showDetail(
            state(detail = detail(myRole = MemberRole.MEMBER), room = room(), selectedTab = RoomTab.FEED),
        )

        compose.onNodeWithText("피드").assertIsDisplayed()
        compose.onNodeWithText("내 달성률").assertDoesNotExist()
        compose.onNodeWithText("종료까지").assertDoesNotExist()
    }

    @Test
    fun `탭을 누르면 그 탭으로 전환하는 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        compose.showDetail(
            state(detail = detail(myRole = MemberRole.MEMBER), room = room()),
            onIntent = { intents += it },
        )

        compose.onNodeWithText("랭킹").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.SelectTab(RoomTab.RANKING)), intents)
    }

    // ---- 하단 CTA ----

    @Test
    fun `이미 참여한 사람에게는 참여 버튼을 만들지 않는다`() {
        // 참여 여부는 myRole 하나로 판단한다. 방 홈(room)은 GROUP·ACTIVE 일 때만 내려오므로
        // 솔로 방이나 시작 전 그룹 방에서는 멤버인데도 room 이 null 이다 — room 으로 판단하면
        // 이미 들어간 방에 "참여하기"가 다시 뜬다. Figma 상세·정보(1134:143)에도 CTA 가 없다.
        compose.showDetail(state(detail = detail(myRole = MemberRole.MEMBER), room = null))

        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `초대 링크 전용 방에서는 참여 버튼 대신 이유를 남긴다`() {
        // 눌러 봐야 막히는 버튼을 두면 사용자는 자기 자격 문제로 오해한다.
        compose.showDetail(
            state(detail = detail(joinBlockReason = JoinBlockReason.PRIVATE_INVITE_ONLY)),
        )

        compose.onNodeWithText("참여하기").assertDoesNotExist()
        compose.onNodeWithText("초대 링크로만 들어올 수 있는 챌린지예요").assertIsDisplayed()
    }

    @Test
    fun `참여 요청 중에는 버튼을 잠가 두 번 참여되지 않게 한다`() {
        compose.showDetail(state(isJoining = true))

        compose.onNodeWithText("참여하는 중…").assertIsNotEnabled()
    }

    @Test
    fun `참여 버튼을 누르면 셋업 단계에 맞는 동작이 실행된다`() {
        // 화면은 무엇을 할지 고르지 않는다 — 권한·앱 등록·앵커·참여 중 어느 단계인지는 바깥이 정하고,
        // 여기서는 그 콜백이 실제로 걸려 있는지만 본다.
        var ctaCount = 0
        compose.showDetail(state(), ctaLabel = "권한 허용하기", onCta = { ctaCount++ })

        compose.onNodeWithText("권한 허용하기").clickPastGuard()

        assertEquals(1, ctaCount)
    }

    @Test
    fun `복제할 수 없는 방에는 템플릿 버튼을 만들지 않는다`() {
        // 복제는 공개 그룹만 된다. 비활성 버튼을 남겨 두면 눌러 보고 서버 403 을 만난다.
        compose.showDetail(state(detail = detail(cloneable = false)))

        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `템플릿으로 만들기를 누르면 복제 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        compose.showDetail(state(detail = detail(cloneable = true)), onIntent = { intents += it })

        compose.onNodeWithText("이 템플릿으로 만들기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.CloneChallenge), intents)
    }

    // ---- 권한 끊김 (디자인 미확정 — 문구는 구현에서 임시 고정) ----

    @Test
    fun `참여 중에 권한이 끊기면 인증이 멈춘 사실을 알린다`() {
        // 권한이 꺼지면 인증은 조용히 멈춘다. 알려주지 않으면 사용자는 매일 실패가 쌓여
        // 강퇴에 이를 때까지 원인을 모른다.
        compose.showDetail(permissionBrokenState())

        compose.onNodeWithText(PERMISSION_BANNER).assertIsDisplayed()
    }

    @Test
    fun `권한 끊김 안내를 누르면 재연결 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        compose.showDetail(permissionBrokenState(), onIntent = { intents += it })

        compose.onNodeWithText(PERMISSION_BANNER).clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.OpenPermissionRepair), intents)
    }

    @Test
    fun `권한 현황을 아직 모르면 끊겼다고 단정하지 않는다`() {
        // 조회 실패(null)를 거부로 접으면 "권한을 못 물어본 것"이 곧 "권한이 없다"가 되어
        // 멀쩡한 사용자에게 매번 경고를 띄운다.
        compose.showDetail(permissionBrokenState().copy(permissions = null))

        compose.onNodeWithText(PERMISSION_BANNER).assertDoesNotExist()
    }

    /** 서버가 위치 권한을 요구하는데 기기에서 꺼져 있는 방. 권한 배너의 전제다. */
    private fun permissionBrokenState() =
        state(
            detail = detail(myRole = MemberRole.MEMBER, requiredPermissions = listOf("LOCATION")),
            room = room(),
            permissions = permissionsAllGranted().copy(location = PermissionState.DENIED),
        )

    private companion object {
        const val PERMISSION_BANNER = "인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기"
    }
}
