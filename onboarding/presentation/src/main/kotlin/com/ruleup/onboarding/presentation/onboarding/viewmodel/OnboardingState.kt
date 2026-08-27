package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.ui.mvi.UiState
import java.time.LocalDate

/**
 * 프로필 설정 플로우의 누적 상태. 페이지별 화면이 같은 ViewModel 을 공유하므로 step 없이 입력값만
 * 쌓는다.
 *
 * @property gender null 은 "아직 안 골랐다"이며, 필수 입력이라 고르기 전에는 다음 단계로
 *   넘어가지 못한다 (회원 정책 §2).
 * @property agreements 체크된 항목만 담는다. 전송 시 6종 전체를 만들어 미체크는 `agreed=false` 로
 *   기록한다 — 선택 약관도 "동의 안 함"을 남겨야 약관 개정 시 재동의 판정이 된다.
 */
data class OnboardingState(
    val nickname: String = "",
    // 실시간 확인 결과. null 은 아직 확인 전(입력 중)이다.
    val nicknameAvailable: Boolean? = null,
    val nicknameMessage: String? = null,
    val interests: List<Category> = emptyList(),
    val profileImageUri: String? = null,
    val birthDate: LocalDate? = null,
    val birthDateInput: String = "",
    val birthDateError: String? = null,
    val gender: Gender? = null,
    val agreements: Set<AgreementType> = emptySet(),
    val isSubmitting: Boolean = false,
) : UiState {
    /** 서버 확인까지 통과해야 다음 단계로 보낸다. */
    val nicknameConfirmed: Boolean get() = nicknameAvailable == true

    val requiredAgreementsSatisfied: Boolean
        get() = AgreementType.REQUIRED.all { it in agreements }

    companion object {
        val initial = OnboardingState()

        /**
         * 생년월일 입력 자릿수(YYYYMMDD). **도메인 규칙이 아니라 입력 위젯의 형식**이다 — 검증은
         * `ValidateBirthDateUseCase` 가 연·월·일을 따로 받아서 한다.
         */
        const val BIRTH_DATE_LENGTH = 8
    }
}
