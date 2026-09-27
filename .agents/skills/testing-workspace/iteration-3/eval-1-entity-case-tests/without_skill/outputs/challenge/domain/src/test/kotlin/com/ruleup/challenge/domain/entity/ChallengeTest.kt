package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `mode` 는 방 홈·랭킹·멤버 관리가 열리는지를 가른다. data 매핑이 모르는 값을 SOLO 로 떨어뜨리므로,
 * 값 문자열이 하나라도 어긋나면 그룹 방이 솔로로 조용히 뒤바뀐다.
 */
class ChallengeModeTest {
    @Test
    fun `명세의 2종을 값 그대로 매핑한다`() {
        assertEquals(ChallengeMode.SOLO, ChallengeMode.fromValue("SOLO"))
        assertEquals(ChallengeMode.GROUP, ChallengeMode.fromValue("GROUP"))
        assertEquals(2, ChallengeMode.entries.size)
    }

    @Test
    fun `모르는 참여 형태는 null 이다`() {
        // 구 participationType 값이 남아 있어도 앱이 터지지 않고 호출부 기본값으로 떨어져야 한다.
        assertNull(ChallengeMode.fromValue("PARTICIPATION_GROUP"))
        assertNull(ChallengeMode.fromValue("solo"))
        assertNull(ChallengeMode.fromValue(null))
    }

    @Test
    fun `그룹만 함께하는 챌린지다`() {
        assertTrue(ChallengeMode.GROUP.isGroup)
        assertFalse(ChallengeMode.SOLO.isGroup)
    }
}

/** `visibility` 는 그룹 전용이라 솔로에서는 null 로 남는다 — 매핑이 null 을 그대로 통과시켜야 한다. */
class ChallengeVisibilityTest {
    @Test
    fun `명세의 2종을 값 그대로 매핑한다`() {
        assertEquals(ChallengeVisibility.PUBLIC, ChallengeVisibility.fromValue("PUBLIC"))
        assertEquals(ChallengeVisibility.PRIVATE, ChallengeVisibility.fromValue("PRIVATE"))
        assertEquals(2, ChallengeVisibility.entries.size)
    }

    @Test
    fun `모르는 공개 범위는 null 이다`() {
        assertNull(ChallengeVisibility.fromValue("SECRET"))
        assertNull(ChallengeVisibility.fromValue(null))
    }

    @Test
    fun `비공개만 초대 링크로 들어온다`() {
        // 탐색 노출과 참여 게이트가 이 값 하나로 갈린다.
        assertTrue(ChallengeVisibility.PRIVATE.isPrivate)
        assertFalse(ChallengeVisibility.PUBLIC.isPrivate)
    }
}

/** `status` 는 상세·목록·생성 응답이 공유한다. 만료 후 자동 삭제는 배치 몫이라 여기에 상태가 없다. */
class ChallengeStatusTest {
    @Test
    fun `명세의 3종을 값 그대로 매핑한다`() {
        assertEquals(ChallengeStatus.UPCOMING, ChallengeStatus.fromValue("UPCOMING"))
        assertEquals(ChallengeStatus.ACTIVE, ChallengeStatus.fromValue("ACTIVE"))
        assertEquals(ChallengeStatus.COMPLETED, ChallengeStatus.fromValue("COMPLETED"))
        assertEquals(3, ChallengeStatus.entries.size)
    }

    @Test
    fun `모르는 상태는 null 이다`() {
        // DELETED 처럼 서버가 상태를 늘려도 호출부 기본값으로 떨어질 뿐 앱은 살아 있어야 한다.
        assertNull(ChallengeStatus.fromValue("DELETED"))
        assertNull(ChallengeStatus.fromValue(null))
    }
}

/**
 * `editableFields` 는 서버가 계산한 폼 잠금 결과다. 값 문자열이 어긋나면 mapNotNull 이 그 필드를
 * 통째로 버려, 사실은 수정 가능한 항목이 화면에서 잠긴 채로 남는다.
 */
class ChallengeFieldTest {
    @Test
    fun `명세의 13종을 모두 매핑한다`() {
        val expected =
            mapOf(
                "title" to ChallengeField.TITLE,
                "description" to ChallengeField.DESCRIPTION,
                "imageUrl" to ChallengeField.IMAGE_URL,
                "mode" to ChallengeField.MODE,
                "visibility" to ChallengeField.VISIBILITY,
                "rankingVisible" to ChallengeField.RANKING_VISIBLE,
                "capacity" to ChallengeField.CAPACITY,
                "minTier" to ChallengeField.MIN_TIER,
                "period" to ChallengeField.PERIOD,
                "weeklyCount" to ChallengeField.WEEKLY_COUNT,
                "params" to ChallengeField.PARAMS,
                "verification" to ChallengeField.VERIFICATION,
                "penalties" to ChallengeField.PENALTIES,
            )

        expected.forEach { (value, field) -> assertEquals(field, ChallengeField.fromValue(value)) }
        assertEquals(expected.size, ChallengeField.entries.size)
    }

    @Test
    fun `모르는 필드는 null 이다`() {
        // 서버가 필드를 추가해도 나머지 잠금 계산은 그대로 살아 있어야 한다.
        assertNull(ChallengeField.fromValue("startDate"))
        assertNull(ChallengeField.fromValue("TITLE"))
        assertNull(ChallengeField.fromValue(null))
    }
}

/** 부분 수정 계약 — null 은 "값 삭제"가 아니라 "미변경"이고, 이미지 삭제만 별도 플래그로 말한다. */
class ChallengeUpdateTest {
    @Test
    fun `버전만 담으면 아무것도 바꾸지 않는다`() {
        // 기본값이 흔들리면 화면이 건드리지도 않은 필드가 PATCH 에 실려 나간다.
        val update = ChallengeUpdate(version = 3)

        assertEquals(3, update.version)
        assertNull(update.title)
        assertNull(update.description)
        assertNull(update.imageUrl)
        assertNull(update.mode)
        assertNull(update.visibility)
        assertNull(update.rankingVisible)
        assertNull(update.capacity)
        assertNull(update.minTier)
        assertNull(update.period)
        assertNull(update.weeklyCount)
        assertNull(update.params)
        assertNull(update.verification)
        assertNull(update.watcherPenalty)
    }

    @Test
    fun `이미지 되돌리기는 기본으로 꺼져 있다`() {
        // imageUrl 을 안 보낸 것과 "기본 이미지로 되돌리기"는 서버에서 전혀 다른 요청이다.
        assertFalse(ChallengeUpdate(version = 1).removeImage)
        assertTrue(ChallengeUpdate(version = 1, removeImage = true).removeImage)
    }
}

/** 409 응답이 editableFields 를 빠뜨려도 화면이 널 처리 없이 폼을 다시 그릴 수 있어야 한다. */
class ChallengeNotEditableExceptionTest {
    @Test
    fun `수정 가능 목록 없이도 만들 수 있고 그때는 빈 집합이다`() {
        assertEquals(emptySet(), ChallengeNotEditableException().editableFields)
    }

    @Test
    fun `서버가 준 수정 가능 목록을 그대로 들고 있는다`() {
        val fields = setOf(ChallengeField.TITLE, ChallengeField.DESCRIPTION)

        assertEquals(fields, ChallengeNotEditableException(fields).editableFields)
    }
}

/** 429 는 해제 시각을 함께 주지만, 안 줄 수도 있다 — 그때도 일반 안내로 떨어져야 한다. */
class ModerationLockedExceptionTest {
    @Test
    fun `해제 시각은 없을 수 있다`() {
        assertNull(ModerationLockedException().retryAfterSeconds)
        assertEquals(3_600, ModerationLockedException(retryAfterSeconds = 3_600).retryAfterSeconds)
    }
}
