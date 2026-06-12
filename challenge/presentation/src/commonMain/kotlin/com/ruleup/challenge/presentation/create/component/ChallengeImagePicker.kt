package com.ruleup.challenge.presentation.create.component

import androidx.compose.runtime.Composable

/**
 * 챌린지 대표(배경) 이미지의 카메라 촬영 / 갤러리 선택을 추상화한 플랫폼 이미지 피커.
 *
 * 카메라·갤러리 접근은 플랫폼마다 구현이 다르므로(Android 는 ActivityResult + FileProvider),
 * UI(commonMain)는 이 인터페이스만 알고 실제 동작은 각 플랫폼 actual 이 제공한다.
 * 선택/촬영된 이미지의 URI(문자열)는 [rememberChallengeImagePicker] 의 콜백으로 전달된다.
 */
interface ChallengeImagePicker {
    /** 카메라 앱을 띄워 사진을 촬영한다. */
    fun launchCamera()

    /** 갤러리(사진 선택기)를 띄운다. */
    fun launchGallery()
}

/**
 * 현재 플랫폼의 [ChallengeImagePicker] 를 생성한다.
 * @param onImagePicked 선택/촬영 성공 시 이미지 URI 문자열을 받는 콜백.
 */
@Composable
expect fun rememberChallengeImagePicker(onImagePicked: (String) -> Unit): ChallengeImagePicker
