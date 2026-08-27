package com.ruleup.domain.entity.user

/**
 * 닉네임 규칙 (회원 정책 §3 · 명세 nicknames/check). **가입과 프로필 수정이 같은 규칙을 본다** —
 * 한쪽만 고쳐지면 온보딩에서 막히는 닉네임이 수정으로는 통과한다.
 *
 * 자음만 나열(`ㄱㄱㄱㄱ`)은 허용하고 **모음만 나열(`ㅏㅏㅏ`)은 불허**한다. 둘 다 같은 문자 집합에
 * 들어 있어 정규식 하나로는 갈라지지 않으므로 별도로 본다.
 */
object NickNameUtil {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 12

    private val NICKNAME_REGEX = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]+$")

    private val VOWELS_ONLY_REGEX = Regex("^[ㅏ-ㅣ]+$")

    fun inRange(name: String): Boolean = name.length in MIN_LENGTH..MAX_LENGTH

    fun isValidName(name: String): Boolean = NICKNAME_REGEX.matches(name) && !VOWELS_ONLY_REGEX.matches(name)

    /** 문자 종류를 길이보다 먼저 본다 — 특수문자가 섞이면 길이 안내가 아니라 문자 안내가 나간다. */
    fun validate(name: String): NicknameValidation =
        when {
            name.isEmpty() -> NicknameValidation.OUT_OF_RANGE
            !isValidName(name) -> NicknameValidation.INVALID_CHAR
            !inRange(name) -> NicknameValidation.OUT_OF_RANGE
            else -> NicknameValidation.VALID
        }

    fun message(validation: NicknameValidation): String =
        when (validation) {
            NicknameValidation.VALID -> "사용 가능한 닉네임이에요"
            NicknameValidation.INVALID_CHAR -> "한글·영문·숫자만 쓸 수 있어요 (모음만 나열·특수문자·공백 불가)"
            NicknameValidation.OUT_OF_RANGE -> "$MIN_LENGTH~${MAX_LENGTH}자로 입력해주세요"
        }
}

enum class NicknameValidation {
    VALID,
    INVALID_CHAR,
    OUT_OF_RANGE,
    ;

    val isValid: Boolean get() = this == VALID
}
