package com.ruleup.network.image

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

/**
 * iOS 이미지 리더. 이미지 피커가 임시 파일로 저장한 file:// URI 에서 바이트를 읽는다.
 * [com.ruleup.network.image.AndroidImageReader] 의 iOS 대응 actual 바인딩.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosImageReader : ImageReader {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun read(uri: String): ImageBytes {
        val data =
            withContext(Dispatchers.Default) {
                val url = NSURL.URLWithString(uri) ?: throw IllegalArgumentException("잘못된 URI: $uri")
                NSData.dataWithContentsOfURL(url)
            } ?: throw IllegalArgumentException("이미지를 읽을 수 없습니다: $uri")

        val bytes = data.toByteArray()
        val mimeType =
            when (uri.substringAfterLast('.', "").lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
        return ImageBytes(bytes = bytes, mimeType = mimeType)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size == 0) return result
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
