package com.ruleup.network.image

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Android 의 ContentResolver 로 content URI 에서 이미지 바이트를 읽는다.
 */
class AndroidImageReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
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
