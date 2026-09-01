package com.ruleup.home.presentation

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.domain.entity.category.Category
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.TodayStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 홈 카드 병합. 이 화면은 **세 출처(서버 목록·서버 진행률·로컬 스토어)를 합쳐** 그리는데, 각 조회는
 * 따로 실패할 수 있다. 여기서 지키는 건 하나다 — 어느 출처가 죽어도 **내가 아는 챌린지가 홈에서
 * 사라지지 않는다.** 사라지면 사용자는 챌린지가 없어진 줄 안다.
 */
class HomeChallengeMergeTest {
    @Test
    fun `서버 목록이 비어도 진행률에 있는 챌린지는 남는다`() {
        // 목록 조회만 실패한 경우다. 진행률이 아는 챌린지까지 지우면 홈이 통째로 빈다.
        val merged = mergeHomeChallenges(emptyList(), snapshot(progress("ch1")), emptyList())

        assertEquals(listOf("ch1"), merged.map { it.challengeId })
    }

    @Test
    fun `진행률이 없어도 서버 목록의 챌린지는 남는다`() {
        val merged = mergeHomeChallenges(listOf(myChallenge("ch1")), progress = null, locals = emptyList())

        assertEquals(listOf("ch1"), merged.map { it.challengeId })
    }

    @Test
    fun `방금 만든 챌린지는 서버가 아직 몰라도 홈에 보인다`() {
        // 생성 직후 진행률 API 에 반영되기 전 구간. 여기서 빠지면 "만들었는데 없어졌다"가 된다.
        val merged = mergeHomeChallenges(emptyList(), progress = null, locals = listOf(local("ch1")))

        assertEquals(listOf("ch1"), merged.map { it.challengeId })
    }

    @Test
    fun `서버가 아는 챌린지는 로컬 기록으로 중복되지 않는다`() {
        val merged =
            mergeHomeChallenges(
                myChallenges = listOf(myChallenge("ch1")),
                progress = snapshot(progress("ch1")),
                locals = listOf(local("ch1")),
            )

        assertEquals(listOf("ch1"), merged.map { it.challengeId })
    }

    @Test
    fun `방금 만든 챌린지가 서버 목록보다 앞에 온다`() {
        // 만든 직후 그것부터 보이는 게 사용자의 기대다.
        val merged =
            mergeHomeChallenges(
                myChallenges = listOf(myChallenge("server")),
                progress = null,
                locals = listOf(local("just-made")),
            )

        assertEquals(listOf("just-made", "server"), merged.map { it.challengeId })
    }

    @Test
    fun `진행률은 0에서 1 사이로 갇힌다`() {
        // 서버가 100 을 넘겨 보내도 진행바가 넘치지 않아야 한다.
        val merged = mergeHomeChallenges(emptyList(), snapshot(progress("ch1", progressRate = 140.0)), emptyList())

        assertEquals(1f, merged.single().progress)
    }

    @Test
    fun `아직 하루도 성공하지 못했으면 며칠째라고 세지 않는다`() {
        val merged = mergeHomeChallenges(emptyList(), snapshot(progress("ch1", successDays = 0)), emptyList())

        assertTrue(merged.single().subtitle.startsWith("오늘 시작"))
    }

    @Test
    fun `진행률이 모르는 참여 형태를 보내면 지어내지 않는다`() {
        // 인증 모듈은 아직 구 필드명을 문자열로 준다 — 못 읽는 값이 "솔로"로 둔갑하면 안 된다.
        val merged = mergeHomeChallenges(emptyList(), snapshot(progress("ch1", participationType = "COUPLE")), emptyList())

        val subtitle = merged.single().subtitle
        assertTrue(!subtitle.contains("솔로") && !subtitle.contains("함께"), subtitle)
    }

    private fun myChallenge(id: String) =
        MyChallenge(
            challengeId = id,
            title = "아침 6시 기상",
            description = null,
            imageUrl = null,
            category = Category.entries.first(),
            mode = ChallengeMode.SOLO,
            status = ChallengeStatus.ACTIVE,
            participantCount = 1,
            capacity = 1,
            minTier = null,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
            myRole = MemberRole.OWNER,
        )

    private fun local(id: String) =
        MyChallengeSummary(
            challengeId = id,
            title = "방금 만든 챌린지",
            category = Category.entries.first(),
            mode = ChallengeMode.SOLO,
            durationDays = 30,
        )

    private fun progress(
        id: String,
        progressRate: Double = 40.0,
        successDays: Int = 4,
        participationType: String? = ChallengeMode.SOLO.value,
    ) = ChallengeProgress(
        challengeId = id,
        title = "아침 6시 기상",
        category = Category.entries.first(),
        participationType = participationType,
        status = ChallengeStatus.ACTIVE.value,
        progressRate = progressRate,
        successDays = successDays,
        targetDays = 20,
        remainingDays = 16,
        todayTarget = true,
        todayStatus = TodayStatus.PENDING,
        lastSyncedAt = null,
    )

    private fun snapshot(vararg items: ChallengeProgress) = ProgressSnapshot(asOf = "2026-09-01T00:00:00Z", challenges = items.toList())
}
