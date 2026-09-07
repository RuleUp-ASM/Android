package com.ruleup.observability.domain.api

import com.ruleup.observability.domain.model.Severity

/*
 * `Log.d` 관습대로 쓰기 위한 [Observability.log] 의 inline 단축 호출.
 * 전부 코어 API 위의 얇은 층이라, 이 파일만 지워도 코어는 영향받지 않는다.
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
