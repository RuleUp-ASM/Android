package com.ruleup.ui.error

/** 화면에 표시할 오류 안내. */
fun Throwable.userFacingMessage(fallback: String): String =
    when {
        this is java.io.IOException -> "연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        javaClass.name.startsWith("com.ruleup.") && ".domain." in javaClass.name -> message?.takeIf { it.isNotBlank() } ?: fallback
        else -> fallback
    }
