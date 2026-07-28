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

/**
 * 외부에서 값을 정해서는 안 되는 인자 이름.
 *
 * 방어의 축을 *"어느 화면이 열리는가"* 가 아니라 *"어떤 인자를 믿는가"* 로 둔다. 화면 목록은
 * 화면이 늘 때마다 자라고 빠뜨리면 **정상 딥링크가 조용히 죽지만**, 이 목록은 화면 수와 무관하고
 * 빠뜨리면 해당 인자가 사라져 화면 자신의 전제 검사에 걸린다 — 안전한 쪽으로 실패한다.
 *
 * 도메인 검증(App Links)은 이걸 대신하지 못한다. 검증이 보장하는 건 *"이 도메인이 이 앱을
 * 인정한다"* 뿐이라, **아무 웹페이지나 검증된 도메인의 URL 을 링크로 걸 수 있다.**
 *
 * - `signupToken`: 가입 플로우의 신원. 앱 내부 로그인 흐름에서만 정당하게 생긴다.
 * - `canManage`: 관리 UI 노출 스위치. 서버 판정으로 옮기기 전까지의 임시 방어다(#161 후속).
 * - `defaultRadiusM`·`dwellMinutes`·`targetPackages`: 지오펜스 설정값. 지도 화면이 정해 넘긴다.
 */
private val UNTRUSTED_ARGS =
    setOf(
        "signupToken",
        "canManage",
        "defaultRadiusM",
        "dwellMinutes",
        "targetPackages",
    )

// App Links 경로 규약.
// - /app/{path}?{args} : 앱 화면 직결(푸시 알림 등). 화면이 늘어도 매니페스트를 고치지 않도록 접두사 하나로 묶는다.
// - /inv/{code}        : 친구 초대. 화면이 아니라 "앱 실행"으로만 받는다.
// - /w/{token}         : 감시자 초대 — 매니페스트에 없다. 앱 설치 여부와 무관하게 웹 동의 페이지로 열린다.
private const val APP_SEGMENT = "app"
private const val FRIEND_INVITE_SEGMENT = "inv"

/** 친구 초대 링크(/inv/{code}) 여부. 라우팅이 아니라 "앱 실행"으로만 처리한다. */
private fun Uri.isFriendInvite(): Boolean = pathSegments?.firstOrNull() == FRIEND_INVITE_SEGMENT

/**
 * App Link 의 [Uri] 를 [NavRoute] 로 변환한다. 변환에 실패하면 null.
 *
 * - path: `/app` 접두사를 떼고 남은 segment 를 슬래시로 합쳐 등록된 PATH 형식으로 만든다
 *   (앞 슬래시 없음, 예: "challenge/notices/detail").
 * - args: query parameter 를 String 맵으로 옮기되 [UNTRUSTED_ARGS] 는 **버린다.**
 *   이 함수는 외부 인텐트 전용 경계이므로, 앱 내부 네비게이션([com.ruleup.domain.helper.NavigationHelper])은
 *   이 경로를 타지 않아 영향받지 않는다.
 */
fun Uri.toNavRoute(): NavRoute? {
    val segments = pathSegments?.takeIf { it.isNotEmpty() } ?: return null
    if (segments.first() != APP_SEGMENT) return null
    val path = segments.drop(1).joinToString("/").takeIf { it.isNotEmpty() } ?: return null
    val args =
        queryParameterNames
            .filter { it.isNotEmpty() && it !in UNTRUSTED_ARGS }
            .associateWith { (getQueryParameter(it) ?: "") }
    return NavRoute(path, args)
}

/** 시작 백스택. 딥링크 유무와 무관하게 스플래시 한 장이다 — 인증 판정이 끝나야 목적지가 정해진다. */
fun startStack(): List<NavKey> = listOf(GenericNavKey(SplashPage.PATH))

/**
 * 콜드스타트 딥링크의 **목적지**를 해석한다. 이동은 인증 판정 뒤에 일어난다.
 *
 * 여기서 백스택을 만들지 않는 이유는, 세션이 없는 채로 화면을 띄우면 그 화면이 곧바로 API 를
 * 호출해 401 을 받기 때문이다. 토큰 정리까지 이어지면 사용자는 목적지가 아니라 **로그인 화면으로
 * 튕기고 딥링크는 유실된다.**
 *
 * 해석할 수 없으면 null — 호출부는 스플래시에서 시작해 자동 로그인 결과로 홈/인트로를 분기한다.
 * **인트로로 직행시키지 않는다**: 이미 로그인한 사용자를 온보딩 첫 화면에 떨어뜨릴 이유가 없다.
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

/**
 * 앱 실행 중 들어온 새 deep-link 를 처리할 [NavRoute] 로 변환.
 * 해석할 수 없거나 미등록 path 면 null 반환 (호출부가 무시 결정).
 */
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
