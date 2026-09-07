package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import com.ruleup.android_ruleup.navigation.appRouteByPath
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeSettingsPage
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.DeeplinkResolver
import com.ruleup.domain.navigation.NavRoute
import javax.inject.Inject
import javax.inject.Singleton

private const val SCHEME = "ruleup"

/**
 * 알림 딥링크(`ruleup://…`) → 앱 라우트.
 *
 * 매핑 표의 원본은 알림 테크 스펙 8이다. 여기서는 **화면이 실재하는 것만** 옮기고 나머지는 null 로
 * 떨어뜨린다 — 없는 화면으로 보내느니 제자리에 두는 편이 낫다.
 *
 * 서버가 타입을 늘리는 것은 정상이므로 **모르는 링크가 오는 것도 정상**이다. 그래서 미지 경로를
 * 오류로 다루지 않는다.
 */
@Singleton
class RuleUpSchemeResolver
    @Inject
    constructor() : DeeplinkResolver {
        override fun resolve(deeplink: String): NavRoute? {
            val uri = runCatching { Uri.parse(deeplink) }.getOrNull() ?: return null
            if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return null

            // ruleup://challenge/{id} 의 host 가 "challenge", pathSegments 가 나머지다.
            val head = uri.host ?: return null
            val rest = uri.pathSegments.orEmpty()
            val route = route(head, rest, uri) ?: return null
            // 등록되지 않은 경로로 보내면 빈 화면이 뜬다 — 레지스트리에 있는 것만 통과시킨다.
            return route.takeIf { appRouteByPath[it.path] != null }
        }

        private fun route(
            head: String,
            rest: List<String>,
            uri: Uri,
        ): NavRoute? =
            when (head) {
                "home" -> NavRoute(AppRoutes.HOME)

                "notifications" -> NavRoute(AppRoutes.NOTIFICATIONS)

                "challenge" -> challengeRoute(rest)

                "verification" ->
                    // 인증 상세 화면이 따로 없다 — 판정 결과는 방 상세의 오늘 카드에서 본다.
                    null

                "appeal" -> NavRoute(AppRoutes.MY_APPEALS)

                "terms" -> NavRoute(AppRoutes.MY_AGREEMENTS)

                "mypage" -> myPageRoute(rest.firstOrNull() ?: uri.path?.trim('/'))

                else -> null
            }

        private fun challengeRoute(rest: List<String>): NavRoute? {
            val challengeId = rest.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            return when (rest.getOrNull(1)) {
                // 방장의 수정 화면. 권한이 없으면 화면이 스스로 막는다
                "edit" -> ChallengeSettingsPage(challengeId).toRoute()
                // 감시자·피드는 방 상세 안의 섹션이라 같은 곳으로 보낸다
                else -> ChallengeDetailPage(challengeId).toRoute()
            }
        }

        private fun myPageRoute(section: String?): NavRoute? =
            when (section) {
                "tier" -> NavRoute(AppRoutes.MY_TIER)
                "account", "security" -> NavRoute(AppRoutes.MY_SETTINGS)
                "nickname", "profile" -> NavRoute(AppRoutes.MY_PROFILE_EDIT)
                null -> NavRoute(AppRoutes.MY_HOME)
                else -> null
            }
    }
