package com.ruleup.onboarding.presentation.common

import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure

/** 실패를 어떤 무게로 보여줄지. */
sealed interface AuthFailureUi {
    /** 짧은 안내. */
    data class Toast(
        val message: String,
    ) : AuthFailureUi

    /** 확인이 필요한 안내. */
    data class Dialog(
        val message: String,
        val restartFromLogin: Boolean = false,
    ) : AuthFailureUi

    /** 진행 불가. */
    data class Blocking(
        val message: String,
        val contactSupport: Boolean = false,
    ) : AuthFailureUi
}

/** 에러 전수를 UI 로 옮긴다. */
fun Throwable.toAuthFailureUi(): AuthFailureUi {
    val failure = (this as? AuthException)?.failure ?: AuthFailure.UNKNOWN
    return when (failure) {
        AuthFailure.LOGIN_FAILED,
        AuthFailure.INVALID_REDIRECT_URI,
        AuthFailure.INVALID_DEVICE_INFO,
        AuthFailure.UNKNOWN,
        -> AuthFailureUi.Toast("로그인에 실패했어요. 다시 시도해주세요")

        AuthFailure.PROVIDER_UNAVAILABLE ->
            AuthFailureUi.Dialog("지금은 연결이 어려워요. 다른 계정으로 로그인해보세요")

        AuthFailure.ACCOUNT_BANNED ->
            AuthFailureUi.Blocking("이용이 제한된 계정이에요", contactSupport = true)

        AuthFailure.INSTALLATION_ALREADY_REGISTERED ->
            AuthFailureUi.Dialog("이 기기에 이미 계정이 있어요. 기존 계정으로 로그인해주세요")

        AuthFailure.INVALID_SIGNUP_TOKEN ->
            AuthFailureUi.Dialog("시간이 초과됐어요. 처음부터 다시 해주세요", restartFromLogin = true)

        AuthFailure.NICKNAME_FORMAT_INVALID -> AuthFailureUi.Toast("사용할 수 없는 닉네임이에요")
        AuthFailure.NICKNAME_DUPLICATED -> AuthFailureUi.Toast("이미 사용 중인 닉네임이에요")
        AuthFailure.NICKNAME_RECENTLY_RELEASED -> AuthFailureUi.Toast("최근에 해제된 닉네임이라 잠시 쓸 수 없어요")

        AuthFailure.BIRTHDATE_INVALID -> AuthFailureUi.Toast("생년월일을 다시 확인해주세요")

        // 법적으로 가입이 불가하다.
        AuthFailure.BIRTHDATE_UNDERAGE -> AuthFailureUi.Blocking("만 14세 미만은 가입할 수 없어요")

        AuthFailure.GENDER_REQUIRED -> AuthFailureUi.Toast("성별을 다시 선택해주세요")
        AuthFailure.INTEREST_LIMIT_EXCEEDED ->
            AuthFailureUi.Toast("관심 분야는 최대 ${InterestLimits.MAX}개까지 고를 수 있어요")
        AuthFailure.REQUIRED_AGREEMENT_MISSING -> AuthFailureUi.Toast("필수 약관에 동의해주세요")

        AuthFailure.IMAGE_TOO_LARGE -> AuthFailureUi.Toast("사진 용량이 너무 커요")
        AuthFailure.IMAGE_INVALID_TYPE -> AuthFailureUi.Toast("지원하지 않는 사진 형식이에요")
        AuthFailure.IMAGE_CORRUPTED -> AuthFailureUi.Toast("사진을 읽지 못했어요")

        AuthFailure.SESSION_EXPIRED ->
            AuthFailureUi.Dialog("다른 기기에서 로그인됐어요", restartFromLogin = true)

        AuthFailure.ACCOUNT_LOCKED -> AuthFailureUi.Dialog("계정이 잠겨 있어 이용할 수 없는 기능이에요")

        AuthFailure.NETWORK -> AuthFailureUi.Blocking("네트워크에 연결할 수 없어요")
    }
}
