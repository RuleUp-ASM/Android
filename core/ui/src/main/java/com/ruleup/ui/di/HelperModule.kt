package com.ruleup.ui.di

import android.content.Context
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.helper.MessageHelperImpl
import com.ruleup.ui.helper.NavigationHelperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HelperModule {
    @Provides
    @Singleton
    fun provideMessageHelper(
        @ApplicationContext context: Context,
    ): MessageHelper = MessageHelperImpl(context)

    @Provides
    @Singleton
    fun provideNavigationHelper(): NavigationHelper = NavigationHelperImpl()
}
