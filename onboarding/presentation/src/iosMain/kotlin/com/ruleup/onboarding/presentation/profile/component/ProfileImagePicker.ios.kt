package com.ruleup.onboarding.presentation.profile.component

import androidx.compose.runtime.Composable

/**
 * iOS 프로필 이미지 피커 스텁.
 *
 * 실제 구현은 PHPickerViewController(갤러리)·UIImagePickerController(카메라)를 루트 UIViewController 에서
 * present 하고, 선택 이미지를 임시 파일로 저장해 file:// URI 를 [onImagePicked] 로 콜백하면 된다.
 * (카메라는 시뮬레이터에서 사용 불가) 현재는 no-op 스텁이다.
 */
@Composable
actual fun rememberProfileImagePicker(onImagePicked: (String) -> Unit): ProfileImagePicker = NoOpProfileImagePicker

private object NoOpProfileImagePicker : ProfileImagePicker {
    override fun launchCamera() {
        // TODO(iOS): UIImagePickerController(sourceType = Camera)
    }

    override fun launchGallery() {
        // TODO(iOS): PHPickerViewController
    }
}
