package com.ruleup.android_ruleup.deeplink

import android.content.Intent
import android.os.Bundle
import com.ruleup.domain.navigation.NavRoute

/**
 * 앱이 스스로 만드는 진입(알림 탭 등)의 목적지를 인텐트 extra 로 싣고 꺼낸다.
 *
 * URL 을 경유하지 않는 이유는 매니페스트 `intent-filter` 를 타지 않기 위해서다. 알림은 우리 앱이
 * 만들어 우리 액티비티를 여는 것이라 외부 라우팅이 필요 없고, 필터를 두면 **검증된 도메인의 URL 을
 * 아무 웹페이지나 링크로 걸 수 있게 된다.**
 *
 * 그래서 여기 실린 값은 **외부 URL 과 달리 인자를 걸러내지 않는다** — 발신자가 우리 앱 자신이다.
 * (다른 앱이 exported 액티비티에 같은 extra 를 넣는 건 가능하지만, 악성 앱 설치가 전제라
 * 드라이브바이 웹 링크와는 문턱이 다르다.)
 */
private const val EXTRA_ROUTE_PATH = "com.ruleup.android_ruleup.ROUTE_PATH"
private const val EXTRA_ROUTE_ARGS = "com.ruleup.android_ruleup.ROUTE_ARGS"

fun Intent.putNavRoute(route: NavRoute): Intent =
    apply {
        putExtra(EXTRA_ROUTE_PATH, route.path)
        putExtra(
            EXTRA_ROUTE_ARGS,
            Bundle().apply { route.args.forEach { (k, v) -> putString(k, v) } },
        )
    }

/** 실려 온 목적지. 없으면 null. */
fun Intent.navRouteExtra(): NavRoute? {
    val path = getStringExtra(EXTRA_ROUTE_PATH)?.takeIf { it.isNotBlank() } ?: return null

    @Suppress("DEPRECATION") // getBundleExtra 는 타입 인자를 받는 오버로드가 없다.
    val bundle = getBundleExtra(EXTRA_ROUTE_ARGS)
    val args =
        bundle
            ?.keySet()
            .orEmpty()
            .mapNotNull { key -> bundle?.getString(key)?.let { key to it } }
            .toMap()
    return NavRoute(path, args)
}
