package com.ruleup.verification.data.signal.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ruleup.verification.domain.entity.NetworkState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * VPN 활성 여부 동기 게이트(전송 스펙 §6.1). VPN ON 이면 서버가 해당 위치 신호를 무효 처리한다
 * (페이크 GPS + VPN 우회를 싸게 거른다). `ACCESS_NETWORK_STATE`(자동 권한) 기반.
 */
class NetworkStateProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun current(): NetworkState = NetworkState(vpnActive = isVpnActive())

        private fun isVpnActive(): Boolean {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }
