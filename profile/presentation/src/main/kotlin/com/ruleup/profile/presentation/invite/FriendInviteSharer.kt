package com.ruleup.profile.presentation.invite

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate

/**
 * 친구 초대 링크 카카오톡 공유. 초대는 사용자 본인 명의 채널로만 전달한다(룰업 직접 발송 금지 —
 * 감시자 초대와 동일 원칙). 카카오톡 미설치면 웹 공유(브라우저)로 폴백한다.
 */
object FriendInviteSharer {
    /** @return 공유 UI 를 띄우지 못했으면 false (호출부가 안내 토스트 처리) */
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
