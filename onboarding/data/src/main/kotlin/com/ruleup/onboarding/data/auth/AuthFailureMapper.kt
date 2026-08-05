package com.ruleup.onboarding.data.auth

import com.ruleup.network.dto.ApiException
import com.ruleup.onboarding.domain.entity.AuthException
import com.ruleup.onboarding.domain.entity.AuthFailure
import java.io.IOException

/**
 * 서버 에러 코드를 [AuthFailure] 로 옮긴다.
 *
 * 모르는 코드는 [AuthFailure.UNKNOWN] 이다 — 서버가 코드를 추가해도 앱이 터지지 않고 일반 안내로
 * 떨어진다.
 */
private val CODES: Map<String, AuthFailure> =
    mapOf(
        "LOGIN_FAILED" to AuthFailure.LOGIN_FAILED,
        "INVALID_REDIRECT_URI" to AuthFailure.INVALID_REDIRECT_URI,
        "INVALID_DEVICE_INFO" to AuthFailure.INVALID_DEVICE_INFO,
        "LOGIN_PROVIDER_UNAVAILABLE" to AuthFailure.PROVIDER_UNAVAILABLE,
        "ACCOUNT_BANNED" to AuthFailure.ACCOUNT_BANNED,
        "INSTALLATION_ALREADY_REGISTERED" to AuthFailure.INSTALLATION_ALREADY_REGISTERED,
        "INVALID_SIGNUP_TOKEN" to AuthFailure.INVALID_SIGNUP_TOKEN,
        "NICKNAME_FORMAT_INVALID" to AuthFailure.NICKNAME_FORMAT_INVALID,
        "NICKNAME_DUPLICATED" to AuthFailure.NICKNAME_DUPLICATED,
        "NICKNAME_RECENTLY_RELEASED" to AuthFailure.NICKNAME_RECENTLY_RELEASED,
        "BIRTHDATE_INVALID" to AuthFailure.BIRTHDATE_INVALID,
        "BIRTHDATE_UNDERAGE" to AuthFailure.BIRTHDATE_UNDERAGE,
        "GENDER_REQUIRED" to AuthFailure.GENDER_REQUIRED,
        "INTEREST_LIMIT_EXCEEDED" to AuthFailure.INTEREST_LIMIT_EXCEEDED,
        "REQUIRED_AGREEMENT_MISSING" to AuthFailure.REQUIRED_AGREEMENT_MISSING,
        "IMAGE_TOO_LARGE" to AuthFailure.IMAGE_TOO_LARGE,
        "IMAGE_INVALID_TYPE" to AuthFailure.IMAGE_INVALID_TYPE,
        "IMAGE_CORRUPTED" to AuthFailure.IMAGE_CORRUPTED,
        "SESSION_EXPIRED" to AuthFailure.SESSION_EXPIRED,
        "ACCOUNT_LOCKED" to AuthFailure.ACCOUNT_LOCKED,
    )

/**
 * 인증 계열 호출을 감싸 실패를 [AuthException] 으로 통일한다.
 *
 * 네트워크 오류([IOException])는 재시도로 풀릴 수 있어 따로 구분한다 — 서버가 준 에러와 섞으면
 * 화면이 "다시 시도"를 권할지 "로그인부터 다시"를 권할지 정할 수 없다.
 */
internal suspend fun <T> mapAuthFailure(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ApiException) {
        throw AuthException(CODES[e.code] ?: AuthFailure.UNKNOWN, e.message, e)
    } catch (e: IOException) {
        throw AuthException(AuthFailure.NETWORK, e.message, e)
    }
