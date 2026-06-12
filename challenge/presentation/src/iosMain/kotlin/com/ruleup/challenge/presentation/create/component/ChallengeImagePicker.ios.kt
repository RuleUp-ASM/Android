package com.ruleup.challenge.presentation.create.component

import androidx.compose.runtime.Composable
import com.ruleup.ui.component.rememberSystemImagePicker

/**
 * iOS 챌린지 대표 이미지 피커. core:ui 의 시스템 피커(PHPicker/카메라)에 위임한다.
 */
@Composable
actual fun rememberChallengeImagePicker(onImagePicked: (String) -> Unit): ChallengeImagePicker {
    val system = rememberSystemImagePicker(onImagePicked)
    return object : ChallengeImagePicker {
        override fun launchCamera() = system.launchCamera()

        override fun launchGallery() = system.launchGallery()
    }
}
