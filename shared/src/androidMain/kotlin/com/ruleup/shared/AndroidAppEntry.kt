package com.ruleup.shared

import android.content.Context
import com.ruleup.shared.di.AndroidAppGraph
import com.ruleup.shared.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

/**
 * Android 앱 전역 DI 그래프를 생성한다. iOS 의 [MainViewController] 와 대칭으로 컴포지션 루트(그래프 생성)를
 * :shared 에 두어, :app 은 생명주기 호스트(Application/Activity)·플랫폼 초기화만 담당하는 얇은 셸로 남긴다.
 *
 * [Context] 는 datastore·이미지 업로드 등 Android 바인딩에 필요하므로 Application.onCreate 에서 1회 호출한다.
 */
fun createAppGraph(
    context: Context,
    baseUrl: String,
): AppGraph = createGraphFactory<AndroidAppGraph.Factory>().create(context, baseUrl)
