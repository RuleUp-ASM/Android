package com.ruleup.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.ruleup.shared.di.IosAppGraph
import dev.zacsweers.metro.createGraphFactory
import platform.UIKit.UIViewController

/**
 * iOS 진입점. Swift 앱(추후 Xcode 프로젝트)에서 `Shared.MainViewController(baseUrl:)` 로 호출해
 * 루트 ViewController 로 띄운다.
 */
fun MainViewController(baseUrl: String): UIViewController =
    ComposeUIViewController {
        val graph = remember { createGraphFactory<IosAppGraph.Factory>().create(baseUrl) }
        AppRoot(graph = graph)
    }
