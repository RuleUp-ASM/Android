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

/**
 * 탐색 진입 스토리 — **실서버**에 대고 돈다.
 *
 * 아래 네 층이 전부 초록이어도 서버가 필드 이름이나 형식을 바꾸면 앱은 망가지는데, 그걸 잡는
 * 유일한 층이다. 앱이 실제로 쓰는 **Repository 계약**을 통해 두드린다 — api 를 직접 부르고
 * 테스트용 DTO 를 따로 두면 "테스트에서만 맞는" 계약을 검증하게 된다. 매퍼가 `internal` 인 것도
 * 그래서다.
 *
 * 돌리는 법:
 * ```
 * RULEUP_ACCEPTANCE=1 DEV_TOKEN_SECRET=... \
 *   ./gradlew :app:testDebugUnitTest --tests "*AcceptanceTest*"
 * ```
 */
class ExploreAcceptanceTest {
    private lateinit var challenges: ChallengeRepositoryImpl
    private lateinit var explore: ExploreRepositoryImpl

    @Before
    fun setUp() {
        AcceptanceGate.require()
        // 매 실행 새 계정을 만든다 — 계정을 공유하면 앞선 실행이 남긴 참여 이력·재입장 대기가
        // 다음 실행의 전제를 깨뜨린다.
        val token = AcceptanceGate.issueToken()
        val api = AcceptanceGate.api(ChallengeApi::class.java, token.accessToken)
        challenges = ChallengeRepositoryImpl(api, NoImageReader)
        explore = ExploreRepositoryImpl(api)
    }

    @Test
    fun `갓 만든 계정은 참여 중인 챌린지가 없다`() =
        runBlocking {
            // 깨지면 온보딩 직후 홈이 남의 챌린지를 보여 준다는 뜻이다.
            val mine = challenges.getMyChallenges()

            assertTrue(mine.isEmpty(), "새 계정인데 챌린지가 ${mine.size}개 있다")
        }

    @Test
    fun `실시간 인기를 앱 모델로 읽을 수 있다`() =
        runBlocking {
            // 목록이 비어 있어도 성공이다 — 여기서 보는 건 개수가 아니라 계약이다.
            // 서버가 필드를 바꾸면 이 줄에서 역직렬화가 터진다.
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

    /** 이 스토리는 이미지 업로드를 하지 않는다 — 불리면 그 자체가 의도치 않은 호출이다. */
    private object NoImageReader : ImageReader {
        override suspend fun read(uri: String): ImageBytes = throw NotImplementedError()
    }
}
