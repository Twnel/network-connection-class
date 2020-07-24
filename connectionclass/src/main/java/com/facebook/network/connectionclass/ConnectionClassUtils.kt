package com.facebook.network.connectionclass

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log

class ConnectionClassUtils {
    enum class NetworkType {
        MOBILE, WIFI, UNDEFINED
    }

    companion object {
        fun getCurrentNetWorkType(context: Context): NetworkType {
            val connectionService = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo = connectionService.activeNetworkInfo
                    ?: return NetworkType.UNDEFINED
            return if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                NetworkType.WIFI
            } else {
                NetworkType.MOBILE
            }
        }
    }
}