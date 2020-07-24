/*
 *  This file provided by Facebook is for non-commercial testing and evaluation
 *  purposes only.  Facebook reserves all rights not expressly granted.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree. An additional grant
 *  of patent rights can be found in the PATENTS file in the same directory.
 *
 */
package com.facebook.network.connectionclass.sample

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.AsyncTask
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.facebook.network.connectionclass.ConnectionClassManager
import com.facebook.network.connectionclass.ConnectionClassManager.ConnectionClassStateChangeListener
import com.facebook.network.connectionclass.ConnectionClassUtils
import com.facebook.network.connectionclass.ConnectionQuality
import com.facebook.network.connectionclass.DeviceBandwidthSampler
import java.io.IOException
import java.net.URL


class MainActivity : Activity() {
    private var mConnectionClassManager: ConnectionClassManager? = null
    private var mDeviceBandwidthSampler: DeviceBandwidthSampler? = null
    private var mListener: ConnectionChangedListener? = null
    private var mTextView: TextView? = null
    private var mTextViewType: TextView? = null
    private var mRunningBar: ProgressBar? = null

    private var mURL = "https://www.twnel.com/wp-content/uploads/2019/03/twnel-logo.png"
    private var mTries = 0
    private var mConnectionClass = ConnectionQuality.UNKNOWN
    private lateinit var receiver: NetworkReceiver
    var connectionService: ConnectivityManager? = null
    var telephonyManager: TelephonyManager? = null
    private var endpoints = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mConnectionClassManager = ConnectionClassManager.getInstance()
        mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance()
        findViewById<Button>(R.id.test_btn).setOnClickListener(testButtonClicked)
        mTextView = findViewById(R.id.connection_class)
        mTextViewType = findViewById(R.id.connection_type)
        mTextView?.text = mConnectionClassManager?.currentBandwidthQuality.toString()
        mRunningBar = findViewById(R.id.runningBar)
        mRunningBar?.visibility = View.GONE
        mListener = ConnectionChangedListener()
        endpoints.add("https://www.twnel.com/wp-content/uploads/2019/03/twnel-logo.png")
        endpoints.add("https://www.micolombiadigital.gov.co/sites/superadmin/content/files/000863/43115.jpg")
        endpoints.add("https://www.twnel.com/wp-content/uploads/2019/02/homeimage.png")
        endpoints.add("https://corozalsucre.micolombiadigital.gov.co/sites/corozalsucre/content/files/000063/3109_comunicado-de-prensa-92948_1024x600.jpeg")
        endpoints.add("https://corozalsucre.micolombiadigital.gov.co/sites/corozalsucre/content/files/000043/2107_chat-institucional_1024x600.jpeg")
        // Registers BroadcastReceiver to track network connection changes.
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        receiver = NetworkReceiver()
        this.registerReceiver(receiver, filter)
        connectionService = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager?.listen(TeleListener(), PhoneStateListener.LISTEN_DATA_CONNECTION_STATE)
        val networkInfo: NetworkInfo? = connectionService?.activeNetworkInfo

        if (networkInfo == null)
            mTextViewType?.text = "UNKNOWN"
        if (networkInfo?.type == ConnectivityManager.TYPE_WIFI) {
            mTextViewType?.text = "WIFI"
            Log.d("NetworkReceiver", "WIFI")
        } else {
            Log.d("NetworkReceiver", "MOBILE")
            mTextViewType?.text = "MOBILE"
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        // Unregisters BroadcastReceiver when app is destroyed.
        this.unregisterReceiver(receiver)
    }

    override fun onPause() {
        super.onPause()
        mConnectionClassManager?.remove(mListener)
    }

    override fun onResume() {
        super.onResume()
        mConnectionClassManager?.register(mListener)
    }

    /**
     * Listener to update the UI upon connectionclass change.
     */
    private inner class ConnectionChangedListener : ConnectionClassStateChangeListener {
        override fun onBandwidthStateChange(bandwidthState: ConnectionQuality) {
            mConnectionClass = bandwidthState
            /* runOnUiThread {
                 mTextView?.text = "$mConnectionClass |\nDownloadKBitsPerSecond: ${mConnectionClassManager?.downloadKBitsPerSecond}"
             }*/
        }
    }

    private val testButtonClicked = View.OnClickListener {
        endpoints.shuffle()
        mURL = endpoints.first()
        DownloadImage().execute(mURL)
    }

    /**
     * AsyncTask for handling downloading and making calls to the timer.
     */
    inner class DownloadImage : AsyncTask<String?, Void?, Void?>() {
        override fun onPreExecute() {
            mDeviceBandwidthSampler?.startSampling()
            mRunningBar?.visibility = View.VISIBLE
        }


        override fun onPostExecute(v: Void?) {
            mDeviceBandwidthSampler?.stopSampling()
            // Retry for up to 10 times until we find a ConnectionClass.
            if (!mDeviceBandwidthSampler!!.isSampling) {
                mRunningBar?.visibility = View.GONE
            }
            mConnectionClass = mConnectionClassManager?.currentBandwidthQuality!!
            mTextView?.text = "$mConnectionClass |\nDownloadKBitsPerSecond: ${mConnectionClassManager?.downloadKBitsPerSecond}"
        }

        override fun doInBackground(vararg params: String?): Void? {
            val imageURL = params[0]
            try {
                // Open a stream to download the image from our URL.
                val connection = URL(imageURL).openConnection()
                connection.useCaches = false
                connection.defaultUseCaches = false
                connection.connect()
                val input = connection.getInputStream()
                try {
                    val buffer = ByteArray(1024)

                    // Do some busy waiting while the stream is open.
                    while (input.read(buffer) != -1) {
                    }
                } finally {
                    input.close()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error while downloading image.")
            }
            return null
        }
    }


    inner class NetworkReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val netType = context?.let { ConnectionClassUtils.getCurrentNetWorkType(it) }
            if (netType == ConnectionClassUtils.NetworkType.WIFI) {
                Log.d("NetworkReceiver", "WIFI")
                mTextViewType?.text = "WIFI"
            } else {
                Log.d("NetworkReceiver", "MOBILE")
                mTextViewType?.text = "MOBILE"
            }

            ConnectionClassManager.getInstance().reset()
            endpoints.shuffle()
            mURL = endpoints.first()
            DownloadImage().execute(mURL)
        }
    }

    companion object {
        private const val TAG = "ConnectionClass-Sample"
    }

    inner class TeleListener : PhoneStateListener() {
        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
            Log.d("ConnectionStateChanged", "state $state, networkType $networkType")
            if (TelephonyManager.DATA_CONNECTED == state) {
                Log.d("DATA_NET_CONNECTED", "YES")
                ConnectionClassManager.getInstance().reset()
                endpoints.shuffle()
                mURL = endpoints.first()
                DownloadImage().execute(mURL)
            }
            if (TelephonyManager.DATA_DISCONNECTED == state) {
                Log.d("DATA_NET_DISCONNECTED", "YES")
                ConnectionClassManager.getInstance().reset()
            }
            if (TelephonyManager.DATA_CONNECTING == state)
                Log.d("DATA_NET_CONNECTING", "YES")
            if (TelephonyManager.DATA_SUSPENDED == state)
                Log.d("DATA_NET_SUSPENDED", "YES")
            if (TelephonyManager.DATA_UNKNOWN == state)
                Log.d("DATA_NET_UNKNOWN", "YES")
        }
    }
}