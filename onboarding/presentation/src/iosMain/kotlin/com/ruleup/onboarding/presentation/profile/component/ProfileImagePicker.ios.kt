package com.ruleup.onboarding.presentation.profile.component

import androidx.compose.runtime.Composable
import com.ruleup.ui.component.rememberSystemImagePicker

/**
 * iOS 프로필 이미지 피커. core:ui 의 시스템 피커(PHPicker/카메라)에 위임한다.
 */
@Composable
actual fun rememberProfileImagePicker(onImagePicked: (String) -> Unit): ProfileImagePicker {
    val system = rememberSystemImagePicker(onImagePicked)
    return object : ProfileImagePicker {
        override fun launchCamera() = system.launchCamera()

        override fun launchGallery() = system.launchGallery()
    }
}
