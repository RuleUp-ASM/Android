package com.ruleup.challenge.presentation.invite

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.DefaultTemplate
import com.kakao.sdk.template.model.Link
import com.ruleup.challenge.domain.navigation.InviteLaunchParams
import com.ruleup.challenge.presentation.common.InviteCardImage
import com.ruleup.challenge.presentation.common.inviteCardTemplate
import com.ruleup.challenge.presentation.common.inviteToken

/** 멤버 초대 링크 카카오톡 공유. */
object MemberInviteSharer {
    /** 초대 카드를 카카오톡으로 공유한다. */
    fun share(
        context: Context,
        challengeTitle: String,
        inviteUrl: String,
    ): Boolean {
        if (!ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            return shareViaWeb(context, inviteTemplate(challengeTitle, inviteUrl, imageUrl = null))
        }
        // 대표 이미지를 올린 뒤 보낸다. 못 올려도 이미지 없이 보낸다
        InviteCardImage.withUrl(context) { imageUrl ->
            ShareClient.instance.shareDefault(context, inviteTemplate(challengeTitle, inviteUrl, imageUrl)) { result, _ ->
                result?.intent?.let(context::startActivity)
            }
        }
        return true
    }

    private fun inviteTemplate(
        challengeTitle: String,
        inviteUrl: String,
        imageUrl: String?,
    ): DefaultTemplate =
        inviteCardTemplate(
            title = "[$challengeTitle] 챌린지에 초대했어요.",
            description = "같이 해볼래요?",
            buttonTitle = "챌린지 보러 가기",
            link =
                Link(
                    webUrl = inviteUrl,
                    mobileWebUrl = inviteUrl,
                    // 설치돼 있으면 앱으로 바로 연다. 웹 주소만 두면 카카오톡 인앱 브라우저가 열고 서버가 스토어로 보낸다
                    androidExecutionParams = inviteToken(inviteUrl)?.let(InviteLaunchParams::challenge),
                ),
            imageUrl = imageUrl,
        )

    private fun shareViaWeb(
        context: Context,
        template: DefaultTemplate,
    ): Boolean {
        val url = WebSharerClient.instance.makeDefaultUrl(template)
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toString().toUri()))
            true
        }.getOrElse { it !is ActivityNotFoundException }
    }
}
