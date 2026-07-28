package com.ruleup.observability.domain.api

import com.ruleup.observability.domain.model.Severity

/*
 * 진단 로깅 단축 호출.
 *
 * [Observability] 의 코어 API 는 log/emit 셋뿐이고, 여기 있는 것들은 그 위의 얇은 편의 레이어다.
 * `Log.d`·`Timber.d` 관습을 그대로 쓰기 위한 것이며, 전부 inline 이라 코어를 직접 부르는 것과
 * 런타임 비용이 같다. 불필요해지면 이 파일만 지우면 된다 — 코어 API 는 영향을 받지 않는다.
 *
 * [message] 는 게이트 통과 후에만 평가된다.
 */

inline fun Observability.v(
    tag: String,
    message: () -> String,
) = log(Severity.VERBOSE, tag, message = message)

inline fun Observability.d(
    tag: String,
    message: () -> String,
) = log(Severity.DEBUG, tag, message = message)

inline fun Observability.i(
    tag: String,
    message: () -> String,
) = log(Severity.INFO, tag, message = message)

inline fun Observability.w(
    tag: String,
    cause: Throwable? = null,
    message: () -> String,
) = log(Severity.WARN, tag, cause, message)

inline fun Observability.e(
    tag: String,
    cause: Throwable? = null,
    message: () -> String,
) = log(Severity.ERROR, tag, cause, message)
