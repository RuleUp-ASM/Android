package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.JoinBlock
import com.ruleup.domain.entity.user.Tier
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import com.ruleup.verification.domain.entity.VerificationStreak
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 챌린지 상세 화면의 **상태별 렌더**. 어떤 상태가 오면 무엇이 보이고 무엇이 사라지는지,
 * 눌렀을 때 어떤 인텐트가 올라가는지를 본다.
 *
 * 문구·계산은 이미 순수 함수 테스트(`TodayVerificationCopyTest` 등)가 지키므로 여기서는 다시 세지 않고,
 * **분기 선택**만 확인한다 — 같은 문장을 두 층에서 세면 문구를 고칠 때마다 두 곳이 깨진다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 동안에는 스피너만 두고 실패 문구를 미리 띄우지 않는다`() {
        compose.showDetail(ChallengeDetailState.initial.copy(challengeId = CHALLENGE_ID))

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 서버가 준 문구를 그대로 보여준다`() {
        compose.showDetail(
            loadedState(detail = null).copy(errorMessage = "찾을 수 없는 챌린지예요."),
        )

        compose.onNodeWithText("찾을 수 없는 챌린지예요.").assertIsDisplayed()
    }

    @Test
    fun `실패 문구가 없어도 빈 화면을 남기지 않는다`() {
        // errorMessage 가 null 인 실패 경로가 실제로 있다(취소·타임아웃). 그때 아무 말도 없으면
        // 사용자는 로딩이 끝난 것인지조차 알 수 없다.
        compose.showDetail(loadedState(detail = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `공개 상세는 방장과 인원 기간 인증 방식을 편다`() {
        compose.showDetail(loadedState())

        compose.onNodeWithText("아침 6:30 기상").assertIsDisplayed()
        compose.onNodeWithText("홍길동 · 12명 참여 중").assertExists()
        compose.onNodeWithText("매일 아침 같이 일어나요").assertExists()
        compose.onNodeWithText("2026-08-01 ~ 2026-08-28").assertExists()
        compose.onNodeWithText("12 / 30명").assertExists()
        compose.onNodeWithText("그룹").assertExists()
        compose.onNodeWithText("기상 06:00 ±10분 내 10걸음").assertExists()
    }

    @Test
    fun `방장이 없으면 방장 없음이라고 적는다`() {
        // 봇방장 방은 owner 가 null 이다. 자리를 비워 두면 "· 12명 참여 중" 만 남아 문장이 잘린다.
        compose.showDetail(loadedState(detail = detail(owner = null)))

        compose.onNodeWithText("방장 없음 · 12명 참여 중").assertExists()
    }

    @Test
    fun `이미 멤버면 하단 참여 영역을 아예 만들지 않는다`() {
        // 참여 여부 판단은 myRole 하나뿐이다 — room 조회 성공 여부로 판단하면 솔로 방에서 다시 뜬다(#94 계열).
        compose.showDetail(loadedState(detail = detail(myRole = MemberRole.MEMBER)), ctaLabel = "참여하기")

        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `CTA 라벨은 넘어온 그대로 그리고 탭은 그대로 올라간다`() {
        var ctaTapped = 0
        compose.showDetail(loadedState(), ctaLabel = "권한 허용하기", onCta = { ctaTapped++ })

        compose.clickText("권한 허용하기")

        assertEquals(1, ctaTapped)
    }

    @Test
    fun `참여 요청 중에는 문구를 바꾸고 두 번째 탭을 막는다`() {
        var ctaTapped = 0
        compose.showDetail(loadedState().copy(isJoining = true), onCta = { ctaTapped++ })

        compose.onNodeWithText("참여하기").assertDoesNotExist()
        compose.clickText("참여하는 중…")

        assertEquals(0, ctaTapped)
    }

    @Test
    fun `비공개 방은 참여 버튼 대신 입장 경로를 알린다`() {
        // 눌러 봐야 막히는 버튼을 두면 사용자가 원인을 오해한다.
        compose.showDetail(
            loadedState(detail = detail(joinBlockReason = JoinBlockReason.PRIVATE_INVITE_ONLY)),
        )

        compose.onNodeWithText("초대 링크로만 들어올 수 있는 챌린지예요").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `복제할 수 없는 방에는 템플릿 버튼을 두지 않는다`() {
        compose.showDetail(loadedState(detail = detail(cloneable = false)))

        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `복제 초안을 만드는 동안에는 다시 눌리지 않는다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(detail = detail(cloneable = true)).copy(isCloning = true),
            onIntent = intents::record,
        )

        compose.clickText("초안을 만드는 중…")

        assertTrue(intents.all.isEmpty())
    }

    @Test
    fun `템플릿 버튼은 복제 인텐트를 올린다`() {
        val intents = RecordedIntents()
        compose.showDetail(loadedState(detail = detail(cloneable = true)), onIntent = intents::record)

        compose.clickText("이 템플릿으로 만들기")

        assertEquals(ChallengeDetailIntent.CloneChallenge, intents.last)
    }

    @Test
    fun `감시자 조회가 안 된 사용자에게는 감시자 섹션이 없다`() {
        // 감시자는 챌린지 x 참여자 단위다. 비참여자에게 보이면 없는 권한을 있는 것처럼 만든다.
        compose.showDetail(loadedState())

        compose.onNodeWithText("감시자").assertDoesNotExist()
    }

    @Test
    fun `내 감시자 목록은 초대와 해제 인텐트를 올린다`() {
        val intents = RecordedIntents()
        compose.showDetail(loadedState().copy(watchers = watchers()), onIntent = intents::record)

        compose.onNodeWithText("엄마").assertExists()
        compose.onNodeWithText("카카오톡으로 감시자 초대하기").performScrollTo().performGuardedClick()
        compose.onNodeWithText("해제").performScrollTo().performGuardedClick()

        assertEquals(
            listOf(ChallengeDetailIntent.InviteWatcher, ChallengeDetailIntent.RemoveWatcher("w-1")),
            intents.all,
        )
    }

    @Test
    fun `방에서 권한이 끊기면 배너로 알리고 복구 화면으로 보낸다`() {
        // 인증은 조용히 멈춘다 — 배너가 없으면 사용자는 매일 실패가 쌓이다 강퇴로 간다.
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room()).copy(
                setup = setup(requiredPermissions = listOf("PACKAGE_USAGE_STATS")),
                permissions = permissions(usageStats = PermissionState.DENIED),
            ),
            onIntent = intents::record,
        )

        compose.clickText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기")

        assertEquals(ChallengeDetailIntent.OpenPermissionRepair, intents.last)
    }

    @Test
    fun `권한 현황을 아직 못 물었으면 배너를 올리지 않는다`() {
        // 모른다고 경고하면 조회 실패가 곧 "권한이 꺼졌다"는 오보가 된다.
        compose.showDetail(
            loadedState(room = room()).copy(
                setup = setup(requiredPermissions = listOf("PACKAGE_USAGE_STATS")),
                permissions = null,
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `미확인 판정은 모달로 올리고 확인하면 ack 를 보낸다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room()).copy(
                todayResult =
                    todayResult(
                        status = TodayResultStatus.DONE,
                        streak = VerificationStreak(before = 6, after = 7),
                        unacknowledged = UnacknowledgedResult(verificationId = "v-1", result = "SUCCESS"),
                    ),
            ),
            onIntent = intents::record,
        )

        compose.onNodeWithText("오늘 인증 성공!").assertIsDisplayed()
        compose.clickText("확인")

        assertEquals(ChallengeDetailIntent.AcknowledgeResult, intents.last)
    }

    @Test
    fun `스켈레톤 위에는 판정 모달을 띄우지 않는다`() {
        // 뒤에 뭐가 있는지 모르는 채 결과부터 마주치게 된다(프론트엔드 테크스펙 4-1 모달 순서).
        compose.showDetail(
            ChallengeDetailState.initial.copy(
                challengeId = CHALLENGE_ID,
                todayResult =
                    todayResult(
                        unacknowledged = UnacknowledgedResult(verificationId = "v-1", result = "SUCCESS"),
                    ),
            ),
        )

        compose.onNodeWithText("오늘 인증 성공!").assertDoesNotExist()
    }

    @Test
    fun `이번 진입에서 닫은 판정은 다시 올라오지 않는다`() {
        // ack 가 실패해도 서버는 같은 미확인 판정을 계속 내려준다 — 화면 로컬 플래그가 유일한 방어다.
        compose.showDetail(
            loadedState(room = room()).copy(
                todayResult =
                    todayResult(
                        unacknowledged = UnacknowledgedResult(verificationId = "v-1", result = "SUCCESS"),
                    ),
                resultAcknowledged = true,
            ),
        )

        compose.onNodeWithText("오늘 인증 성공!").assertDoesNotExist()
    }

    @Test
    fun `티어로 막히면 필요한 티어와 내 티어를 나란히 보여준다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(detail = detail(minTier = Tier.GOLD, myDisplayTier = Tier.BRONZE)).copy(
                joinBlock = JoinBlock(reason = JoinBlockReason.TIER_GATE),
            ),
            onIntent = intents::record,
        )

        compose.onNodeWithText("티어 조건을 만족하지 않아요").assertExists()
        compose.onNodeWithText("필요한 티어 GOLD · 내 티어 BRONZE").assertExists()
        compose.clickText("내 티어 보기")

        assertEquals(ChallengeDetailIntent.FollowJoinBlockAction, intents.last)
    }

    @Test
    fun `재입장 대기는 탈퇴인지 강퇴인지 말하지 않고 날짜만 알린다`() {
        compose.showDetail(
            loadedState().copy(
                joinBlock =
                    JoinBlock(
                        reason = JoinBlockReason.REJOIN_COOLDOWN,
                        rejoinAvailableAt = "2026-09-05T00:00:00+09:00",
                    ),
            ),
        )

        compose.onNodeWithText("아직 다시 들어올 수 없어요").assertExists()
        compose.onNodeWithText("2026-09-05 부터 다시 참여할 수 있어요").assertExists()
    }

    @Test
    fun `앱이 모르는 차단 사유는 일반 안내로 떨어진다`() {
        // 서버가 사유를 추가해도 빈 시트가 뜨면 안 된다.
        compose.showDetail(loadedState().copy(joinBlock = JoinBlock(reason = null)))

        compose.onNodeWithText("지금은 참여할 수 없어요").assertExists()
        compose.onNodeWithText("잠시 후 다시 시도해 주세요").assertExists()
    }

    @Test
    fun `차단 시트를 닫으면 해제 인텐트가 올라간다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState().copy(joinBlock = JoinBlock(reason = JoinBlockReason.FULL)),
            onIntent = intents::record,
        )

        compose.onNodeWithText("정원이 찼어요").assertExists()
        compose.clickText("닫기")

        assertEquals(ChallengeDetailIntent.DismissJoinBlock, intents.last)
    }
}
