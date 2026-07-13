package com.ruleup.challenge.presentation.watcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate

/**
 * 감시자 초대 카드 카카오톡 공유(감시자 통지 스펙 메시지 ① · 중간 톤).
 *
 * 초대는 반드시 **사용자 본인 명의의 카카오톡 공유**로만 전달한다(룰업이 감시자에게 직접
 * 발송하는 것은 금지 — 발신 주체가 합법/위법의 분기점). "실패하면 알림이 가요" 를 카드에
 * 명시해 수락 = 수신동의임을 투명하게 한다.
 */
object WatcherInviteSharer {
    /**
     * 초대 카드를 카카오톡으로 공유한다. 카카오톡 미설치면 웹 공유(브라우저)로 폴백한다.
     * @return 공유 UI 를 띄우지 못했으면 false (호출부가 안내 토스트 처리)
     */
    fun share(
        context: Context,
        ownerNickname: String,
        challengeTitle: String,
        inviteUrl: String,
    ): Boolean {
        val template = inviteTemplate(ownerNickname, challengeTitle, inviteUrl)
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
        ownerNickname: String,
        challengeTitle: String,
        inviteUrl: String,
    ): TextTemplate =
        TextTemplate(
            text =
                "${ownerNickname}님이 당신을 루틴 감시자로 초대했어요\n" +
                    "[$challengeTitle]에서 ${ownerNickname}님이 약속을 지키는지 지켜봐 주세요. " +
                    "실패하면 알림이 가요.",
            link =
                Link(
                    webUrl = inviteUrl,
                    mobileWebUrl = inviteUrl,
                ),
            buttonTitle = "수락하기",
        )

    // 카카오톡 미설치: 카카오 웹 공유 페이지를 브라우저로 연다.
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
