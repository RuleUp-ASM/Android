package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.Gender
import java.time.LocalDate

/**
 * 가입 요청 입력값(POST /auth/signup).
 *
 * [localImageUri] 는 전송 본문에 들어가지 않는다. 프로필 사진은 **가입을 마친 뒤** 발급받은
 * accessToken 으로 별도 API 에 올리므로, 이 값은 그 후속 호출을 위해 폼에 얹혀 갈 뿐이다.
 *
 * @property interestCategories 상한은 [InterestLimits]. 하한은 없고, 건너뛰면 빈 리스트다.
 * @property gender 필수 입력 — 남/여 (회원 정책 §2).
 */
data class SignupForm(
    val signupToken: String,
    val nickname: String,
    val interestCategories: List<Category>,
    val birthDate: LocalDate,
    val gender: Gender,
    val agreements: AgreementConsents,
    val localImageUri: String? = null,
) {
    /**
     * 화면도 같은 상한으로 선택을 막지만 그건 UX 이지 정합성이 아니다. 이 폼은 **송신 전용**이라
     * (서버 응답이 이 타입으로 들어오지 않는다) 여기서 던지는 예외는 언제나 우리 코드의 버그다.
     */
    init {
        require(interestCategories.size <= InterestLimits.MAX) {
            "관심 분야가 상한을 넘었습니다: ${interestCategories.size}"
        }
    }
}
