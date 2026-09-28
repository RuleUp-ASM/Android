package com.ruleup.verification.data.signal.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ruleup.verification.domain.entity.NetworkState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** VPN 활성 여부 동기 게이트. */
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
