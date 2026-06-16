package com.ruleup.domain.helper

import com.ruleup.domain.message.IconType
import com.ruleup.domain.message.MessageEffect
import kotlinx.coroutines.flow.Flow

interface MessageHelper {
    val effect: Flow<MessageEffect>

    fun showToast(toastMsg: String)

    fun showSnackBar(
        iconType: IconType = IconType.SUCCESS,
        messageText: String,
        callToActionText: String? = null,
        onClickCTA: (() -> Unit)? = null,
    )

    fun showSnackBar(
        iconType: IconType = IconType.SUCCESS,
        messageRes: Int,
        callToActionText: String? = null,
        onClickCTA: (() -> Unit)? = null,
    )

    fun showOneButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        buttonText: String = "Ok",
        onClickButton: (() -> Unit)? = null,
    )

    fun showTwoButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        leftButtonText: String = "Cancel",
        onClickLeftButton: (() -> Unit)? = null,
        rightButtonText: String = "Ok",
        onClickRightButton: (() -> Unit)? = null,
    )
}
