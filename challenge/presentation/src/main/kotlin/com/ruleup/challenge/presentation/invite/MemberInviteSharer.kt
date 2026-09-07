package com.ruleup.challenge.presentation.invite

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate

/**
 * 멤버 초대 링크 카카오톡 공유 (Figma 1134:1646 위쪽 카드).
 *
 * 감시자 초대와 달리 **서버가 카드 문구를 주지 않는다**(발급 응답에 `kakaoShare` 가 없다) —
 * 그래서 문구를 여기서 만든다. 서버가 나중에 페이로드를 주면 그 값을 쓰도록 바꾼다.
 */
object MemberInviteSharer {
    /**
     * 초대 카드를 카카오톡으로 공유한다. 카카오톡 미설치면 웹 공유(브라우저)로 폴백한다.
     * @return 공유 UI 를 띄우지 못했으면 false (호출부가 안내 토스트 처리)
     */
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
