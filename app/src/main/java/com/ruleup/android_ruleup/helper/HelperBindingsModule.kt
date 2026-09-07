package com.ruleup.android_ruleup.helper

import com.ruleup.android_ruleup.deeplink.RuleUpSchemeResolver
import com.ruleup.android_ruleup.navigation.AppRouteAccessPolicy
import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.DeeplinkResolver
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

    /** 알림 딥링크(`ruleup://…`) 해석. 라우트 표를 아는 건 컴포지션 루트뿐이다. */
    @Binds
    @Singleton
    abstract fun bindDeeplinkResolver(impl: RuleUpSchemeResolver): DeeplinkResolver
}
