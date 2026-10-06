package com.ruleup.challenge.presentation.common

import android.content.Context
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.DefaultTemplate
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.ruleup.challenge.presentation.R
import java.io.File

/**
 * 카카오톡 초대 카드의 대표 이미지(Figma 「카톡 초대 카드 대표 이미지」 C안).
 *
 * 카카오 카드는 공개 이미지 주소만 받는다. 호스팅할 곳이 없어 앱에 넣어 둔 이미지를
 * 카카오 이미지 서버에 올려 그 주소를 쓴다. 올린 주소는 프로세스가 살아 있는 동안 재사용한다.
 */
internal object InviteCardImage {
    const val WIDTH = 800
    const val HEIGHT = 400

    @Volatile
    private var uploadedUrl: String? = null

    /** 이미지 주소를 넘긴다. 올리지 못하면 null — 그때는 이미지 없는 카드로 보낸다. */
    fun withUrl(
        context: Context,
        onReady: (String?) -> Unit,
    ) {
        uploadedUrl?.let {
            onReady(it)
            return
        }
        val file =
            runCatching {
                File(context.cacheDir, "invite_card.png").also { target ->
                    context.resources.openRawResource(R.raw.invite_card).use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }
                }
            }.getOrNull()
        if (file == null) {
            onReady(null)
            return
        }
        ShareClient.instance.uploadImage(file) { result, _ ->
            val url = result?.infos?.original?.url
            if (url != null) uploadedUrl = url
            onReady(url)
        }
    }
}

/** 초대 카드 템플릿. 대표 이미지를 올렸으면 이미지형, 아니면 지금까지의 텍스트형으로 보낸다. */
internal fun inviteCardTemplate(
    title: String,
    description: String?,
    buttonTitle: String,
    link: Link,
    imageUrl: String?,
): DefaultTemplate =
    if (imageUrl != null) {
        FeedTemplate(
            content =
                Content(
                    title = title,
                    imageUrl = imageUrl,
                    link = link,
                    description = description,
                    imageWidth = InviteCardImage.WIDTH,
                    imageHeight = InviteCardImage.HEIGHT,
                ),
            buttons = listOf(Button(title = buttonTitle, link = link)),
        )
    } else {
        TextTemplate(
            text = listOfNotNull(title, description).joinToString("\n"),
            link = link,
            buttonTitle = buttonTitle,
        )
    }
