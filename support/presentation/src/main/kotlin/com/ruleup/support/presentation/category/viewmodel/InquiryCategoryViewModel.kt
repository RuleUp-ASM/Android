package com.ruleup.support.presentation.category.viewmodel

import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.support.domain.navigation.InquiryComposePage
import com.ruleup.support.domain.navigation.InquiryListPage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 문의하기 · 카테고리 ViewModel (Figma `1417:2`).
 *
 * 바로가기는 `AppRoutes` 상수로 직접 이동한다 — 대상이 profile·verification 쪽 화면이라 그
 * feature 의 `Page` 를 쓰려면 domain 의존을 더 끌어와야 하는데, 경로 하나 때문에 모듈 그래프를
 * 넓히는 것보다 단일 소스인 상수를 참조하는 편이 가볍다.
 */
@HiltViewModel
class InquiryCategoryViewModel
    @Inject
    constructor(
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<InquiryCategoryIntent, InquiryCategoryState, InquiryCategoryReducerEvent, NoEffect>(
            InquiryCategoryState,
        ) {
        override fun onIntent(intent: InquiryCategoryIntent) {
            when (intent) {
                InquiryCategoryIntent.Back -> navigationHelper.navigateToBack()

                is InquiryCategoryIntent.Select ->
                    navigationHelper.navigateTo(InquiryComposePage(intent.category))

                InquiryCategoryIntent.OpenHistory -> navigationHelper.navigateTo(InquiryListPage)

                is InquiryCategoryIntent.OpenShortcut ->
                    navigationHelper.navigateByRoute(NavRoute(intent.shortcut.path))
            }
        }

        override fun reduce(
            state: InquiryCategoryState,
            event: InquiryCategoryReducerEvent,
        ): InquiryCategoryState = state
    }

private val InquiryShortcut.path: String
    get() =
        when (this) {
            InquiryShortcut.APPEAL -> AppRoutes.MY_APPEALS
            InquiryShortcut.WATCHING -> AppRoutes.MY_WATCHING
        }
