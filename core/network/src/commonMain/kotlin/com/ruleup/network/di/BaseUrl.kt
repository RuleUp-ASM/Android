package com.ruleup.network.di

import dev.zacsweers.metro.Qualifier

/**
 * Ktor HttpClient 의 base URL 주입용 한정자.
 * 값은 app 모듈(BuildConfig.BASE_URL)이 그래프에 @Provides 로 공급한다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BaseUrl
