package com.ruleup.challenge.presentation.create.component

import kotlin.test.Test
import kotlin.test.assertEquals

/** Android 11 이상 백그라운드 위치 권한의 별도 요청. */
class PermissionRequestBatchesTest {
    private val fine = "android.permission.ACCESS_FINE_LOCATION"
    private val background = "android.permission.ACCESS_BACKGROUND_LOCATION"

    @Test
    fun `백그라운드 위치는 전경 권한 뒤에 따로 요청한다`() {
        assertEquals(listOf(listOf(fine), listOf(background)), permissionRequestBatches(listOf(fine, background)))
    }

    @Test
    fun `백그라운드 위치가 없으면 한 번에 요청한다`() {
        assertEquals(listOf(listOf(fine)), permissionRequestBatches(listOf(fine)))
    }

    @Test
    fun `백그라운드 위치만 남았으면 그것만 요청한다`() {
        assertEquals(listOf(listOf(background)), permissionRequestBatches(listOf(background)))
    }
}
