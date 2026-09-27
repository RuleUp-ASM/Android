package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.verification.domain.entity.FailureReason
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 상세 화면이 상태를 어떻게 그리고 조작을 어떤 의도로 올리는지 고정한다.
 *
 * 이 화면은 상태 객체 하나로 서로 다른 네 화면(불러오는 중·실패·공개 상세·방 상세)을 그리고, 그 위에
 * 권한 배너·판정 모달·하단 CTA 를 조건부로 얹는다. 조건이 하나 어긋나도 컴파일은 되고 아래 층 테스트도
 * 전부 초록이다 — "이미 참여한 방에 참여하기 버튼이 다시 뜬다" 같은 회귀는 여기서만 잡힌다.
 *
 * 문구를 만들어내는 계산(실패 사유·이의 마감일·연속 기록)은 케이스 층이 이미 본다
 * (`VerificationResultModalTest`·`AppealSheetTest`). 여기서는 **무엇이 보이고 무엇이 눌리는가**만 본다.
 *
 * 다루지 않는 것: 가입 차단 시트·멤버 확인 다이얼로그·권한 바텀시트. 앞 둘은 시트/다이얼로그를 여는
 * 조작이 방 안 섹션 깊숙이 있어 이 층에서 열기 어렵고, 셋 다 사유별 문구 대응표라 `when` 을 순수 함수로
 * 꺼내 케이스 층에서 보는 편이 싸다. `TEST_STRATEGY.md` 미검증 목록에 올려 뒀다.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * 디자인 시스템의 클릭은 `SingleClickGuard`(전역 300ms)를 거치는데, 그 시각은 JVM 전역이고
     * Robolectric 은 테스트마다 `SystemClock` 을 초기값으로 되돌린다. 그대로 두면 **첫 클릭부터**
     * 조용히 삼켜지고, 단언은 "버튼이 안 눌렸다"가 아니라 "의도가 안 올라왔다"로만 깨진다.
     * 테스트마다 시계를 앞선 테스트보다 확실히 앞으로 밀어 가드를 통과시킨다.
     */
    @Before
    fun `앞선 테스트보다 시계를 앞으로 밀어 전역 클릭 가드를 통과시킨다`() {
        clockOffsetMillis += TEST_CLOCK_STEP_MILLIS
        ShadowSystemClock.advanceBy(Duration.ofMillis(clockOffsetMillis))
    }

    @Test
    fun `불러오는 중에는 상세도 실패 문구도 보여주지 않는다`() {
        // 받아오기 전 화면에 이전 값이나 실패 문구가 남으면 사용자는 없는 사실을 읽는다.
        setContent(state(isLoading = true))

        compose.onNodeWithText("참여 형태").assertDoesNotExist()
        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `상세를 받지 못하면 서버가 준 문구를 그대로 보여준다`() {
        setContent(state(detail = null, errorMessage = "찾을 수 없는 챌린지예요."))

        compose.onNodeWithText("찾을 수 없는 챌린지예요.").assertIsDisplayed()
        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `서버가 이유를 주지 않아도 빈 화면으로 두지 않는다`() {
        // 실패했는데 아무것도 없으면 사용자는 앱이 멈춘 줄 안다.
        setContent(state(detail = null, errorMessage = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `참여 전에는 탭 없이 공개 상세만 보여준다`() {
        setContent(state(detail = detail(), room = null))

        compose.onNodeWithText("챌린지").assertIsDisplayed()
        compose.onNodeWithText("아침 6시 기상").assertIsDisplayed()
        compose.onNodeWithText("참여 형태").assertExists()
        compose.onNodeWithText("정보").assertDoesNotExist()
    }

    @Test
    fun `참여 중인 방이면 세 탭으로 열고 공개 상세는 보여주지 않는다`() {
        setContent(state(detail = detail(myRole = MemberRole.MEMBER), room = room()))

        compose.onNodeWithText("정보").assertIsDisplayed()
        compose.onNodeWithText("피드").assertIsDisplayed()
        compose.onNodeWithText("랭킹").assertIsDisplayed()
        // 정보 탭 헤더(남은 기간·내 달성률)는 정보 탭에서만 편다.
        compose.onNodeWithText("종료까지").assertIsDisplayed()
        compose.onNodeWithText("참여 형태").assertDoesNotExist()
    }

    @Test
    fun `정보 탭이 아니면 D-day 헤더를 펴지 않는다`() {
        // 피드·랭킹은 목록이 화면을 채워야 한다. 헤더가 남으면 목록이 그만큼 잘린다.
        setContent(
            state(
                detail = detail(myRole = MemberRole.MEMBER),
                room = room(),
                selectedTab = RoomTab.FEED,
            ),
        )

        compose.onNodeWithText("종료까지").assertDoesNotExist()
    }

    @Test
    fun `탭을 누르면 탭 전환 의도가 올라간다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        setContent(
            state = state(detail = detail(myRole = MemberRole.MEMBER), room = room()),
            onIntent = { intents += it },
        )

        compose.onNodeWithText("피드").performClick()

        assertEquals(listOf(ChallengeDetailIntent.SelectTab(RoomTab.FEED)), intents)
    }

    @Test
    fun `방장이 아니면 챌린지 수정 진입점을 만들지 않는다`() {
        // 눌러도 서버가 막을 메뉴를 열어 두면 사용자는 자기가 뭘 잘못했는지로 읽는다.
        setContent(
            state(
                detail = detail(myRole = MemberRole.MEMBER),
                room = room(myRole = MemberRole.MEMBER),
            ),
        )

        compose.onNodeWithContentDescription("더 보기").assertDoesNotExist()
    }

    @Test
    fun `방장이면 더 보기에서 챌린지 수정으로 갈 수 있다`() {
        val intents = mutableListOf<ChallengeDetailIntent>()
        setContent(
            state =
                state(
                    detail = detail(myRole = MemberRole.OWNER),
                    room = room(myRole = MemberRole.OWNER),
                ),
            onIntent = { intents += it },
        )

        compose.onNodeWithContentDescription("더 보기").clickPastGuard()
        compose.onNodeWithText("챌린지 수정").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.OpenSettings), intents)
    }

    @Test
    fun `참여 중에 권한이 꺼져 있으면 배너로 알리고 재연결로 보낸다`() {
        // 권한이 끊기면 인증은 조용히 멈춘다 — 사용자가 스스로 알아챌 방법이 없어 매일 실패가 쌓인다.
        val intents = mutableListOf<ChallengeDetailIntent>()
        setContent(
            state =
                state(
                    detail = detail(myRole = MemberRole.MEMBER),
                    room = room(),
                    setup = setup(requiredPermissions = listOf("LOCATION")),
                    permissions = permissions(location = PermissionState.DENIED),
                ),
            onIntent = { intents += it },
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertIsDisplayed()
        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").performClick()

        assertEquals(listOf(ChallengeDetailIntent.OpenPermissionRepair), intents)
    }

    @Test
    fun `권한을 아직 확인하지 못했으면 배너를 띄우지 않는다`() {
        // 모른다고 경고부터 띄우면 조회 실패가 곧 "권한이 꺼졌다"는 거짓 사실이 된다.
        setContent(
            state(
                detail = detail(myRole = MemberRole.MEMBER),
                room = room(),
                setup = setup(requiredPermissions = listOf("LOCATION")),
                permissions = null,
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `아직 확인하지 않은 판정이 있으면 모달로 올린다`() {
        // 밤새 확정된 결과를 사용자가 알 수 있는 유일한 자리다.
        val intents = mutableListOf<ChallengeDetailIntent>()
        setContent(
            state = state(todayResult = failedTodayResult()),
            onIntent = { intents += it },
        )

        compose.onNodeWithText("오늘 인증을 놓쳤어요").assertIsDisplayed()
        compose.onNodeWithText("확인").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.AcknowledgeResult), intents)
    }

    @Test
    fun `불러오는 중에는 판정 모달을 스켈레톤 위에 올리지 않는다`() {
        // 뒤에 뭐가 있는지 모른 채 결과부터 마주치게 된다(프론트엔드 테크스펙 4-1 「모달 순서」).
        setContent(state(isLoading = true, todayResult = failedTodayResult()))

        compose.onNodeWithText("오늘 인증을 놓쳤어요").assertDoesNotExist()
    }

    @Test
    fun `이미 확인한 판정은 다시 올리지 않는다`() {
        // ack 가 실패해도 서버는 같은 판정을 계속 내려준다. 화면이 기억하지 않으면 모달이 반복된다.
        setContent(state(todayResult = failedTodayResult(), resultAcknowledged = true))

        compose.onNodeWithText("오늘 인증을 놓쳤어요").assertDoesNotExist()
    }

    @Test
    fun `이미 참여한 사람에게는 참여 버튼을 다시 보여주지 않는다`() {
        // 참여 여부는 myRole 하나로만 판단한다. 방 홈 조회 성공 여부로 판단하면 솔로 방·시작 전 그룹 방에서
        // 멤버인데도 "참여하기" 가 다시 뜬다.
        setContent(state(detail = detail(myRole = MemberRole.MEMBER), room = null))

        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `초대 전용 방에서는 참여 버튼 대신 이유를 보여준다`() {
        // 눌러봐야 막히는 버튼을 두면 사용자가 막힌 원인을 자기 자격 문제로 오해한다.
        setContent(state(detail = detail(joinBlockReason = JoinBlockReason.PRIVATE_INVITE_ONLY)))

        compose.onNodeWithText("초대 링크로만 들어올 수 있는 챌린지예요").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `참여 버튼을 누르면 화면이 정한 다음 단계로 넘어간다`() {
        // 다음 단계(권한·앱 등록·앵커·가입)는 버튼 문구와 함께 호출부가 정한다 — 여기서는 눌리는지만 본다.
        var proceeded = false
        setContent(
            state = state(detail = detail()),
            ctaLabel = "권한 허용하기",
            onCta = { proceeded = true },
        )

        compose.onNodeWithText("권한 허용하기").clickPastGuard()

        assertTrue(proceeded)
    }

    @Test
    fun `참여 요청 중에는 버튼을 잠근다`() {
        // 정원 경합은 서버가 막지만, 두 번 눌린 사이에 화면이 두 번 넘어가는 건 막지 못한다.
        setContent(state(detail = detail(), isJoining = true))

        compose.onNodeWithText("참여하는 중…").assertIsNotEnabled()
    }

    @Test
    fun `복제할 수 없는 방에는 템플릿 버튼을 만들지 않는다`() {
        // 복제는 공개 그룹만 된다. 눌러보게 두지 않고 사전에 없앤다.
        setContent(state(detail = detail(cloneable = false)))

        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `초안을 만드는 중에는 복제 버튼을 다시 누를 수 없다`() {
        setContent(state(detail = detail(cloneable = true), isCloning = true))

        compose.onNodeWithText("초안을 만드는 중…").assertIsNotEnabled()
    }

    // ---- 화면 띄우기 ----

    private fun setContent(
        state: ChallengeDetailState,
        ctaLabel: String = "참여하기",
        onIntent: (ChallengeDetailIntent) -> Unit = {},
        onCta: () -> Unit = {},
    ) {
        compose.setContent {
            RuleUpTheme {
                ChallengeDetailContent(
                    state = state,
                    ctaLabel = ctaLabel,
                    onIntent = onIntent,
                    onBack = {},
                    onCta = onCta,
                )
            }
        }
    }

    /** 한 테스트에서 두 번 누를 때 전역 클릭 가드에 두 번째가 삼켜지지 않도록 시계를 밀고 누른다. */
    private fun SemanticsNodeInteraction.clickPastGuard() {
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        performClick()
    }

    // ---- 고정값 ----

    private fun state(
        isLoading: Boolean = false,
        detail: ChallengeDetail? = detail(),
        errorMessage: String? = null,
        room: ChallengeRoom? = null,
        setup: ChallengeSetupInfo? = null,
        permissions: PermissionSnapshot? = null,
        todayResult: TodayResult? = null,
        resultAcknowledged: Boolean = false,
        selectedTab: RoomTab = RoomTab.INFO,
        isJoining: Boolean = false,
        isCloning: Boolean = false,
    ) = ChallengeDetailState(
        challengeId = "c_1",
        isLoading = isLoading,
        detail = detail,
        errorMessage = errorMessage,
        setup = setup,
        room = room,
        permissions = permissions,
        todayResult = todayResult,
        resultAcknowledged = resultAcknowledged,
        selectedTab = selectedTab,
        isJoining = isJoining,
        isCloning = isCloning,
    )

    private fun detail(
        myRole: MemberRole = MemberRole.NONE,
        cloneable: Boolean = false,
        joinBlockReason: JoinBlockReason? = null,
    ) = ChallengeDetail(
        challengeId = "c_1",
        title = "아침 6시 기상",
        description = "매일 아침 6시에 일어나요",
        imageUrl = null,
        category = Category.WAKE_SLEEP,
        mode = ChallengeMode.GROUP,
        visibility = ChallengeVisibility.PUBLIC,
        status = ChallengeStatus.ACTIVE,
        owner = ChallengeOwner(userId = "u_owner", nickname = "루피"),
        ownerType = OwnerType.USER,
        participantCount = 4,
        capacity = 10,
        isFull = false,
        period = ChallengePeriod(start = "2026-08-01", end = "2026-08-31", remainingDays = 12),
        verification =
            VerificationConfig(
                type = VerificationType.AUTO,
                method = VerificationMethod.WAKE,
                detail = "기상 06:00 ±10분 내 10걸음",
            ),
        stats = ChallengeStats(completionRate = null, retentionRate = null),
        gate = ChallengeGate(minTier = null, myDisplayTier = Tier.BRONZE, eligible = true),
        joinBlockReason = joinBlockReason,
        rejoinAvailableAt = null,
        joinNote = JoinNote.IMMEDIATE,
        cloneable = cloneable,
        myRole = myRole,
        moderation = null,
    )

    private fun room(myRole: MemberRole = MemberRole.MEMBER) =
        ChallengeRoom(
            myRole = myRole,
            ownerType = OwnerType.USER,
            summary =
                RoomSummary(
                    title = "아침 6시 기상",
                    roomSuccessRate = 0.72,
                    remainingDays = 12,
                    participantCount = 4,
                    capacity = 10,
                ),
            topRanking = emptyList(),
            myTodayStatus = TodayVerificationStatus.IN_PROGRESS,
        )

    private fun setup(requiredPermissions: List<String>) =
        ChallengeSetupInfo(
            manual = false,
            ready = true,
            verificationMethod = VerificationMethod.WAKE,
            requiredPermissions = requiredPermissions,
            requiresAnchors = false,
            anchorsConfigured = false,
            requiresTargetPackages = false,
        )

    private fun permissions(location: PermissionState = PermissionState.GRANTED) =
        PermissionSnapshot(
            location = location,
            backgroundLocation = PermissionState.GRANTED,
            activityRecognition = PermissionState.GRANTED,
            usageStats = PermissionState.GRANTED,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )

    private fun failedTodayResult() =
        TodayResult(
            date = "2026-08-31",
            verificationId = "v_1",
            status = TodayResultStatus.FAILED,
            window = null,
            confirmedAt = null,
            failureReason = FailureReason.WOKE_UP_LATE,
            streak = null,
            unacknowledged = UnacknowledgedResult(verificationId = "v_1", result = "FAILED"),
            appeal = null,
        )

    private companion object {
        // 테스트 사이 간격. 한 테스트 안의 클릭(1초씩)이 다음 테스트의 시작 시각을 넘지 않을 만큼 크다.
        const val TEST_CLOCK_STEP_MILLIS = 60_000L

        var clockOffsetMillis = 0L
    }
}
