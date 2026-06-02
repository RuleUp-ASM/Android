package com.ruleup.data.profile.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

fun Context.resolveImage(uri: Uri): Pair<ByteArray, String> {
    val bytes =
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("이미지를 열 수 없습니다: $uri")

    val ext =
        contentResolver
            .getType(uri) // "image/jpeg"
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } // "jpg"
            ?: "jpg"

    val fileName = "profile_${System.currentTimeMillis()}.$ext"
    return bytes to fileName
}
