package com.ruleup.onboarding.domain.observability

import com.ruleup.observability.domain.model.attributes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 팩토리 출력을 그대로 고정한다. 이벤트 이름·키·값 타입이 곧 분석 백엔드와의 계약이라, 이름 하나가
 * 바뀌면 대시보드가 조용히 비는데 컴파일은 그대로 통과한다.
 */
class OnboardingEventsTest {
    @Test
    fun `login_screen_view 는 진입 유형을 싣는다`() {
        val event = OnboardingEvents.loginScreenView(LoginEntryType.RELOGIN)

        assertEquals("login_screen_view", event.name)
        assertEquals(attributes { put("entry_type", "relogin") }, event.attrs)
    }

    @Test
    fun `login_result 성공에는 error_code 키를 아예 넣지 않는다`() {
        // 빈 문자열을 넣으면 집계에 "빈 값"이라는 가짜 분류가 하나 생긴다.
        val event = OnboardingEvents.loginResult(provider = "kakao", success = true, isNewUser = true)

        assertEquals(
            attributes {
                put("provider", "kakao")
                put("success", true)
                put("is_new_user", true)
            },
            event.attrs,
        )
    }

    @Test
    fun `login_result 실패는 에러 코드를 싣는다`() {
        val event = OnboardingEvents.loginResult(provider = "google", success = false, errorCode = "LOGIN_FAILED")

        assertEquals(
            attributes {
                put("provider", "google")
                put("success", false)
                put("error_code", "LOGIN_FAILED")
            },
            event.attrs,
        )
    }

    @Test
    fun `단계 이벤트의 step_index 는 화면 표기와 같은 1부터다`() {
        // n/6 표기와 어긋나면 집계에서 단계가 한 칸씩 밀린다.
        assertEquals(1, OnboardingStep.NICKNAME.index)
        assertEquals(6, OnboardingStep.TERMS.index)

        val event = OnboardingEvents.stepView(OnboardingStep.BIRTH)
        assertEquals("onboarding_step_view", event.name)
        assertEquals(
            attributes {
                put("step", "birth")
                put("step_index", 3L)
            },
            event.attrs,
        )
    }

    @Test
    fun `signup_complete 는 소요 시간이 없으면 키를 생략한다`() {
        val event =
            OnboardingEvents.signupComplete(
                interestCount = 2,
                hasGender = true,
                optionalAgreements = 1,
                durationMs = null,
            )

        assertEquals("signup_complete", event.name)
        assertEquals(
            attributes {
                put("interest_count", 2L)
                put("has_gender", true)
                put("optional_agreements", 1L)
            },
            event.attrs,
        )
    }

    @Test
    fun `모든 이벤트 이름이 스펙과 일치한다`() {
        assertEquals("login_attempt", OnboardingEvents.loginAttempt("kakao").name)
        assertEquals("onboarding_step_complete", OnboardingEvents.stepComplete(OnboardingStep.PHOTO, true).name)
        assertEquals("nickname_check", OnboardingEvents.nicknameCheck(valid = true, available = true).name)
        assertEquals("signup_failed", OnboardingEvents.signupFailed("BIRTHDATE_UNDERAGE").name)
        assertEquals("profile_image_upload_result", OnboardingEvents.profileImageUploadResult(true).name)
        assertEquals("session_expired", OnboardingEvents.sessionExpired(SessionExpiredTrigger.OTHER_DEVICE).name)
    }
}
