package com.ruleup.domain.entity.user

import kotlin.test.Test
import kotlin.test.assertEquals

class NickNameUtilTest {
    @Test
    fun `자음만 나열한 닉네임은 쓸 수 있다`() {
        // 회원 정책 §3 — 자음만 나열(ㄱㄱㄱㄱ)은 명시적으로 허용이다.
        assertEquals(NicknameValidation.VALID, NickNameUtil.validate("ㄱㄱㄱㄱ"))
    }

    @Test
    fun `모음만 나열한 닉네임은 쓸 수 없다`() {
        // 자음만 나열과 같은 문자 집합에 있어 정규식 하나로는 갈라지지 않는다.
        assertEquals(NicknameValidation.INVALID_CHAR, NickNameUtil.validate("ㅏㅏㅏ"))
        assertEquals(NicknameValidation.INVALID_CHAR, NickNameUtil.validate("ㅗㅜ"))
    }

    @Test
    fun `모음이 섞인 닉네임은 막지 않는다`() {
        assertEquals(NicknameValidation.VALID, NickNameUtil.validate("ㅏ가"))
        assertEquals(NicknameValidation.VALID, NickNameUtil.validate("룰업러"))
    }

    @Test
    fun `길이는 2에서 12자다`() {
        assertEquals(NicknameValidation.OUT_OF_RANGE, NickNameUtil.validate("가"))
        assertEquals(NicknameValidation.OUT_OF_RANGE, NickNameUtil.validate("가".repeat(13)))
        assertEquals(NicknameValidation.VALID, NickNameUtil.validate("가".repeat(12)))
    }

    @Test
    fun `특수문자와 공백은 문자 사유로 막는다`() {
        assertEquals(NicknameValidation.INVALID_CHAR, NickNameUtil.validate("룰업 러"))
        assertEquals(NicknameValidation.INVALID_CHAR, NickNameUtil.validate("룰업!"))
    }
}
