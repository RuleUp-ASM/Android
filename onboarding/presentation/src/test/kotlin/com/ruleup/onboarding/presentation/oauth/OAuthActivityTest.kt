package com.ruleup.onboarding.presentation.oauth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OAuthActivityTest {
    @Test
    fun `재생성된 구글 인증을 취소하면 취소 결과를 돌려준다`() {
        assertRestoredFailure("구글 인증이 취소되었습니다.") { null }
    }

    @Test
    fun `재생성된 구글 인증에 코드가 없으면 성공으로 처리하지 않는다`() {
        assertRestoredFailure("구글 인가 코드를 받지 못했습니다.") { request ->
            AuthorizationResponse.Builder(request).build().toIntent()
        }
    }

    @Test
    fun `재생성된 구글 인증 화면은 기존 요청의 코드 검증값으로 결과를 반환한다`() {
        val config =
            AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://oauth2.googleapis.com/token"),
            )
        val request =
            AuthorizationRequest
                .Builder(config, "test-client", ResponseTypeValues.CODE, Uri.parse("com.example.test:/oauth"))
                .setScope("openid email profile")
                .build()
        val state = Bundle().apply { putString("google_authorization_request", request.jsonSerializeString()) }
        val intent = Intent().putExtra(OAuthActivity.EXTRA_PROVIDER, "GOOGLE")
        val controller = Robolectric.buildActivity(OAuthActivity::class.java, intent).create(state)
        val activity = controller.get()
        val registry = activity.activityResultRegistry
        val registryState = Bundle()
        registry.onSaveInstanceState(registryState)
        val key = requireNotNull(registryState.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS")).single()
        val requestCode = requireNotNull(registryState.getIntegerArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_RCS")).single()
        registryState.putStringArrayList("KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS", arrayListOf(key))
        registry.onRestoreInstanceState(registryState)

        val authResult =
            AuthorizationResponse
                .Builder(request)
                .setAuthorizationCode("authorization-code")
                .build()
                .toIntent()
        assertTrue(registry.dispatchResult(requestCode, Activity.RESULT_OK, authResult))
        controller.start().resume()
        shadowOf(Looper.getMainLooper()).idle()

        val shadow = shadowOf(activity)
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertEquals("authorization-code", shadow.resultIntent.getStringExtra(OAuthActivity.EXTRA_CODE))
        assertEquals(request.codeVerifier, shadow.resultIntent.getStringExtra(OAuthActivity.EXTRA_VERIFIER))
        assertEquals(request.redirectUri.toString(), shadow.resultIntent.getStringExtra(OAuthActivity.EXTRA_REDIRECT))
        controller.pause().stop().destroy()
    }

    @Test
    fun `구글 인증 중 화면이 재생성되면 기존 요청을 기다리고 새 인증 화면은 열지 않는다`() {
        val config =
            AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://oauth2.googleapis.com/token"),
            )
        val request =
            AuthorizationRequest
                .Builder(config, "test-client", ResponseTypeValues.CODE, Uri.parse("com.example.test:/oauth"))
                .setScope("openid email profile")
                .build()
        val state = Bundle().apply { putString("google_authorization_request", request.jsonSerializeString()) }
        val intent = Intent().putExtra(OAuthActivity.EXTRA_PROVIDER, "GOOGLE")
        val controller =
            Robolectric
                .buildActivity(OAuthActivity::class.java, intent)
                .create(state)
                .start()
                .resume()
        shadowOf(Looper.getMainLooper()).idle()
        val activity = controller.get()

        assertFalse(activity.isFinishing)
        assertNull(shadowOf(activity).nextStartedActivity)
        val nextState = Bundle()
        controller.saveInstanceState(nextState)
        val savedRequest = AuthorizationRequest.jsonDeserialize(nextState.getString("google_authorization_request")!!)
        assertEquals(request.codeVerifier, savedRequest.codeVerifier)
        controller.pause().stop().destroy()
        assertFalse(activity.isFinishing)
    }

    private fun assertRestoredFailure(
        expectedError: String,
        resultData: (AuthorizationRequest) -> Intent?,
    ) {
        val config =
            AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://oauth2.googleapis.com/token"),
            )
        val request =
            AuthorizationRequest
                .Builder(config, "test-client", ResponseTypeValues.CODE, Uri.parse("com.example.test:/oauth"))
                .setScope("openid email profile")
                .build()
        val state = Bundle().apply { putString("google_authorization_request", request.jsonSerializeString()) }
        val controller =
            Robolectric
                .buildActivity(
                    OAuthActivity::class.java,
                    Intent().putExtra(OAuthActivity.EXTRA_PROVIDER, "GOOGLE"),
                ).create(state)
        val activity = controller.get()
        val registry = activity.activityResultRegistry
        val registryState = Bundle()
        registry.onSaveInstanceState(registryState)
        val key = requireNotNull(registryState.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS")).single()
        val requestCode = requireNotNull(registryState.getIntegerArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_RCS")).single()
        registryState.putStringArrayList("KEY_COMPONENT_ACTIVITY_LAUNCHED_KEYS", arrayListOf(key))
        registry.onRestoreInstanceState(registryState)

        assertTrue(registry.dispatchResult(requestCode, Activity.RESULT_CANCELED, resultData(request)))
        controller.start().resume()
        shadowOf(Looper.getMainLooper()).idle()

        val shadow = shadowOf(activity)
        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadow.resultCode)
        assertEquals(expectedError, shadow.resultIntent.getStringExtra(OAuthActivity.EXTRA_ERROR))
        assertNull(shadow.resultIntent.getStringExtra(OAuthActivity.EXTRA_CODE))
        controller.pause().stop().destroy()
    }
}
