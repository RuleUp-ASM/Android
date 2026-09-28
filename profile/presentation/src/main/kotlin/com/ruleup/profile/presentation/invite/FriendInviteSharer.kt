package com.ruleup.profile.presentation.invite

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate

/** 친구 초대 링크 카카오톡 공유. */
object FriendInviteSharer {
    /** @return 공유 화면 표시 여부. */
    fun share(
        context: Context,
        inviteUrl: String,
        inviteCode: String,
    ): Boolean {
        val template =
            TextTemplate(
                text = "RuleUp에서 함께 약속을 지켜봐요!\n초대 코드: $inviteCode",
                link =
                    Link(
                        webUrl = inviteUrl,
                        mobileWebUrl = inviteUrl,
                    ),
                buttonTitle = "시작하기",
            )
        return if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            ShareClient.instance.shareDefault(context, template) { result, _ ->
                result?.intent?.let(context::startActivity)
            }
            true
        } else {
            runCatching {
                val url = WebSharerClient.instance.makeDefaultUrl(template)
                context.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.recover { throwable ->
                if (throwable is ActivityNotFoundException) false else throw throwable
            }.getOrDefault(false)
        }
    }
}
