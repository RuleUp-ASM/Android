package com.ruleup.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** 카메라 촬영 / 갤러리 선택을 추상화한 iOS 시스템 이미지 피커. */
interface SystemImagePicker {
    fun launchCamera()

    fun launchGallery()
}

/**
 * iOS 시스템 이미지 피커. 갤러리는 PHPickerViewController, 카메라는 UIImagePickerController 를 루트 VC 에서
 * present 하고, 선택/촬영 이미지를 임시 파일로 저장해 file:// URI 를 [onImagePicked] 로 콜백한다.
 * 각 feature 모듈의 expect actual(ProfileImagePicker/ChallengeImagePicker)이 이 헬퍼로 위임한다.
 */
@Composable
fun rememberSystemImagePicker(onImagePicked: (String) -> Unit): SystemImagePicker {
    // 프리뷰에는 UIViewController 가 없어 no-op.
    if (LocalInspectionMode.current) return remember { NoOpSystemImagePicker }
    return remember { IosSystemImagePicker(onImagePicked) }
}

private object NoOpSystemImagePicker : SystemImagePicker {
    override fun launchCamera() = Unit

    override fun launchGallery() = Unit
}

private class IosSystemImagePicker(
    private val onImagePicked: (String) -> Unit,
) : SystemImagePicker {
    // 피커 표시 중 델리게이트가 해제되지 않도록 강참조로 보존하고, 완료 시 해제한다.
    private var galleryDelegate: GalleryDelegate? = null
    private var cameraDelegate: CameraDelegate? = null

    override fun launchGallery() {
        val config =
            PHPickerConfiguration().apply {
                selectionLimit = 1
                filter = PHPickerFilter.imagesFilter()
            }
        val picker = PHPickerViewController(configuration = config)
        val delegate = GalleryDelegate(onImagePicked) { galleryDelegate = null }
        galleryDelegate = delegate
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    override fun launchCamera() {
        if (!UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
            )
        ) {
            return
        }
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        val delegate = CameraDelegate(onImagePicked) { cameraDelegate = null }
        cameraDelegate = delegate
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class GalleryDelegate(
    private val onImagePicked: (String) -> Unit,
    private val onFinish: () -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            onFinish()
            return
        }
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data: NSData?, _: NSError? ->
            val path = data?.let { saveImageData(it) }
            if (path != null) {
                dispatch_async(dispatch_get_main_queue()) { onImagePicked(path) }
            }
            onFinish()
        }
    }
}

private class CameraDelegate(
    private val onImagePicked: (String) -> Unit,
    private val onFinish: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.let { UIImageJPEGRepresentation(it, 0.9) }
        val path = data?.let { saveImageData(it) }
        if (path != null) onImagePicked(path)
        onFinish()
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onFinish()
    }
}

/** NSData(이미지)를 임시 디렉터리에 저장하고 file:// URI 문자열을 돌려준다. */
private fun saveImageData(data: NSData): String? {
    val path = NSTemporaryDirectory() + "img_${NSUUID().UUIDString}.jpg"
    return if (data.writeToFile(path, atomically = true)) {
        NSURL.fileURLWithPath(path).absoluteString
    } else {
        null
    }
}

/** 현재 화면 최상단 UIViewController(present 대상). */
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}
