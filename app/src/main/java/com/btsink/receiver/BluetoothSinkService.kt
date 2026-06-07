package com.btsink.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BluetoothSinkService : Service() {
    companion object {
        private const val TAG = "BTSinkService"
        private const val CHANNEL_ID = "bt_sink_channel"
        private const val NOTIF_ID = 1
        private const val A2DP_SINK_PROFILE = 11
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var audioManager: AudioManager? = null

    private val a2dpSinkListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            Log.d(TAG, "A2DP Sink connected")
            routeAudioToWiredOutput()
        }
        override fun onServiceDisconnected(profile: Int) {
            Log.d(TAG, "A2DP Sink disconnected")
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        enableA2dpSink()
        return START_STICKY
    }

    private fun enableA2dpSink() {
        try {
            val method = BluetoothAdapter::class.java.getMethod(
                "getProfileProxy",
                Context::class.java,
                BluetoothProfile.ServiceListener::class.java,
                Int::class.javaPrimitiveType
            )
            method.invoke(bluetoothAdapter, this, a2dpSinkListener, A2DP_SINK_PROFILE)
            Log.d(TAG, "A2DP Sink requested via reflection")
        } catch (e: Exception) {
            Log.e(TAG, "Reflection failed: ${e.message}")
            routeAudioToWiredOutput()
        }
    }

    private fun routeAudioToWiredOutput() {
        audioManager?.apply {
            isSpeakerphoneOn = false
            isBluetoothScoOn = false
            mode = AudioManager.MODE_NORMAL
        }
        Log.d(TAG, "Audio routed to wired output")
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Bluetooth Sink", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BT Sink Active")
            .setContentText("Receiving audio from Poco X7 Pro")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager?.mode = AudioManager.MODE_NORMAL
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
