package com.ruleup.android_ruleup.helper

import com.ruleup.android_ruleup.navigation.AppRouteAccessPolicy
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.RouteAccessPolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HelperBindingsModule {
    @Binds
    @Singleton
    abstract fun bindNavigationHelper(impl: NavigationHelperImpl): NavigationHelper

    @Binds
    @Singleton
    abstract fun bindMessageHelper(impl: MessageHelperImpl): MessageHelper

    @Binds
    @Singleton
    abstract fun bindPushNotificationHelper(impl: PushNotificationHelperImpl): PushNotificationHelper

    @Binds
    @Singleton
    abstract fun bindRouteAccessPolicy(impl: AppRouteAccessPolicy): RouteAccessPolicy
}
