package com.ruleup.domain.navigation

/**
 * 한 경로가 로그인을 요구하는지 판단한다.
 *
 * 라우트 목록은 호스트(`:app`)가 갖고 있는데 그 값이 필요한 쪽(스플래시의 딥링크 분기)은 feature
 * 다. `NavigationHelper`·`MessageHelper` 와 같은 방식으로 계약만 여기 두고 구현은 호스트가 한다.
 */
interface RouteAccessPolicy {
    /**
     * [path] 화면을 열려면 로그인이 필요한가.
     *
     * **모르는 경로는 true 다.** 딥링크는 외부에서 들어오므로, 등록되지 않은 경로를 공개로 취급하면
     * 오타 하나가 인증 우회 시도의 통로가 된다. 안전한 쪽으로 실패한다.
     */
    fun requiresLogin(path: String): Boolean
}
