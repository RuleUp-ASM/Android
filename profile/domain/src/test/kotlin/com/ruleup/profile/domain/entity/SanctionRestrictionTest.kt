package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.FeatureCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 정지 계정의 상태값은 종류와 무관하게 `SUSPENDED` 하나다. */
class SanctionRestrictionTest {
    @Test
    fun `활성 계정은 제한이 없다`() {
        assertEquals(AccountRestriction.None, history(AccountStatus.ACTIVE, type = null).restriction)
    }

    @Test
    fun `기능 정지는 그 기능만 막고 앱을 잠그지 않는다`() {
        val restriction = history(AccountStatus.SUSPENDED, SanctionType.FEATURE_SUSPENSION, "REPORT").restriction

        assertEquals(AccountRestriction.Feature("REPORT"), restriction)
        assertFalse(restriction.isFullLock)
        assertTrue(restriction.blocks(FeatureCode.REPORT))
    }

    @Test
    fun `계정 잠금과 영구 정지는 앱 전체를 잠근다`() {
        assertTrue(history(AccountStatus.SUSPENDED, SanctionType.LOCK).restriction.isFullLock)
        assertTrue(history(AccountStatus.SUSPENDED, SanctionType.BAN).restriction.isFullLock)
    }

    @Test
    fun `종류를 모르는 정지는 전체 잠금으로 본다`() {
        // 통과시키면 제재가 조용히 풀린다
        assertTrue(history(AccountStatus.SUSPENDED, type = null).restriction.isFullLock)
    }

    @Test
    fun `챌린지 강퇴는 계정을 잠그지 않는다`() {
        // 강퇴는 그 방에서만 효력이 있다.
        assertEquals(
            AccountRestriction.None,
            history(AccountStatus.SUSPENDED, SanctionType.CHALLENGE_KICK).restriction,
        )
    }

    private fun history(
        status: AccountStatus,
        type: SanctionType?,
        featureCode: String? = null,
    ) = SanctionHistory(
        accountStatus = status,
        activeSanction =
            type?.let {
                ActiveSanction(
                    sanctionId = "s-1",
                    track = SanctionTrack.ADMIN,
                    type = it,
                    featureCode = featureCode,
                    reasonCode = "REPORT_CONFIRMED",
                    reasonText = null,
                    startsAt = null,
                    endsAt = null,
                    reviewRequestable = false,
                )
            },
        admin = emptyList(),
        auto = emptyList(),
    )
}
