package com.ruleup.network.image

import android.content.Context
import androidx.core.net.toUri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android 의 ContentResolver 로 content URI 에서 이미지 바이트를 읽는다.
 * iOS 추가 시 iosMain 에 PHPicker/파일 경로 기반 actual 구현을 ImageReader 로 바인딩하면 된다.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidImageReader(
    private val context: Context,
) : ImageReader {
    override suspend fun read(uri: String): ImageBytes {
        val parsed = uri.toUri()
        val resolver = context.contentResolver
        val bytes =
            withContext(Dispatchers.IO) {
                resolver.openInputStream(parsed)?.use { it.readBytes() }
            } ?: throw IllegalArgumentException("이미지를 읽을 수 없습니다: $uri")
        val mimeType = resolver.getType(parsed) ?: "image/*"
        return ImageBytes(bytes = bytes, mimeType = mimeType)
    }
}
