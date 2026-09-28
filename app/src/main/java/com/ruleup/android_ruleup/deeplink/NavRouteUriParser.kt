package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.navigation.appRouteByPath
import com.ruleup.challenge.domain.navigation.ChallengeInvitePage
import com.ruleup.challenge.domain.navigation.WatcherAcceptPage
import com.ruleup.domain.navigation.DeeplinkResolver
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.navigation.SplashPage

private const val TAG = "[DeepLink]"

// App Links 경로 규약.
private const val APP_SEGMENT = "app"
private const val FRIEND_INVITE_SEGMENT = "inv"
private const val WATCHER_INVITE_SEGMENT = "w"
private const val RULEUP_SCHEME = "ruleup"
private const val CHALLENGE_INVITE_SEGMENT = "c"

/** 앱 화면 주소의 호스트. */
private const val APP_LINK_HOST = "android.ruleup.co.kr"

private fun Uri.isFriendInvite(): Boolean = pathSegments?.firstOrNull() == FRIEND_INVITE_SEGMENT

/** 감시자 초대 `/w/{token}` 을 수락 화면 경로로 옮긴다. */
private fun Uri.toWatcherAcceptRoute(): NavRoute? = tokenRoute(WATCHER_INVITE_SEGMENT)?.let { WatcherAcceptPage(it).toRoute() }

/** 챌린지 멤버 초대 `/c/{token}` 을 미리보기 화면 경로로 옮긴다. */
private fun Uri.toChallengeInviteRoute(): NavRoute? = tokenRoute(CHALLENGE_INVITE_SEGMENT)?.let { ChallengeInvitePage(it).toRoute() }

/** `/{segment}/{token}` 에서 토큰만 꺼낸다. */
private fun Uri.tokenRoute(segment: String): String? {
    val segments = pathSegments ?: return null
    if (segments.firstOrNull() != segment) return null
    return segments.getOrNull(1)?.takeIf { it.isNotBlank() }
}

/** App Link 의 [Uri] 를 [NavRoute] 로 변환한다. */
fun Uri.toNavRoute(): NavRoute? {
    val segments = pathSegments?.takeIf { it.isNotEmpty() } ?: return null
    if (segments.first() != APP_SEGMENT) return null
    val path = segments.drop(1).joinToString("/").takeIf { it.isNotEmpty() } ?: return null
    val args =
        queryParameterNames
            .filter { it.isNotEmpty() }
            .associateWith { (getQueryParameter(it) ?: "") }
    return NavRoute(path, args)
}

/** 앱이 자기 화면을 가리키려고 만드는 URI. */
fun NavRoute.toAppLinkUri(): Uri =
    Uri
        .Builder()
        .scheme("https")
        .authority(APP_LINK_HOST)
        .appendPath(APP_SEGMENT)
        .apply { path.split("/").forEach { appendPath(it) } }
        .apply { args.forEach { (k, v) -> appendQueryParameter(k, v) } }
        .build()

/** 커스텀 스킴(`ruleup://`) 딥링크. */
private fun schemeRoute(
    uri: Uri,
    deeplinkResolver: DeeplinkResolver?,
): NavRoute? {
    if (!uri.scheme.equals(RULEUP_SCHEME, ignoreCase = true)) return null
    return deeplinkResolver?.resolve(uri.toString())
}

/** 시작 백스택. */
fun startStack(): List<NavKey> = listOf(GenericNavKey(SplashPage.PATH))

/** 콜드스타트 딥링크의 목적지만 해석한다. */
fun resolveStartRoute(
    uri: Uri?,
    observability: Observability,
    deeplinkResolver: DeeplinkResolver? = null,
): NavRoute? {
    if (uri == null) return null
    // 알림 탭은 ruleup:// 로 온다
    schemeRoute(uri, deeplinkResolver)?.let { return it }
    // 친구 초대(/inv/{code})는 특정 화면이 아니라 앱 실행으로 받는다.
    if (uri.isFriendInvite()) return null
    // 초대 링크들은 화면이 있다
    uri.toChallengeInviteRoute()?.let { return it }
    uri.toWatcherAcceptRoute()?.let { return it }
    val route = uri.toNavRoute()
    if (route == null || appRouteByPath[route.path] == null) {
        // URI 전체(쿼리 포함)는 남기지 않고 path 만 남긴다(민감 인자 로깅 방지).
        observability.w(TAG) { "해석할 수 없는 딥링크: path=${uri.path}" }
        return null
    }
    return route
}

fun resolveNewIntentRoute(
    uri: Uri,
    observability: Observability,
    deeplinkResolver: DeeplinkResolver? = null,
): NavRoute? {
    schemeRoute(uri, deeplinkResolver)?.let { return it }
    // 앱 사용 중 들어온 친구 초대 링크는 이동할 곳이 없다(이미 가입·로그인 상태)
    if (uri.isFriendInvite()) return null
    uri.toChallengeInviteRoute()?.let { return it }
    uri.toWatcherAcceptRoute()?.let { return it }
    val route = uri.toNavRoute()
    if (route == null || appRouteByPath[route.path] == null) {
        observability.w(TAG) { "해석할 수 없는 딥링크 무시: path=${uri.path}" }
        return null
    }
    return route
}
