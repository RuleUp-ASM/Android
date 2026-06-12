package com.ruleup.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.ruleup.shared.di.IosAppGraph
import dev.zacsweers.metro.createGraphFactory
import platform.UIKit.UIViewController

/**
 * iOS 진입점. Swift 앱에서 `Shared.MainViewController()` 로 호출해 루트 ViewController 로 띄운다.
 * BASE_URL 은 local.properties → [AppConfig] 로 주입되므로 Swift 가 따로 넘기지 않는다.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        val graph = remember { createGraphFactory<IosAppGraph.Factory>().create(AppConfig.BASE_URL) }
        AppRoot(graph = graph)
    }
