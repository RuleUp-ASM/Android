package com.ruleup.android_ruleup.home

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.entity.user.InterestCategory
import com.ruleup.ui.R
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
 * 진행률 응답 + 로컬 "내 챌린지"를 병합한다.
 *
 * 진행률에 아직 없는(방금 만든) 로컬 챌린지를 맨 앞에 올려 즉시 노출하고, 같은 challengeId 가
 * 진행률에도 있으면 실제 데이터(진행률) 쪽을 사용한다.
 */
fun mergeHomeChallenges(
    progress: ProgressSnapshot?,
    locals: List<MyChallengeSummary>,
): List<HomeChallengeUi> {
    val progressCards = progress?.challenges.orEmpty().map { it.toHomeUi() }
    val progressIds = progressCards.map { it.challengeId }.toSet()
    val localCards =
        locals
            .filter { it.challengeId !in progressIds }
            .map { it.toHomeUi() }
    return localCards + progressCards
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
        iconRes = iconResFor(category),
        accentColor = accentColorFor(category),
    )
}

private fun MyChallengeSummary.toHomeUi(): HomeChallengeUi =
    HomeChallengeUi(
        challengeId = challengeId,
        title = title,
        subtitle = "오늘 시작 · ${if (participationType == ParticipationType.GROUP) "함께" else "솔로"}",
        progress = 0f,
        todayTarget = true,
        iconRes = iconResFor(category),
        accentColor = accentColorFor(category),
    )

@DrawableRes
private fun iconResFor(category: InterestCategory?): Int =
    when (category) {
        InterestCategory.EXERCISE -> R.drawable.ic_cat_exercise
        InterestCategory.READING, InterestCategory.STUDY -> R.drawable.ic_cat_reading
        InterestCategory.MEDITATION -> R.drawable.ic_cat_meditation
        InterestCategory.HEALTH -> R.drawable.ic_cat_health
        InterestCategory.WAKE_UP -> R.drawable.ic_cat_wakeup
        InterestCategory.WORK -> R.drawable.ic_cat_work
        InterestCategory.HOBBY -> R.drawable.ic_cat_hobby
        InterestCategory.COOKING -> R.drawable.ic_cat_cooking
        InterestCategory.FINANCE -> R.drawable.ic_cat_finance
        InterestCategory.ENVIRONMENT -> R.drawable.ic_cat_environment
        InterestCategory.RELATIONSHIP -> R.drawable.ic_cat_relationship
        InterestCategory.MUSIC -> R.drawable.ic_cat_music
        InterestCategory.WRITING -> R.drawable.ic_cat_writing
        InterestCategory.CODING -> R.drawable.ic_cat_coding
        null -> R.drawable.ic_cat_exercise
    }

/** 카테고리별 강조색(Figma 카드 아이콘 톤). */
private fun accentColorFor(category: InterestCategory?): Color =
    when (category) {
        InterestCategory.WAKE_UP, InterestCategory.MEDITATION -> Color(0xFF6C5CE7)
        InterestCategory.READING, InterestCategory.STUDY, InterestCategory.WRITING -> Color(0xFF22C55E)
        InterestCategory.HEALTH, InterestCategory.COOKING -> Color(0xFFF59E0B)
        InterestCategory.EXERCISE -> Color(0xFFF43F5E)
        InterestCategory.FINANCE, InterestCategory.WORK -> Color(0xFF3B82F6)
        InterestCategory.MUSIC, InterestCategory.HOBBY -> Color(0xFF06B6D4)
        else -> Color(0xFF6C5CE7)
    }
