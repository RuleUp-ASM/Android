package com.ruleup.domain.helper

import com.ruleup.domain.message.MessageEffect
import kotlinx.coroutines.flow.Flow

interface MessageHelper {
    val effect: Flow<MessageEffect>

    fun showToast(toastMsg: String)

    fun showSnackBar(messageText: String)

    fun showSnackBar(messageRes: Int)

    fun showOneButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        buttonText: String = "Ok",
        onClickButton: (() -> Unit)? = null,
    )
}
