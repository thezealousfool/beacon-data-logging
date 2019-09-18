package com.vivekroy.navcoglogging

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AsyncActivity() {

    private val logTag = "MA:Vvk"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothSupported = bluetoothAdapter != null
    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!bluetoothSupported) {
            Log.e(logTag, "Device does not support bluetooth")
        }
        dialog = locationAlert()
    }

    override fun onResume() {
        Log.d(logTag, "onResume")
        super.onResume()
        (applicationContext as BeaconLoggingApplication).setMainActivity(this)
        CoroutineScope(IO).launch {
            if (!requestBluetoothOn()) Log.e(logTag, "Bluetooth not turned on")
            if (!requestLocationPermission()) Log.e(logTag, "Location permission not granted")
            if (!requestLocationOn()) Log.e(logTag, "Location not turned on")
        }
    }

    override fun onPause() {
        Log.d(logTag, "onPause")
        super.onPause()
        (applicationContext as BeaconLoggingApplication).setMainActivity(null)
    }

    private fun locationAlert(): AlertDialog {
        return AlertDialog.Builder(this)
        .setMessage("Location needs to be turned on")
        .setPositiveButton("OK") { _, _ ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }.setNegativeButton("Cancel") { _, _ ->
            Log.e(logTag, "Location not turned on")
        }.create()
    }

    private suspend fun requestBluetoothOn(): Boolean {
        if (! bluetoothAdapter.isEnabled) {
            val result = launchIntentAsync(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return result.await()?.resultCode?:Activity.RESULT_CANCELED == Activity.RESULT_OK
        }
        return true
    }

    private suspend fun requestLocationPermission(): Boolean {
        val grantResults = requestPermissionAsync(Array(1) {
            android.Manifest.permission.ACCESS_COARSE_LOCATION })
            .await().grantResults
        return if (grantResults.isEmpty()) false
        else grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun requestLocationOn(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (locationManager.getProviders(true).size > 0)
            true
        else {
            withContext(Main) { dialog.show() }
            false
        }
    }

    fun buttonClicked(view: View) {
        val application = this.applicationContext as BeaconLoggingApplication
        if (application.beaconManager.rangingNotifiers.isNotEmpty()) {
            application.disableMonitoring()
            button.text = "Start"
        } else {
            button.text = "Stop"
            application.enableMonitoring()
        }
    }
}
