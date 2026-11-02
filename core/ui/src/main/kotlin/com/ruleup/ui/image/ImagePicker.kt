package com.ruleup.ui.image

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/** 카메라·갤러리 이미지 선택기. */
interface ImagePicker {
    fun launchCamera()

    fun launchGallery()
}

@Suppress("ktlint:compose:parameter-naming")
@Composable
fun rememberImagePicker(onImagePicked: (String) -> Unit): ImagePicker {
    if (LocalInspectionMode.current) return NoOpImagePicker

    val context = LocalContext.current

    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onImagePicked(uri.toString())
        }

    var cameraImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraImageUri
            if (success && uri != null) onImagePicked(uri)
        }

    fun takePicture() {
        val uri = createCameraImageUri(context)
        cameraImageUri = uri.toString()
        camera.launch(uri)
    }

    // 앱이 CAMERA 를 선언해 두었으므로, 허용 전에 카메라 앱을 띄우면 SecurityException 이 난다.
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) takePicture()
        }

    return object : ImagePicker {
        override fun launchCamera() {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            if (granted) takePicture() else cameraPermission.launch(Manifest.permission.CAMERA)
        }

        override fun launchGallery() {
            gallery.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}

private object NoOpImagePicker : ImagePicker {
    override fun launchCamera() = Unit

    override fun launchGallery() = Unit
}

private fun createCameraImageUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("image_", ".jpg", cameraDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}
