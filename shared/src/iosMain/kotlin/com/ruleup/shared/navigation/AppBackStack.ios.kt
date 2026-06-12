package com.ruleup.shared.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// 백스택 저장/복원 직렬화가 단일 NavKey 구현([GenericNavKey])의 다형성을 인식하도록 등록한다.
// (Android 의 rememberNavBackStack(vararg) 오버로드는 자동 처리하나, iOS 는 SavedStateConfiguration 이 필요.)
private val navBackStackConfig =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(GenericNavKey::class, GenericNavKey.serializer())
                }
            }
    }

@Composable
actual fun rememberAppBackStack(startStack: List<NavKey>): NavBackStack<NavKey> =
    rememberNavBackStack(navBackStackConfig, *startStack.toTypedArray())
