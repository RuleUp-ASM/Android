package com.ruleup.challenge.presentation.common

import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** 카카오톡 초대 카드 템플릿. */
class InviteCardTemplateTest {
    private val link = Link(webUrl = "https://android.ruleup.co.kr/w/t1", androidExecutionParams = mapOf("invite" to "w", "token" to "t1"))

    @Test
    fun `대표 이미지를 올렸으면 이미지형 카드로 보내고 버튼도 같은 앱 실행 링크를 쓴다`() {
        // 버튼 링크가 다르면 이미지는 앱으로, 버튼은 스토어로 갈라진다(#574).
        val template = inviteCardTemplate("초대", "설명", "수락하기", link, imageUrl = "https://k.kakaocdn.net/x.png")

        val feed = assertIs<FeedTemplate>(template)
        assertEquals("https://k.kakaocdn.net/x.png", feed.content.imageUrl)
        assertEquals(link, feed.buttons?.single()?.link)
    }

    @Test
    fun `대표 이미지를 못 올렸으면 이미지 없이 텍스트 카드로라도 보낸다`() {
        // 업로드 실패로 공유 자체가 막히면 초대를 못 보낸다.
        val template = inviteCardTemplate("초대", "설명", "수락하기", link, imageUrl = null)

        assertIs<TextTemplate>(template)
    }
}
