package com.ruleup.onboarding.domain.auth

object NickNameUtil {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 12

    // 한글 음절(가-힣) + 단일 자/모음(ㄱ-ㅎ, ㅏ-ㅣ) + 영문 + 숫자 허용. 특수문자·공백 불가.
    private val NICKNAME_REGEX = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]+$")

    fun inRange(name: String): Boolean = name.length in MIN_LENGTH..MAX_LENGTH

    fun isValidName(name: String): Boolean = NICKNAME_REGEX.matches(name)

    fun isValid(name: String): Boolean = inRange(name) && isValidName(name)

    /**
     * 입력값을 검증해 실패 "사유"까지 구분해 돌려준다.
     * 문자 종류를 길이보다 먼저 보므로, 특수문자가 섞이면 길이 안내가 아니라 문자 안내가 나간다.
     */
    fun validate(name: String): NicknameValidation =
        when {
            name.isEmpty() -> NicknameValidation.OUT_OF_RANGE
            !isValidName(name) -> NicknameValidation.INVALID_CHAR
            !inRange(name) -> NicknameValidation.OUT_OF_RANGE
            else -> NicknameValidation.VALID
        }

    /** 검증 결과에 맞는 안내 문구. 인라인 메시지와 스낵바가 같은 문구를 쓰도록 한곳에 둔다. */
    fun message(validation: NicknameValidation): String =
        when (validation) {
            NicknameValidation.VALID -> "사용 가능한 닉네임이에요"
            NicknameValidation.INVALID_CHAR -> "한글·영문·숫자만 쓸 수 있어요 (특수문자·공백 불가)"
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
