package com.ruleup.home.presentation

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryIconRes
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot

/**
 * 홈 챌린지 카드 1개의 표시 모델. 서버 진행률(ChallengeProgress)과 로컬 요약(MyChallengeSummary)을
 * 한 가지 형태로 합쳐 렌더한다.
 */
data class HomeChallengeUi(
    val challengeId: String,
    val title: String,
    val subtitle: String,
    // 0f..1f
    val progress: Float,
    val todayTarget: Boolean,
    @DrawableRes val iconRes: Int,
    val accentColor: Color,
)

/**
 * 홈 카드 소스를 병합한다.
 *
 * 서버 "내 챌린지 목록"(GET /challenges)을 권위 있는 기준으로 삼고, 진행률(verification/progress)로
 * 각 카드의 진행바·오늘 대상 여부를 채운다. 목록 조회가 실패해 비면 진행률만으로도 렌더되도록
 * 목록에 없는 진행률 카드를 뒤에 유지하고, 아직 목록·진행률 어디에도 없는(방금 만든) 로컬 챌린지는
 * 맨 앞에 올려 즉시 노출한다.
 */
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
            .map { it.toHomeUi() }

    val coveredIds = serverIds + progressOnlyCards.map { it.challengeId }.toSet()
    val localCards =
        locals
            .filter { it.challengeId !in coveredIds }
            .map { it.toHomeUi() }
    return localCards + serverCards + progressOnlyCards
}

private fun MyChallenge.toHomeUi(progress: ChallengeProgress?): HomeChallengeUi {
    val dayPart = if (progress == null || progress.successDays <= 0) "오늘 시작" else "${progress.successDays}일째"
    val groupPart = if (participationType == ParticipationType.GROUP) "함께" else "솔로"
    return HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = listOf(dayPart, groupPart).joinToString(" · "),
        progress = progress?.let { (it.progressRate / 100.0).toFloat().coerceIn(0f, 1f) } ?: 0f,
        todayTarget = progress?.todayTarget ?: false,
        iconRes = categoryIconRes(category),
        accentColor = categoryAccentColor(category),
    )
}

private fun ChallengeProgress.toHomeUi(): HomeChallengeUi {
    val dayPart = if (successDays <= 0) "오늘 시작" else "${successDays}일째"
    val groupPart =
        when (participationType) {
            ParticipationType.GROUP.value -> "함께"
            ParticipationType.SOLO.value -> "솔로"
            else -> null
        }
    return HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = listOfNotNull(dayPart, groupPart).joinToString(" · "),
        progress = (progressRate / 100.0).toFloat().coerceIn(0f, 1f),
        todayTarget = todayTarget,
        iconRes = categoryIconRes(category),
        accentColor = categoryAccentColor(category),
    )
}

private fun MyChallengeSummary.toHomeUi(): HomeChallengeUi =
    HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = "오늘 시작 · ${if (participationType == ParticipationType.GROUP) "함께" else "솔로"}",
        progress = 0f,
        todayTarget = true,
        iconRes = categoryIconRes(category),
        accentColor = categoryAccentColor(category),
    )
