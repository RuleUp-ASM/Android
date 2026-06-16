package com.ruleup.onboarding.presentation.profile

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * iOS 엔 Activity 가 없으므로, 프로필 설정 플로우 화면들이 공유할 단일 [ViewModelStore] 를 가진
 * 프로세스 스코프 owner 를 제공한다. (Android 의 LocalActivity 기반 owner 대응)
 */
private object SharedProfileViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

@Composable
actual fun rememberActivityViewModelStoreOwner(): ViewModelStoreOwner = SharedProfileViewModelStoreOwner
