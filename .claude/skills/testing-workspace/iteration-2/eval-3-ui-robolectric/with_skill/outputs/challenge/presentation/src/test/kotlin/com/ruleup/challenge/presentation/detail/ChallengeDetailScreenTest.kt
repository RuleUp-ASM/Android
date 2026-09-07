package com.ruleup.challenge.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.WatcherType
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.PermissionState
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 상세 화면이 **상태를 무엇으로 그리고, 조작을 어떤 의도로 올리는지**를 지킨다.
 *
 * 여기서 보는 건 분기뿐이다 — 문구를 만들어내는 계산(오늘 인증 카피 등)은 케이스 층이 이미 잡고,
 * 화면 이동은 ViewModel 층이 잡는다. 이 화면은 상태를 받아 그리고 의도를 올릴 뿐이다.
 *
 * 대상은 바깥 [ChallengeDetailScreen] 이 아니라 [ChallengeDetailContent] 다. 바깥쪽은
 * `hiltViewModel()`·런타임 권한 런처를 직접 꺼내므로 상태를 넣어 렌더할 수 없다. 그래서 바깥쪽에서만
 * 계산되는 **CTA 라벨 매핑(권한 → 앱 등록 → 앵커 → 참여)은 여기서 검증되지 않는다.**
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val intents = mutableListOf<ChallengeDetailIntent>()
    private var backPressed = 0
    private var ctaPressed = 0

    @Before
    fun advanceClockBeforeEachTest() {
        advanceClockPastPreviousTests()
    }

    @Test
    fun `불러오는 중에는 실패도 참여 버튼도 먼저 보여주지 않는다`() {
        // 로딩 위에 오류나 CTA 를 미리 그리면, 아직 아무것도 모르는 상태를 사용자가 결론으로 읽는다.
        render(detailState(isLoading = true, detail = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `상세를 불러오지 못하면 서버가 준 이유를 그대로 보여준다`() {
        render(detailState(detail = null, errorMessage = "찾을 수 없는 챌린지예요."))

        compose.onNodeWithText("찾을 수 없는 챌린지예요.").assertIsDisplayed()
        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `이유를 모르면 빈 화면 대신 실패했다는 사실을 말한다`() {
        // 이유 없는 빈 화면은 "챌린지가 없다"로 읽힌다. 실패는 실패라고 말해야 다시 시도한다.
        render(detailState(detail = null, errorMessage = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `공개 상세는 제목과 참여 인원과 참여 조건을 보여준다`() {
        render(detailState())

        compose.onNodeWithText("아침 6시 기상").assertIsDisplayed()
        compose.onNodeWithText("루틴왕 · 3명 참여 중").assertIsDisplayed()
        compose.onNodeWithText("3 / 10명").assertIsDisplayed()
        compose.onNodeWithText("기상 06:00 ±10분 내 10걸음").assertIsDisplayed()
    }

    @Test
    fun `방장이 없는 방은 방장 이름을 지어내지 않는다`() {
        // 봇방장 방은 owner 가 null 로 온다. 아무나 채워 넣으면 없는 사람이 방장으로 보인다.
        render(detailState(detail = challengeDetail(owner = null)))

        compose.onNodeWithText("방장 없음 · 3명 참여 중").assertIsDisplayed()
    }

    @Test
    fun `설명이 없으면 설명 자리를 만들지 않는다`() {
        // 심사 중·거부된 설명은 타인 화면에서 빈 값으로 온다. 빈 줄을 그리면 레이아웃이 어긋난다.
        render(detailState(detail = challengeDetail(description = "  ")))

        compose.onNodeWithText("매일 아침 6시에 일어나요").assertDoesNotExist()
    }

    @Test
    fun `참여 버튼을 누르면 지금 해야 할 셋업 단계로 넘어간다`() {
        render(detailState(), ctaLabel = "권한 허용하기")

        compose.onNodeWithText("권한 허용하기").clickPastGuard()

        assertEquals(1, ctaPressed)
    }

    @Test
    fun `이미 참여 중이면 참여 버튼을 다시 보여주지 않는다`() {
        // 참여 여부는 myRole 하나로만 판단한다. 방 홈 조회 성공 여부로 보면 솔로 방·시작 전 그룹 방에서
        // 내가 멤버인데도 "참여하기" 가 다시 뜬다(#room 회귀).
        render(detailState(detail = challengeDetail(myRole = MemberRole.MEMBER)))

        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `초대 링크로만 들어오는 방은 참여 버튼 대신 이유를 보여준다`() {
        // 눌러봐야 막히는 버튼을 두면 사용자가 원인을 자기 자격 문제로 오해한다.
        render(detailState(detail = challengeDetail(joinBlockReason = JoinBlockReason.PRIVATE_INVITE_ONLY)))

        compose.onNodeWithText("초대 링크로만 들어올 수 있는 챌린지예요").assertIsDisplayed()
        compose.onNodeWithText("참여하기").assertDoesNotExist()
    }

    @Test
    fun `참여를 요청하는 동안에는 다시 눌러도 또 요청하지 않는다`() {
        // 정원 경합에서 연타가 곧 중복 가입 요청이 된다.
        render(detailState(isJoining = true))

        compose.onNodeWithText("참여하는 중…").clickPastGuard()

        assertEquals(0, ctaPressed)
    }

    @Test
    fun `복제할 수 없는 챌린지에는 템플릿 버튼을 두지 않는다`() {
        render(detailState(detail = challengeDetail(cloneable = false)))

        compose.onNodeWithText("이 템플릿으로 만들기").assertDoesNotExist()
    }

    @Test
    fun `템플릿으로 만들기를 누르면 복제 의도가 올라간다`() {
        render(detailState(detail = challengeDetail(cloneable = true)))

        compose.onNodeWithText("이 템플릿으로 만들기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.CloneChallenge), intents)
    }

    @Test
    fun `초안을 만드는 동안에는 다시 눌러도 또 만들지 않는다`() {
        render(detailState(detail = challengeDetail(cloneable = true), isCloning = true))

        compose.onNodeWithText("초안을 만드는 중…").clickPastGuard()

        assertTrue(intents.isEmpty())
    }

    @Test
    fun `내 감시자를 조회하지 못했으면 감시자 자리를 만들지 않는다`() {
        // 감시자는 챌린지 × 참여자 단위라 비참여자에겐 403 이다. 빈 섹션을 그리면 참여자만 쓸 수 있는
        // 기능을 비참여자에게 광고하게 된다.
        var watchers by mutableStateOf<ChallengeWatchers?>(null)
        compose.setContent {
            RuleUpTheme {
                ChallengeDetailContent(
                    state = detailState(watchers = watchers),
                    ctaLabel = "참여하기",
                    onIntent = { intents += it },
                    onBack = { backPressed++ },
                    onCta = { ctaPressed++ },
                )
            }
        }

        compose.onNodeWithText("감시자").assertDoesNotExist()

        compose.runOnIdle { watchers = ChallengeWatchers(limit = 3, watchers = listOf(watcher())) }

        compose.onNodeWithText("감시자").assertIsDisplayed()
    }

    @Test
    fun `뒤로를 누르면 화면을 떠난다`() {
        render(detailState())

        compose.onNodeWithContentDescription("뒤로").clickPastGuard()

        assertEquals(1, backPressed)
    }

    @Test
    fun `참여 중인데 권한이 꺼져 있으면 인증이 멈췄다고 알린다`() {
        // 자동 인증은 조용히 멈춘다 — 배너가 없으면 사용자는 매일 실패가 쌓여 강퇴될 때까지 모른다.
        render(
            detailState(
                room = challengeRoom(),
                setup = setupInfo(requiredPermissions = listOf("LOCATION")),
                permissions = permissionSnapshot(location = PermissionState.DENIED),
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertIsDisplayed()
    }

    @Test
    fun `권한이 다 켜져 있으면 경고를 띄우지 않는다`() {
        render(
            detailState(
                room = challengeRoom(),
                setup = setupInfo(requiredPermissions = listOf("LOCATION")),
                permissions = permissionSnapshot(),
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `권한 경고를 누르면 권한을 다시 잇는 화면으로 보낸다`() {
        render(
            detailState(
                room = challengeRoom(),
                setup = setupInfo(requiredPermissions = listOf("LOCATION")),
                permissions = permissionSnapshot(location = PermissionState.DENIED),
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.OpenPermissionRepair), intents)
    }

    @Test
    fun `방 안에서 탭을 누르면 그 탭을 열라는 의도가 올라간다`() {
        // 탭 전환은 화면이 스스로 하지 않는다 — 아직 안 받아온 탭이면 그때 조회해야 하기 때문.
        render(detailState(room = challengeRoom(), selectedTab = RoomTab.INFO))

        compose.onNodeWithText("피드").clickPastGuard()

        assertEquals(listOf(ChallengeDetailIntent.SelectTab(RoomTab.FEED)), intents)
    }

    private fun render(
        state: ChallengeDetailState,
        ctaLabel: String = "참여하기",
    ) {
        compose.setContent {
            // 색 토큰은 기본값 없는 staticCompositionLocalOf 라 테마 없이 렌더하면 그 자리에서 터진다.
            RuleUpTheme {
                ChallengeDetailContent(
                    state = state,
                    ctaLabel = ctaLabel,
                    onIntent = { intents += it },
                    onBack = { backPressed++ },
                    onCta = { ctaPressed++ },
                )
            }
        }
    }

    private fun watcher(): Watcher =
        Watcher(
            watcherId = "w_1",
            type = WatcherType.USER,
            channel = WatcherChannel.IN_APP,
            status = WatcherStatus.ACTIVE,
            displayName = "감시자닉",
            contactMasked = null,
            expiresAt = null,
        )
}
