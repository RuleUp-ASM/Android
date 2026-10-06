package com.ruleup.home.presentation

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.domain.entity.category.Category
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.TodayStatus

/** 홈 챌린지 카드 1개의 표시 모델. */
data class HomeChallengeUi(
    val challengeId: String,
    val title: String,
    val subtitle: String,
    // 기간 전체 진행률 0f..1f
    val progress: Float,
    val todayTarget: Boolean,
    val category: Category?,
    // 주간 수행 횟수 1~7. 진행률에만 있는 카드는 모른다(null).
    val weeklyCount: Int?,
    // 모르면 null
    val todayStatus: TodayStatus?,
    val imageUrl: String?,
    // 시작 전·강퇴가 아니라 지금 인증할 수 있는 방인가.
    val active: Boolean,
)

/** 서버 "내 챌린지 목록"이 기준이고 진행률이 진행바·오늘 대상 여부를 채운다. */
fun mergeHomeChallenges(
    myChallenges: List<MyChallenge>,
    progress: ProgressSnapshot?,
    locals: List<MyChallengeSummary>,
): List<HomeChallengeUi> {
    val progressById = progress?.challenges.orEmpty().associateBy { it.challengeId }
    val serverCards = myChallenges.map { it.toHomeUi(progressById[it.challengeId]) }
    val serverIds = myChallenges.map { it.challengeId }.toSet()

    // 목록 조회 실패 등으로 서버 목록이 비어도 진행률 카드는 유지한다.
    val progressOnlyCards =
        progress
            ?.challenges
            .orEmpty()
            .filter { it.challengeId !in serverIds }
            // 진행률 스냅샷은 끝난 방도 싣는다
            .filterNot { ChallengeStatus.fromValue(it.status) == ChallengeStatus.COMPLETED }
            .map { it.toHomeUi() }

    val coveredIds = serverIds + progressOnlyCards.map { it.challengeId }.toSet()
    val localCards =
        locals
            .filter { it.challengeId !in coveredIds }
            .map { it.toHomeUi() }
    return localCards + serverCards + progressOnlyCards
}

private fun MyChallenge.toHomeUi(progress: ChallengeProgress?): HomeChallengeUi {
    val dayPart =
        when {
            leftType?.isKicked == true -> "강퇴됨"
            isUpcoming -> "시작 전"
            progress == null || progress.successDays <= 0 -> "진행중"
            else -> "${progress.successDays}일째"
        }
    val groupPart = if (mode.isGroup) "함께" else "솔로"
    return HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = listOf(dayPart, groupPart).joinToString(" · "),
        progress = progress?.let { (it.progressRate / 100.0).toFloat().coerceIn(0f, 1f) } ?: 0f,
        todayTarget = !isUpcoming && leftType == null && progress?.todayTarget == true,
        category = category,
        weeklyCount = weeklyCount,
        todayStatus = progress?.todayStatus,
        imageUrl = imageUrl,
        active = !isUpcoming && leftType == null,
    )
}

private fun ChallengeProgress.toHomeUi(): HomeChallengeUi {
    val dayPart =
        when (status) {
            "UPCOMING" -> "시작 전"
            "KICKED" -> "강퇴됨"
            else -> if (successDays <= 0) "진행중" else "${successDays}일째"
        }
    // 인증 모듈의 진행률 응답은 아직 구 필드명(participationType)을 문자열로 준다.
    val groupPart =
        when (participationType) {
            ChallengeMode.GROUP.value -> "함께"
            ChallengeMode.SOLO.value -> "솔로"
            else -> null
        }
    return HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = listOfNotNull(dayPart, groupPart).joinToString(" · "),
        progress = (progressRate / 100.0).toFloat().coerceIn(0f, 1f),
        todayTarget = status == "ACTIVE" && todayTarget,
        category = category,
        weeklyCount = null,
        todayStatus = todayStatus,
        imageUrl = null,
        active = status == "ACTIVE",
    )
}

private fun MyChallengeSummary.toHomeUi(): HomeChallengeUi =
    HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = "진행중 · ${if (mode.isGroup) "함께" else "솔로"}",
        progress = 0f,
        todayTarget = true,
        category = category,
        weeklyCount = null,
        todayStatus = null,
        imageUrl = null,
        active = true,
    )
