package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 비멤버가 보는 **공개 상세**. 방에 들어가기 전 화면이라 판단 재료(누가 · 몇 명 · 어떻게 인증)와
 * 하단 CTA 가 전부다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `로딩 중에는 본문 대신 스피너만 있다`() {
        compose.showDetail(loadedState().copy(isLoading = true, detail = null))

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 서버가 준 문구를 그대로 보여준다`() {
        compose.showDetail(loadedState().copy(detail = null, errorMessage = "찾을 수 없는 챌린지예요."))

        compose.onNodeWithText("찾을 수 없는 챌린지예요.").assertIsDisplayed()
    }

    @Test
    fun `실패 문구가 없어도 빈 화면으로 두지 않는다`() {
        compose.showDetail(loadedState().copy(detail = null, errorMessage = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `공개 상세는 참여 판단에 필요한 것만 편다`() {
        compose.showDetail(loadedState())

        compose.onNodeWithText("아침 6:30 기상").assertIsDisplayed()
        compose.onNodeWithText("루피 · 12명 참여 중").assertIsDisplayed()
        compose.onNodeWithText("매일 아침 같이 일어나요").assertIsDisplayed()
        compose.onNodeWithText("12 / 30명").assertExists()
        compose.onNodeWithText("그룹").assertExists()
        // 인증 방식은 서버가 준 문장을 그대로 쓴다 — 앱이 루틴별 문구를 조립하지 않는다.
        compose.onNodeWithText("기상 06:00 ±10분 내 10걸음").assertExists()
    }

    @Test
    fun `방장이 없으면 이름 자리를 비워 두지 않는다`() {
        compose.showDetail(loadedState(detail(ownerNickname = null, participantCount = 3)))

        compose.onNodeWithText("방장 없음 · 3명 참여 중").assertIsDisplayed()
    }

    @Test
    fun `인증 방식 문장이 없으면 자동 직접 여부만이라도 적는다`() {
        compose.showDetail(loadedState(detail(verificationDetail = null)))

        compose.onNodeWithText("자동 인증").assertExists()
    }

    @Test
    fun `CTA 문구는 화면이 계산해 넘긴 값을 그대로 쓴다`() {
        // 권한·앱·앵커 중 무엇을 먼저 요구할지는 스크린이 정한다. 본문은 받은 문구를 그릴 뿐이다.
        val harness = compose.showDetail(loadedState(), ctaLabel = "권한 허용하기")

        compose.onNodeWithText("권한 허용하기").assertIsDisplayed()
        compose.clickText("권한 허용하기")

        assertEquals(1, harness.ctaClicks)
    }

    @Test
    fun `참여 요청 중에는 버튼이 잠긴다`() {
        // 정원 경합은 서버가 막지만, 두 번 눌러 두 번 나가는 것 자체가 사용자에게는 오작동이다.
        val harness = compose.showDetail(loadedState().copy(isJoining = true))

        compose.onNodeWithText("참여하는 중…").assertIsDisplayed()
        compose.clickText("참여하는 중…")

        assertEquals(0, harness.ctaClicks)
    }

    @Test
    fun `비공개 방은 참여 버튼 대신 이유를 남긴다`() {
        // 눌러봐야 막히는 버튼을 두면 사용자가 원인을 오해한다.
        compose.showDetail(loadedState(detail(joinBlockReason = JoinBlockReason.PRIVATE_INVITE_ONLY)))

        compose.onNodeWithText("초대 링크로만 들어올 수 있는 챌린지예요").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `이미 참여 중이면 하단 버튼 자체가 없다`() {
        // 방 홈(room)이 아직 없어도 myRole 하나로 판단한다 — 솔로·시작 전 그룹에서 방 홈은 null 이다.
        compose.showDetail(loadedState(detail(myRole = MemberRole.MEMBER)))

        compose.onNodeWithText("참여하기").assertDoesNotExist()
        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `복제할 수 없는 방에는 템플릿 버튼을 만들지 않는다`() {
        compose.showDetail(loadedState(detail(cloneable = false)))

        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `복제 가능한 방은 템플릿 버튼을 연다`() {
        val harness = compose.showDetail(loadedState(detail(cloneable = true)))

        compose.clickText("이 템플릿으로 만들기")

        harness.last<ChallengeDetailIntent.CloneChallenge>()
    }

    @Test
    fun `복제 중에는 진행 문구로 바뀌고 다시 눌리지 않는다`() {
        val harness = compose.showDetail(loadedState(detail(cloneable = true)).copy(isCloning = true))

        compose.onNodeWithText("초안을 만드는 중…").assertIsDisplayed()
        compose.clickText("초안을 만드는 중…")

        assertTrue(harness.intents.isEmpty())
    }

    @Test
    fun `감시자 섹션은 조회에 성공했을 때만 열린다`() {
        // 감시자는 챌린지 × 참여자 단위라, 미참여자에게는 목록 자체가 내려오지 않는다(null).
        compose.showDetail(loadedState().copy(watchers = null))

        compose.onNodeWithText("감시자").assertDoesNotExist()
    }

    @Test
    fun `감시자 섹션은 유효 인원과 한도를 함께 센다`() {
        val harness =
            compose.showDetail(
                loadedState().copy(
                    watchers =
                        watchers(
                            watcher(watcherId = "w_1", displayName = "엄마"),
                            watcher(watcherId = "w_2", displayName = "형", status = WatcherStatus.REVOKED),
                        ),
                ),
            )

        compose.onNodeWithText("감시자").performScrollTo().assertIsDisplayed()
        // 해제된 감시자는 한도를 먹지 않는다.
        compose.onNodeWithText("1/3").assertExists()
        compose.onNodeWithText("엄마").assertExists()

        compose.clickText("카카오톡으로 감시자 초대하기", scroll = true)

        harness.last<ChallengeDetailIntent.InviteWatcher>()
    }

    @Test
    fun `감시자 해제는 그 감시자를 지목해 올린다`() {
        val harness =
            compose.showDetail(
                loadedState().copy(watchers = watchers(watcher(watcherId = "w_42"))),
            )

        compose.clickText("해제", scroll = true)

        assertEquals("w_42", harness.last<ChallengeDetailIntent.RemoveWatcher>().watcherId)
    }

    @Test
    fun `초대를 만드는 동안에는 버튼이 다시 눌리지 않는다`() {
        val harness =
            compose.showDetail(
                loadedState().copy(watchers = watchers(), isInvitingWatcher = true),
            )

        compose.clickText("초대 만드는 중...", scroll = true)

        assertTrue(harness.intents.isEmpty())
    }

    @Test
    fun `뒤로는 상단바에서만 나간다`() {
        val harness = compose.showDetail(loadedState())

        compose.clickDescribed("뒤로")

        assertEquals(1, harness.backClicks)
    }
}
