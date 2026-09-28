package com.ruleup.profile.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 타인 프로필. */
data class MemberProfilePage(
    val userId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_USER_ID to userId))

    companion object {
        const val PATH = AppRoutes.MEMBER_PROFILE
        const val ARG_USER_ID = "userId"
    }
}

/** 잠금 화면. */
data object AccountLockedPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.ACCOUNT_LOCKED
}
