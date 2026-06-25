package com.ruleup.ui.helper

import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UiBindingsModule {
    @Binds
    @Singleton
    abstract fun bindNavigationHelper(impl: NavigationHelperImpl): NavigationHelper

    @Binds
    @Singleton
    abstract fun bindMessageHelper(impl: MessageHelperImpl): MessageHelper
}
