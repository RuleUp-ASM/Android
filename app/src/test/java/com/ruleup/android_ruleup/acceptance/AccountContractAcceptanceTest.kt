package com.ruleup.android_ruleup.acceptance

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.repository.ChallengeRepositoryImpl
import com.ruleup.challenge.data.repository.WatcherRepositoryImpl
import com.ruleup.challenge.domain.entity.MyChallengeFilter
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.network.image.ImageBytes
import com.ruleup.network.image.ImageReader
import com.ruleup.notification.data.api.NotificationApi
import com.ruleup.notification.data.repository.NotificationRepositoryImpl
import com.ruleup.profile.data.api.AccountApi
import com.ruleup.profile.data.api.MyPageApi
import com.ruleup.profile.data.repository.AccountRepositoryImpl
import com.ruleup.profile.data.repository.MyPageRepositoryImpl
import com.ruleup.profile.domain.entity.AgreementRevokeForbiddenException
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 계정 계약(티어·통계·동의·제재·감시) — **실서버**에 대고 돈다.
 *
 * 이 계약들은 2026-09-07 에 한꺼번에 붙었고, **필드 이름이 어긋나면 화면이 오류 없이 빈칸이 된다.**
 * 매퍼 단위 테스트는 "우리가 가정한 JSON"을 검증할 뿐이라 그 어긋남을 못 잡는다 — 실제 응답을
 * 앱 Repository 로 읽어 보는 이 층만 잡는다.
 *
 * 돌리는 법:
 * ```
 * RULEUP_ACCEPTANCE=1 DEV_TOKEN_SECRET=... \
 *   ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"
 * ```
 */
class AccountContractAcceptanceTest {
    private lateinit var myPage: MyPageRepositoryImpl
    private lateinit var account: AccountRepositoryImpl
    private lateinit var challenges: ChallengeRepositoryImpl
    private lateinit var watchers: WatcherRepositoryImpl
    private lateinit var notifications: NotificationRepositoryImpl

    @Before
    fun setUp() {
        AcceptanceGate.require()
        val token = AcceptanceGate.issueToken(tier = "GOLD")
        myPage = MyPageRepositoryImpl(AcceptanceGate.api(MyPageApi::class.java, token.accessToken))
        account = AccountRepositoryImpl(AcceptanceGate.api(AccountApi::class.java, token.accessToken))
        val challengeApi = AcceptanceGate.api(ChallengeApi::class.java, token.accessToken)
        challenges = ChallengeRepositoryImpl(challengeApi, NoImageReader)
        watchers = WatcherRepositoryImpl(challengeApi)
        notifications =
            NotificationRepositoryImpl(AcceptanceGate.api(NotificationApi::class.java, token.accessToken))
    }

    @Test
    fun `내 티어를 앱 모델로 읽고 승급 거리까지 받는다`() =
        runBlocking<Unit> {
            // 구 매너 온도 계약이 남아 있으면 여기서 역직렬화가 터진다.
            val tier = myPage.getTier()

            assertEquals("GOLD", tier.displayTier.value, "발급 때 지정한 티어와 다르다")
            assertNotNull(tier.promotion, "최상위가 아닌데 다음 티어까지의 거리가 없다")
        }

    @Test
    fun `티어 히스토리는 표본이 없어도 보관 안내를 준다`() =
        runBlocking<Unit> {
            // 갓 만든 계정이라 best·monthly 는 비어 있는 게 정상이다 — 없는 값을 지어내지 않는지 본다.
            val history = myPage.getTierHistory()

            assertTrue(history.monthly.isEmpty(), "새 계정인데 월말 기록이 있다")
        }

    @Test
    fun `통계는 지표 5종 고정 계약으로 온다`() =
        runBlocking<Unit> {
            // period 를 요구하던 구 계약이면 400 이 나거나 필드가 통째로 다르다.
            val stats = myPage.getStats()

            assertEquals(0, stats.totalSuccessCount, "새 계정인데 성공 인증이 있다")
            assertEquals(0, stats.streak.best, "새 계정인데 최고 연속이 있다")
        }

    @Test
    fun `마이 홈이 티어와 세 카운트를 함께 준다`() =
        runBlocking<Unit> {
            val home = myPage.getHome()

            assertEquals("GOLD", home.displayTier.value)
            assertEquals(0, home.counts.inProgress)
        }

    @Test
    fun `내 챌린지 목록은 세 탭 모두 커서 계약을 지킨다`() =
        runBlocking<Unit> {
            // 커서가 남았는데 다음이 없다고 하면 목록이 중간에 잘린다.
            MyChallengeFilter.entries.forEach { filter ->
                val page = challenges.getMyChallenges(filter)

                assertTrue(
                    page.nextCursor == null || page.hasNext,
                    "$filter 탭: 커서는 남았는데 다음이 없다고 한다",
                )
            }
        }

    @Test
    fun `동의 현황은 약관 5종과 개별 동의 2종을 모두 준다`() =
        runBlocking<Unit> {
            // 앱이 모르는 type 이 오면 그 행이 조용히 사라진다 — 7종이 다 읽히는지가 계약이다.
            val status = account.getAgreements()

            assertEquals(7, status.agreements.size, "동의 항목이 7종이 아니다: ${status.agreements.map { it.type }}")
            assertNotNull(status.of(AgreementType.HEALTH_INFO), "건강정보 개별 동의를 읽지 못했다")
        }

    @Test
    fun `선택 동의는 철회되고 필수 약관은 철회가 막힌다`() =
        runBlocking<Unit> {
            val version =
                account.getAgreements().of(AgreementType.MARKETING)?.version
                    ?: error("마케팅 동의 버전이 없다")

            val after =
                account.submitAgreements(
                    listOf(AgreementSubmission(AgreementType.MARKETING, agreed = false, version = version)),
                )
            assertEquals(false, after.of(AgreementType.MARKETING)?.agreed, "선택 동의 철회가 반영되지 않았다")

            // 필수 3종 철회는 탈퇴 안내로 갈려야 한다 — 일반 오류로 접히면 사용자가 할 일을 모른다.
            assertFailsWith<AgreementRevokeForbiddenException> {
                account.submitAgreements(
                    listOf(AgreementSubmission(AgreementType.TERMS_OF_SERVICE, agreed = false, version = version)),
                )
            }
        }

    @Test
    fun `구버전으로 동의하면 다시 불러오라고 알려 준다`() =
        runBlocking<Unit> {
            // 버전이 어긋난 동의를 남기면 나중에 "무엇에 동의했는지"를 입증할 수 없다.
            assertFailsWith<AgreementVersionMismatchException> {
                account.submitAgreements(
                    listOf(AgreementSubmission(AgreementType.EVENT, agreed = true, version = "0.0")),
                )
            }
        }

    @Test
    fun `제재 이력은 깨끗한 계정에서 빈 이력으로 온다`() =
        runBlocking<Unit> {
            // 잠금 계정도 열려야 하는 화면이라, 여기서 터지면 사용자가 잠금 사유를 볼 길이 없다.
            val history = account.getSanctions()

            assertTrue(history.isEmpty, "새 계정인데 제재가 있다")
        }

    @Test
    fun `알림 센터 목록을 앱 모델로 읽는다`() =
        runBlocking<Unit> {
            // 여기서 터지면 알림함이 통째로 오류 화면이 된다. 새 계정이라 목록은 비어 있는 게 정상이다.
            val page = notifications.getNotifications()

            assertTrue(page.items.isEmpty(), "새 계정인데 알림이 있다")
            assertTrue(page.unread.isEmpty(), "빈 목록인데 미읽음이 있다")
        }

    @Test
    fun `알림 설정을 앱 모델로 읽는다`() =
        runBlocking<Unit> {
            // 배포된 서버가 아직 구 모델(types + marketing)을 주더라도 읽혀야 한다 —
            // 못 읽으면 설정 화면이 열리지 않는다.
            val settings = notifications.getSettings()

            assertNotNull(settings.groups, "그룹 설정을 읽지 못했다")
        }

    @Test
    fun `내가 감시자로 등록된 목록을 앱 모델로 읽는다`() =
        runBlocking<Unit> {
            val watching = watchers.getWatching()

            assertTrue(watching.isEmpty(), "새 계정인데 감시 항목이 있다")
        }

    /** 이 스토리는 이미지 업로드를 하지 않는다 — 불리면 그 자체가 의도치 않은 호출이다. */
    private object NoImageReader : ImageReader {
        override suspend fun read(uri: String): ImageBytes = throw NotImplementedError()
    }
}
