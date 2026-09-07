package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier

/**
 * 마이 홈 카운트 (명세 `counts`). 챌린지 탭의 세그먼트 수와 같은 값이다.
 *
 * 구 `groups`(그룹 챌린지 수)는 명세에서 사라졌다 — 탭이 진행 중 / 완료 / 이탈로 갈리면서
 * 그룹·솔로 구분은 카운트가 아니라 카드가 말한다.
 */
data class MyHomeCounts(
    val inProgress: Int,
    val completed: Int,
    val left: Int,
)

/**
 * 마이 홈 일괄 조회 (명세: GET /me/home). 마이 탭 메인 렌더링용.
 *
 * 본인 화면이라 [nickname]·[profileImageUrl] 은 심사 상태와 무관하게 입력값이 온다(거부면 직전
 * 승인본). 뱃지는 [nicknameStatus] 로 그린다.
 *
 * 점수·티어는 [MyTier] 와 같은 값의 요약이다 — 여기서는 히어로에 필요한 세 필드만 오고,
 * 유예 밴드·다음 티어까지의 거리는 `GET /me/tier` 가 갖는다.
 */
data class MyHome(
    val nickname: String,
    val nicknameStatus: NicknameStatus,
    val profileImageUrl: String?,
    val tier: Tier,
    // 누적 점수 0~2,000
    val score: Int,
    // 표시·방 입장 판정에 쓰는 티어. 유예 밴드면 tier 보다 높다
    val displayTier: Tier,
    val counts: MyHomeCounts,
    val accountStatus: AccountStatus,
    val lockInfo: LockInfo?,
)
