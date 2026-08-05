package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.onboarding.domain.entity.AgreementType
import com.ruleup.onboarding.domain.entity.Gender
import com.ruleup.ui.mvi.MviIntent

sealed interface ProfileIntent : MviIntent {
    data class SetSignupToken(
        val token: String,
    ) : ProfileIntent

    data class SetNickName(
        val name: String,
    ) : ProfileIntent

    data class SetProfileIcon(
        val img: String,
    ) : ProfileIntent

    data class SetProfileInterest(
        val interestCategory: Category,
    ) : ProfileIntent

    /** 생일 입력. 숫자만 8자리(YYYYMMDD)로 누적되며 검증은 8자리가 찼을 때 돈다. */
    data class SetBirthDate(
        val digits: String,
    ) : ProfileIntent

    /** 성별 선택. 같은 값을 다시 고르면 해제된다(= 건너뛴 것과 같은 상태). */
    data class SetGender(
        val gender: Gender,
    ) : ProfileIntent

    data class ToggleAgreement(
        val type: AgreementType,
    ) : ProfileIntent

    /** 전체 동의 토글. 하나라도 빠져 있으면 모두 체크하고, 다 차 있으면 모두 해제한다. */
    data object ToggleAllAgreements : ProfileIntent

    /** 닉네임 페이지 "다음" — 형식·중복 검사를 통과하면 다음 단계로 이동한다. */
    data object CheckNickname : ProfileIntent

    /** 약관 페이지 "시작하기" — 가입을 제출하고 성공 시 홈으로 이동한다. */
    data object Submit : ProfileIntent
}
