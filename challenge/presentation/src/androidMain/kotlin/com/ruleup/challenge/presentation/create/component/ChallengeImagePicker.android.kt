package com.ruleup.challenge.presentation.create.component

import android.content.Context
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
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberChallengeImagePicker(onImagePicked: (String) -> Unit): ChallengeImagePicker {
    // 프리뷰(@Preview)에는 Activity/ActivityResultRegistry 가 없어 런처 등록이 불가하므로 no-op 을 돌려준다.
    if (LocalInspectionMode.current) return NoOpChallengeImagePicker

    val context = LocalContext.current

    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onImagePicked(uri.toString())
        }

    // 촬영 결과가 저장될 URI. 카메라 앱이 떠 있는 동안 프로세스가 재생성될 수 있어 saveable 로 보존한다.
    var cameraImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraImageUri
            if (success && uri != null) onImagePicked(uri)
        }

    return object : ChallengeImagePicker {
        override fun launchCamera() {
            val uri = createCoverImageUri(context)
            cameraImageUri = uri.toString()
            camera.launch(uri)
        }

        override fun launchGallery() {
            gallery.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}

/** 프리뷰용 no-op 피커. */
private object NoOpChallengeImagePicker : ChallengeImagePicker {
    override fun launchCamera() = Unit

    override fun launchGallery() = Unit
}

/** 카메라 촬영 결과를 받을 캐시 파일 URI. onboarding 모듈이 등록한 FileProvider(merged manifest)를 재사용한다. */
private fun createCoverImageUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("cover_", ".jpg", cameraDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}
