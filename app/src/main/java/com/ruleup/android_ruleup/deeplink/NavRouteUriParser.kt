package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import com.ruleup.android_ruleup.navigation.GenericNavKey
import com.ruleup.android_ruleup.navigation.appRouteByPath
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.navigation.SplashPage

private const val TAG = "[DeepLink]"

// App Links 경로 규약.
// - /app/{path}?{args} : 앱 화면 직결(푸시 알림 등). 화면이 늘어도 매니페스트를 고치지 않도록 접두사 하나로 묶는다.
// - /inv/{code}        : 친구 초대. 화면이 아니라 "앱 실행"으로만 받는다.
// - /w/{token}         : 감시자 초대 — 매니페스트에 없다. 앱 설치 여부와 무관하게 웹 동의 페이지로 열린다.
private const val APP_SEGMENT = "app"
private const val FRIEND_INVITE_SEGMENT = "inv"

/**
 * 앱 화면 주소의 호스트. 알림이 자기 목적지를 조립할 때 쓴다.
 *
 * 이 호스트의 `/app/...` 은 **매니페스트 intent-filter 에 등록돼 있지 않다.** 등록된 App Link 는
 * 친구 초대 `/inv` 뿐이라, 웹페이지가 이 URL 로 앱 화면을 여는 경로가 없다(#179).
 *
 * ⚠️ `/app` 필터를 추가하는 순간 그 경로가 열린다. 그때는 **외부가 정해서는 안 되는 인자**
 * (`canManage` 같은 권한 스위치, 지오펜스 설정값)를 [toNavRoute] 에서 다시 걸러내야 한다.
 * 지금 걸러내지 않는 유일한 근거가 "그 문이 닫혀 있다"이다.
 */
private const val APP_LINK_HOST = "android.ruleup.co.kr"

private fun Uri.isFriendInvite(): Boolean = pathSegments?.firstOrNull() == FRIEND_INVITE_SEGMENT

/**
 * App Link 의 [Uri] 를 [NavRoute] 로 변환한다. 변환에 실패하면 null.
 *
 * - path: `/app` 접두사를 떼고 남은 segment 를 슬래시로 합쳐 등록된 PATH 형식으로 만든다
 *   (앞 슬래시 없음, 예: "challenge/ranking").
 * - args: query parameter 를 그대로 String 맵으로 옮긴다. 인자를 걸러내지 않는 근거는
 *   [APP_LINK_HOST] 주석 참고 — `/app` 이 매니페스트에 없어 외부에서 이 경로로 들어올 수 없다.
 */
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

/**
 * 앱이 자기 화면을 가리키려고 만드는 URI. 알림의 PendingIntent 가 쓴다.
 *
 * 인텐트는 `MainActivity` 를 명시하므로 이 URI 가 매니페스트 필터를 타지 않는다 — 목적지를
 * 실어 나르는 그릇일 뿐이다. 덕분에 진입 해석이 [toNavRoute] 한 곳으로 모이면서도 웹에서 앱
 * 화면을 여는 경로는 생기지 않는다.
 */
fun NavRoute.toAppLinkUri(): Uri =
    Uri
        .Builder()
        .scheme("https")
        .authority(APP_LINK_HOST)
        .appendPath(APP_SEGMENT)
        .apply { path.split("/").forEach { appendPath(it) } }
        .apply { args.forEach { (k, v) -> appendQueryParameter(k, v) } }
        .build()

/** 시작 백스택. 딥링크 유무와 무관하게 스플래시 한 장이다 — 인증 판정이 끝나야 목적지가 정해진다. */
fun startStack(): List<NavKey> = listOf(GenericNavKey(SplashPage.PATH))

/**
 * 콜드스타트 딥링크의 **목적지**만 해석한다. 백스택을 여기서 세우지 않는 근거는 PendingDeepLink KDoc.
 *
 * 해석할 수 없으면 null — 호출부는 스플래시에서 시작한다. **인트로로 직행시키지 않는다**:
 * 이미 로그인한 사용자를 온보딩 첫 화면에 떨어뜨릴 이유가 없다.
 */
fun resolveStartRoute(
    uri: Uri?,
    observability: Observability,
): NavRoute? {
    if (uri == null) return null
    // 친구 초대(/inv/{code})는 특정 화면이 아니라 앱 실행으로 받는다. 가입 시 inviteCode 서버 전달은
    // auth 스펙(inviteCode 필드) 개정 후 후속.
    if (uri.isFriendInvite()) return null
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
): NavRoute? {
    // 앱 사용 중 들어온 친구 초대 링크는 이동할 곳이 없다(이미 가입·로그인 상태) — 무시.
    if (uri.isFriendInvite()) return null
    val route = uri.toNavRoute()
    if (route == null || appRouteByPath[route.path] == null) {
        observability.w(TAG) { "해석할 수 없는 딥링크 무시: path=${uri.path}" }
        return null
    }
    return route
}
