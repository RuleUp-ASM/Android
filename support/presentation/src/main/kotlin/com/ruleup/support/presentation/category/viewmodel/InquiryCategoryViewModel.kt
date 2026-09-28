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

/** 문의하기 · 카테고리 ViewModel. */
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
                InquiryCategoryIntent.Back -> {
                    navigationHelper.navigateToBack()
                }

                is InquiryCategoryIntent.Select -> {
                    navigationHelper.navigateTo(InquiryComposePage(intent.category))
                }

                InquiryCategoryIntent.OpenHistory -> {
                    navigationHelper.navigateTo(InquiryListPage)
                }

                is InquiryCategoryIntent.OpenShortcut -> {
                    navigationHelper.navigateByRoute(NavRoute(intent.shortcut.path))
                }
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
