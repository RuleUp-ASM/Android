package com.ruleup.network.image

/** 플랫폼별 이미지 소스(content URI 등)를 멀티파트 업로드용 바이트로 읽어들이는 추상화. */
interface ImageReader {
    suspend fun read(uri: String): ImageBytes
}

/** 업로드에 필요한 이미지 바이트와 MIME 타입. */
class ImageBytes(
    val bytes: ByteArray,
    val mimeType: String,
)
