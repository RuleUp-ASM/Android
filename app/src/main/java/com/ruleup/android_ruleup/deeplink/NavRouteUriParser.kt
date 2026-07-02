package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.navigation.appRouteByPath
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.onboarding.domain.IntroPromisePage
import com.ruleup.onboarding.domain.SplashPage
import timber.log.Timber

private const val TAG = "[DeepLink]"

/**
 * 외부(App Link)로 진입을 허용하는 조회성 화면 화이트리스트.
 *
 * MainActivity 는 exported + BROWSABLE 이라 임의 앱/웹이 ruleup://app/... 로 진입시킬 수 있다.
 * 셋업·프로필 등 상태를 바꾸거나 signupToken 을 받는 화면에 임의 인자로 진입하지 못하도록,
 * 외부에서 도달 가능한 path 를 명시적으로 제한한다. (앱 내부 네비게이션은 NavigationHelper 를 쓰므로
 * 이 경로를 타지 않아 영향받지 않는다.)
 */
private val EXTERNAL_ALLOWED_PATHS =
    setOf(
        AppRoutes.HOME,
        AppRoutes.CHALLENGE_DETAIL,
        AppRoutes.VERIFICATION_DETAIL,
        AppRoutes.VERIFICATION_PROGRESS,
    )

/**
 * App Link 의 [Uri] 를 [NavRoute] 로 변환한다.
 * - path: pathSegments 를 슬래시로 합쳐 등록된 PATH 와 동일한 형식으로 만든다 (앞 슬래시 없음, 예: "profile/icon").
 * - args: 모든 query parameter 를 그대로 String 맵으로 옮긴다 (복합 타입은 호출부의 Args.from 이 디코딩).
 */
fun Uri.toNavRoute(): NavRoute {
    val segments = pathSegments?.takeIf { it.isNotEmpty() } ?: return NavRoute(IntroPromisePage.PATH)
    val path = segments.joinToString("/")
    val args =
        queryParameterNames
            .filter { it.isNotEmpty() }
            .associateWith { (getQueryParameter(it) ?: "") }
    return NavRoute(path, args)
}

/**
 * App Link 진입 시 시작 백스택을 구성한다.
 * - URI 가 없거나 미등록/미허용 path 면 Intro 단일 스택으로 fallback.
 * - 허용된 path 면 해당 [com.ruleup.android_ruleup.navigation.AppRoute] 의 syntheticStack 을 사용한다.
 */
fun resolveStartStack(uri: Uri?): List<NavKey> {
    // 일반 실행(딥링크 없음)은 스플래시에서 시작해 자동 로그인 여부로 홈/인트로를 분기한다.
    if (uri == null) return listOf(GenericNavKey(SplashPage.PATH))
    val route = uri.toNavRoute()
    if (route.path !in EXTERNAL_ALLOWED_PATHS || appRouteByPath[route.path] == null) {
        // URI 전체(쿼리 포함)는 남기지 않고 path 만 남긴다(민감 인자 로깅 방지).
        Timber.tag(TAG).w("허용되지 않은 딥링크 진입 차단: path=%s", route.path)
        return listOf(GenericNavKey(IntroPromisePage.PATH))
    }
    return appRouteByPath.getValue(route.path).syntheticStack(route.args)
}

/**
 * 앱 실행 중 들어온 새 deep-link 를 처리할 [NavRoute] 로 변환.
 * 미등록/미허용 path 면 null 반환 (호출부가 무시 결정).
 */
fun resolveNewIntentRoute(uri: Uri): NavRoute? {
    val route = uri.toNavRoute()
    if (route.path !in EXTERNAL_ALLOWED_PATHS || appRouteByPath[route.path] == null) {
        Timber.tag(TAG).w("허용되지 않은 딥링크 무시: path=%s", route.path)
        return null
    }
    return route
}
