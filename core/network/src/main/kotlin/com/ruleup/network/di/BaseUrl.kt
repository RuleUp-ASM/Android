package com.ruleup.network.di

import javax.inject.Qualifier

/**
 * Retrofit base URL 주입용 한정자.
 * 값은 app 모듈(BuildConfig.BASE_URL)이 Hilt 모듈로 공급한다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BaseUrl
