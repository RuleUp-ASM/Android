package com.ruleup.shared.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShowToast(): (String) -> Unit {
    val appContext = LocalContext.current.applicationContext
    return { message ->
        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
    }
}
