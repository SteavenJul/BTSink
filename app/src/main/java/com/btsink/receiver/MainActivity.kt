package com.btsink.receiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var deviceText: TextView
    private var isRunning = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED
                    )
                    updateConnectionStatus(state)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        deviceText = findViewById(R.id.deviceText)
        toggleButton.setOnClickListener {
            if (!isRunning) startSink() else stopSink()
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        registerReceiver(btReceiver, filter)
        checkPermissionsAndInit()
    }

    private fun checkPermissionsAndInit() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN))
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            if (!hasPermission(Manifest.permission.BLUETOOTH))
                permissions.add(Manifest.permission.BLUETOOTH)
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADMIN))
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            initBluetooth()
        }
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initBluetooth()
        } else {
            statusText.text = "Permissions denied. Please grant Bluetooth permissions."
        }
    }

    private fun initBluetooth() {
        if (bluetoothAdapter == null) {
            statusText.text = "Bluetooth not supported."
            toggleButton.isEnabled = false
            return
        }
        if (bluetoothAdapter?.isEnabled == false) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 101)
        } else {
            statusText.text = "Ready - tap Start to activate BT Sink"
        }
    }

    private fun startSink() {
        val serviceIntent = Intent(this, BluetoothSinkService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        isRunning = true
        toggleButton.text = "Stop"
        statusText.text = "Sink active - pair your Poco X7 Pro now"
        deviceText.text = "Waiting for connection..."
    }

    private fun stopSink() {
        stopService(Intent(this, BluetoothSinkService::class.java))
        isRunning = false
        toggleButton.text = "Start"
        statusText.text = "Stopped"
        deviceText.text = ""
    }

    private fun updateConnectionStatus(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_CONNECTED -> {
                statusText.text = "Connected! Audio playing to earbuds"
                deviceText.text = "Poco X7 Pro -> Oppo A52 -> Earbuds"
            }
            BluetoothAdapter.STATE_DISCONNECTED -> {
                if (isRunning) {
                    statusText.text = "Sink active - waiting for connection..."
                    deviceText.text = "Waiting for connection..."
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btReceiver)
    }
}
