package com.ruleup.challenge.presentation.invite

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate

/** 멤버 초대 링크 카카오톡 공유. */
object MemberInviteSharer {
    /** 초대 카드를 카카오톡으로 공유한다. */
    fun share(
        context: Context,
        challengeTitle: String,
        inviteUrl: String,
    ): Boolean {
        val template = inviteTemplate(challengeTitle, inviteUrl)
        return if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            ShareClient.instance.shareDefault(context, template) { result, _ ->
                result?.intent?.let(context::startActivity)
            }
            true
        } else {
            shareViaWeb(context, template)
        }
    }

    private fun inviteTemplate(
        challengeTitle: String,
        inviteUrl: String,
    ): TextTemplate =
        TextTemplate(
            text = "[$challengeTitle] 챌린지에 초대했어요. 같이 해볼래요?",
            link = Link(webUrl = inviteUrl, mobileWebUrl = inviteUrl),
            buttonTitle = "챌린지 보러 가기",
        )

    private fun shareViaWeb(
        context: Context,
        template: TextTemplate,
    ): Boolean {
        val url = WebSharerClient.instance.makeDefaultUrl(template)
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toString().toUri()))
            true
        }.getOrElse { it !is ActivityNotFoundException }
    }
}
