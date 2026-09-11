package com.ruleup.support.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page
import com.ruleup.support.domain.entity.InquiryCategory

/** 문의하기 · 카테고리 페이지 (Figma `1417:2`). 설정 허브 → 문의하기. */
data object InquiryCategoryPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_INQUIRY_NEW
}

/**
 * 문의하기 · 작성 페이지 (Figma `1417:117`).
 *
 * 분류를 인자로 받는다 — 카테고리 화면을 거치지 않고 이 화면만 복원돼도 분류 없이 접수되는 일이
 * 없어야 한다. 화면 안에서 분류를 바꾸면 같은 경로를 새 인자로 다시 연다.
 */
data class InquiryComposePage(
    val category: InquiryCategory,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CATEGORY to category.value))

    companion object {
        const val PATH = AppRoutes.MY_INQUIRY_COMPOSE
        const val ARG_CATEGORY = "category"
    }
}

/** 내 문의 내역 페이지 (Figma `1419:27`). 설정 허브 → 내 문의 내역. */
data object InquiryListPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_INQUIRIES
}

/**
 * 문의 상세 페이지 (Figma `1419:87`). 열람 전용이다.
 *
 * 명세는 답변 알림의 딥링크(`ruleup://me/inquiries/{id}`)도 이 화면으로 받게 돼 있지만,
 * **답변을 알림함으로 알리지 않기로 해(2026-09-11) 딥링크 진입점은 붙이지 않았다.** 통지를 켜게
 * 되면 경로 매핑만 추가하면 되고 이 페이지는 그대로 쓴다.
 */
data class InquiryDetailPage(
    val inquiryId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_INQUIRY_ID to inquiryId))

    companion object {
        const val PATH = AppRoutes.MY_INQUIRY_DETAIL
        const val ARG_INQUIRY_ID = "inquiryId"
    }
}
