package com.ruleup.challenge.presentation.create

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner

/**
 * 챌린지 생성 플로우(입력·AI 추천 확인)가 단일 [CreateChallengeViewModel] 인스턴스를 공유하도록,
 * 화면(네비게이션 엔트리)보다 상위의 스코프(Android 는 Activity)를 가진 [ViewModelStoreOwner] 를 제공한다.
 *
 * 화면별 기본 owner([androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner])를 쓰면
 * 페이지마다 ViewModel 이 새로 생성돼 입력값이 누적되지 않으므로, 상위 스코프 owner 가 필요하다.
 */
@Composable
expect fun rememberActivityViewModelStoreOwner(): ViewModelStoreOwner
