package com.ruleup.android_ruleup.acceptance

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.repository.ChallengeRepositoryImpl
import com.ruleup.challenge.data.repository.ExploreRepositoryImpl
import com.ruleup.network.image.ImageBytes
import com.ruleup.network.image.ImageReader
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

/** 탐색 진입 스토리 */
class ExploreAcceptanceTest {
    private lateinit var challenges: ChallengeRepositoryImpl
    private lateinit var explore: ExploreRepositoryImpl

    @Before
    fun setUp() {
        AcceptanceGate.require()

        val token = AcceptanceGate.issueToken()
        val api = AcceptanceGate.api(ChallengeApi::class.java, token.accessToken)
        challenges = ChallengeRepositoryImpl(api, NoImageReader)
        explore = ExploreRepositoryImpl(api)
    }

    @Test
    fun `갓 만든 계정은 참여 중인 챌린지가 없다`() =
        runBlocking {
            // 깨지면 온보딩 직후 홈이 남의 챌린지를 보여 준다는 뜻이다.
            val mine = challenges.getMyChallenges().challenges

            assertTrue(mine.isEmpty(), "새 계정인데 챌린지가 ${mine.size}개 있다")
        }

    @Test
    fun `실시간 인기를 앱 모델로 읽을 수 있다`() =
        runBlocking {
            val snapshot = explore.getTrending()

            assertTrue(snapshot.items.all { it.challengeId.isNotBlank() }, "식별자 없는 카드가 있다")
        }

    @Test
    fun `둘러보기 첫 페이지를 앱 모델로 읽을 수 있다`() =
        runBlocking {
            val page = explore.explore()

            // 커서가 남았는데 다음이 없다고 하면 목록이 무한 요청에 빠지거나 중간에 잘린다.
            assertTrue(page.nextCursor == null || page.hasNext, "커서는 남았는데 다음이 없다고 한다")
        }

    @Test
    fun `카테고리 집계를 앱 모델로 읽을 수 있다`() =
        runBlocking {
            val categories = explore.getCategories()

            assertTrue(categories.all { it.activeGroupCount >= 0 }, "음수 방 수가 내려왔다")
        }

    /** 이미지 업로드 미사용. */
    private object NoImageReader : ImageReader {
        override suspend fun read(uri: String): ImageBytes = throw NotImplementedError()
    }
}
