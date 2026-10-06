package com.ruleup.challenge.presentation.watcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.challenge.domain.navigation.InviteLaunchParams
import com.ruleup.challenge.presentation.common.inviteToken

/** 감시자 초대 카드 카카오톡 공유. */
object WatcherInviteSharer {
    /** 초대 카드를 카카오톡으로 공유한다. */
    fun share(
        context: Context,
        card: WatcherInviteCard,
        inviteUrl: String,
    ): Boolean {
        val template = inviteTemplate(card, inviteUrl)
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
        card: WatcherInviteCard,
        inviteUrl: String,
    ): TextTemplate =
        TextTemplate(
            text = "${card.title}\n${card.description}",
            link =
                Link(
                    webUrl = inviteUrl,
                    mobileWebUrl = inviteUrl,
                    // 설치돼 있으면 앱으로 바로 연다. 웹 주소만 두면 카카오톡 인앱 브라우저가 열고 서버가 스토어로 보낸다
                    androidExecutionParams = inviteToken(inviteUrl)?.let(InviteLaunchParams::watcher),
                ),
            buttonTitle = card.buttonLabel,
        )

    private fun shareViaWeb(
        context: Context,
        template: TextTemplate,
    ): Boolean =
        runCatching {
            val url = WebSharerClient.instance.makeDefaultUrl(template)
            context.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.recover { throwable ->
            if (throwable is ActivityNotFoundException) false else throw throwable
        }.getOrDefault(false)
}
