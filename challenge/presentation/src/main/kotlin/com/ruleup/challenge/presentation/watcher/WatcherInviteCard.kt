package com.ruleup.challenge.presentation.watcher

import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInviteCard

/** 카톡 공유 카드 문구. 서버가 문구를 주지 않으면 기본 문구를 쓴다. */
internal fun WatcherInvitation.inviteCard(challengeTitle: String): WatcherInviteCard =
    kakaoShare
        ?: WatcherInviteCard(
            title = "당신을 루틴 감시자로 초대했어요",
            description = "[$challengeTitle]에서 약속을 지키는지 지켜봐 주세요. 실패하면 알림이 가요.",
            buttonLabel = "수락하기",
        )
