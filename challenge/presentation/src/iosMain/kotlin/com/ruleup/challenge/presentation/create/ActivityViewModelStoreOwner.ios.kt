package com.ruleup.challenge.presentation.create

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * iOS 엔 Activity 가 없으므로, 챌린지 생성 플로우(입력·확인) 화면들이 공유할 단일 [ViewModelStore] 를 가진
 * 프로세스 스코프 owner 를 제공한다. (Android 의 LocalActivity 기반 owner 대응)
 */
private object SharedCreateChallengeViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

@Composable
actual fun rememberActivityViewModelStoreOwner(): ViewModelStoreOwner = SharedCreateChallengeViewModelStoreOwner
