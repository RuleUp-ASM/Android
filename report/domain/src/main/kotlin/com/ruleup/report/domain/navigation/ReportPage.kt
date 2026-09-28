package com.ruleup.report.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 신고하기. */
data class ReportPage(
    val userId: String? = null,
    val challengeId: String? = null,
    val targetName: String,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            buildMap {
                userId?.let { put(ARG_USER_ID, it) }
                challengeId?.let { put(ARG_CHALLENGE_ID, it) }
                put(ARG_TARGET_NAME, targetName)
            },
        )

    companion object {
        const val PATH = AppRoutes.REPORT
        const val ARG_USER_ID = "userId"
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_TARGET_NAME = "targetName"
    }
}
