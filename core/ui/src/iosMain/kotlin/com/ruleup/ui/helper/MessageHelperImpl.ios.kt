package com.ruleup.ui.helper

import com.ruleup.domain.helper.MessageHelper
import com.ruleup.domain.message.IconType
import com.ruleup.domain.message.MessageEffect
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * iOS 메시지 헬퍼. Android 의 [MessageHelperImpl](androidMain, Toast/Context 기반)에 대응하는 actual 바인딩.
 * Channel 기반 effect 방출은 동일하고, Android 리소스(Int) 의존만 없다.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class MessageHelperImpl : MessageHelper {
    private val _effect = Channel<MessageEffect>(Channel.BUFFERED)
    override val effect: Flow<MessageEffect> = _effect.receiveAsFlow()

    override fun showToast(toastMsg: String) {
        emit(MessageEffect.ShowToastMsg(toastMsg))
    }

    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        emit(MessageEffect.ShowSnackBarError(messageText))
    }

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        // iOS 엔 Android 리소스(Int)가 없으므로 식별자만 전달한다.
        emit(MessageEffect.ShowSnackBarError("message:$messageRes"))
    }

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        emit(
            MessageEffect.ShowOneButtonDialog(
                titleText = titleText,
                descText = descText,
                cantIgnore = cantIgnore,
                buttonText = buttonText,
                onClickButton = onClickButton,
            ),
        )
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) {
        // TBD (Android 구현과 동일하게 미구현)
    }

    private fun emit(messageEffect: MessageEffect) {
        val result = _effect.trySend(messageEffect)
        if (result.isFailure) println("MessageHelper dropped: $messageEffect")
    }
}
