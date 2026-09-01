package com.ruleup.domain.test

import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.message.MessageEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 사용자에게 알린 내용을 모아두는 [MessageHelper]. 무엇을 알렸는지·안 알렸는지를 둘 다 본다. */
class RecordingMessageHelper : MessageHelper {
    val toasts = mutableListOf<String>()
    val snackBarTexts = mutableListOf<String>()
    val snackBarResIds = mutableListOf<Int>()
    val dialogDescriptions = mutableListOf<String>()

    /** 경로에 상관없이 사용자가 본 문구 전부. "무엇으로 알렸는지"가 계약이 아닐 때 쓴다. */
    val allMessages: List<String>
        get() = toasts + snackBarTexts + dialogDescriptions

    override val effect: Flow<MessageEffect> = emptyFlow()

    override fun showToast(toastMsg: String) {
        toasts += toastMsg
    }

    override fun showSnackBar(messageText: String) {
        snackBarTexts += messageText
    }

    override fun showSnackBar(messageRes: Int) {
        snackBarResIds += messageRes
    }

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogDescriptions += descText
    }
}
