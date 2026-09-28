package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import com.ruleup.android_ruleup.navigation.appRouteByPath
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeSettingsPage
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.DeeplinkResolver
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.profile.domain.navigation.MyCalendarPage
import com.ruleup.support.domain.navigation.InquiryDetailPage
import javax.inject.Inject
import javax.inject.Singleton

private const val SCHEME = "ruleup"

/** 알림 딥링크(`ruleup://…`) → 앱 라우트. */
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
            val route = route(head, rest) ?: return null
            // 등록되지 않은 경로로 보내면 빈 화면이 뜬다
            return route.takeIf { appRouteByPath[it.path] != null }
        }

        private fun route(
            head: String,
            rest: List<String>,
        ): NavRoute? =
            when (head) {
                "home" -> NavRoute(AppRoutes.HOME)

                "notifications" -> NavRoute(AppRoutes.NOTIFICATIONS)

                // 권한 재허용 알림은 복수형 challenges/{id}/setup 으로 온다
                "challenge", "challenges" -> challengeRoute(rest)

                "verification" ->
                    // 폐기된 링크다.
                    null

                // FAIL_EXPECTED 알림은 `ruleup://appeal/{verificationId}` 로 온다.
                "appeal" -> NavRoute(AppRoutes.MY_APPEALS)

                "terms" -> NavRoute(AppRoutes.MY_AGREEMENTS)

                // 서버는 두 표기를 섞어 보낸다
                "mypage", "me" -> myPageRoute(rest)

                // 감시 실패 통지(`ruleup://watching/notices/{id}`).
                "watching" -> NavRoute(AppRoutes.MY_WATCHING)

                // 모더레이션 거부(MODERATION_REJECTED) 알림이 쓰는 경로다.
                "profile" -> NavRoute(AppRoutes.MY_PROFILE_EDIT).takeIf { rest.firstOrNull() == "edit" }

                else -> null
            }

        private fun challengeRoute(rest: List<String>): NavRoute? {
            val challengeId = rest.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            return when (rest.getOrNull(1)) {
                // 방장의 수정 화면.
                "edit" -> ChallengeSettingsPage(challengeId).toRoute()
                // 감시자·피드는 방 상세 안의 섹션이라 같은 곳으로 보낸다
                else -> ChallengeDetailPage(challengeId).toRoute()
            }
        }

        private fun myPageRoute(rest: List<String>): NavRoute? =
            when (rest.firstOrNull()) {
                "tier" -> NavRoute(AppRoutes.MY_TIER)
                "appeals" -> NavRoute(AppRoutes.MY_APPEALS)
                "account", "security" -> NavRoute(AppRoutes.MY_SETTINGS)
                "nickname", "profile" -> NavRoute(AppRoutes.MY_PROFILE_EDIT)

                // 자동 제재 통지(CHALLENGE_KICKED)가 여는 제재 이력이다.
                "sanctions" -> NavRoute(AppRoutes.MY_SANCTIONS)

                // 실패 예정 알림은 날짜를 달고 온다
                "calendar" -> MyCalendarPage(rest.getOrNull(1)).toRoute()

                // 문의 답변 통지.
                "inquiries" ->
                    rest
                        .getOrNull(1)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { InquiryDetailPage(it).toRoute() }
                        ?: NavRoute(AppRoutes.MY_INQUIRIES)

                // 부정행위 검출 이력 전용 화면이 아직 없다.
                "cheat-history" -> NavRoute(AppRoutes.MY_SANCTIONS)

                null -> NavRoute(AppRoutes.MY_HOME)
                else -> null
            }
    }
