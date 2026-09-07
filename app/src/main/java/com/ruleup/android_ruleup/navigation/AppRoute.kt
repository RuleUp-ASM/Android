package com.ruleup.android_ruleup.navigation

import androidx.compose.runtime.Composable

/**
 * 앱 내 한 페이지의 호스트 측 메타데이터. typed 인자는 각 feature domain 의 Page 가 갖는다.
 * 새 페이지는 [appRoutes] 에 한 줄만 더한다.
 */
data class AppRoute(
    val path: String,
    val isBottomTab: Boolean = false,
    /**
     * 루트 화면 여부. true 면 이동 시 기존 백스택을 모두 비우고 단일 키로 시작한다
     * (예: 가입 완료 → 홈, 로그아웃 → 로그인).
     */
    val isRoot: Boolean = false,
    /**
     * 이 화면을 열려면 로그인이 필요한가.
     *
     * 딥링크가 미인증 상태로 들어왔을 때 목적지를 버릴지 그대로 열지를 이 값으로 가른다.
     *
     * **기본값이 `true` 인 게 핵심이다.** 새 화면을 등록하면서 깜빡해도 로그인을 요구하는 쪽으로
     * 떨어진다 — 반대로 두면 빠뜨린 화면이 조용히 공개된다.
     */
    val isLoginRequired: Boolean = true,
    /**
     * deep-link 진입 시 구성할 시작 백스택. 일반 페이지는 자기 자신만 푸시되고,
     * 부모 페이지가 있는 경우 부모 키들을 함께 반환한다.
     */
    val syntheticStack: (args: Map<String, String>) -> List<GenericNavKey> = { args ->
        listOf(GenericNavKey(path, args))
    },
    val render: @Composable (args: Map<String, String>) -> Unit,
)
