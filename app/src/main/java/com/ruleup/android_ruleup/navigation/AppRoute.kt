package com.ruleup.android_ruleup.navigation

import androidx.compose.runtime.Composable

/** 앱 내 한 페이지의 호스트 측 메타데이터. */
data class AppRoute(
    val path: String,
    val isBottomTab: Boolean = false,
    /** 루트 화면 여부. */
    val isRoot: Boolean = false,
    /** 이 화면을 열려면 로그인이 필요한가. */
    val isLoginRequired: Boolean = true,
    /** deep-link 진입 시 구성할 시작 백스택. */
    val syntheticStack: (args: Map<String, String>) -> List<GenericNavKey> = { args ->
        listOf(GenericNavKey(path, args))
    },
    val render: @Composable (args: Map<String, String>) -> Unit,
)
