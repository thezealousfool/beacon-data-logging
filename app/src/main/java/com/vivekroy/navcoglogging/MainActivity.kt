package com.vivekroy.navcoglogging

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.altbeacon.beacon.*


class MainActivity : AsyncActivity(), BeaconConsumer {

    private val logTag = "Vivek::"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var beaconManager: BeaconManager

    private val bluetoothSupported = bluetoothAdapter != null
    private lateinit var dialog: AlertDialog
    private lateinit var notifier: RangeNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!bluetoothSupported) {
            Log.e(logTag, "Device does not support bluetooth")
        }
        dialog = locationAlert()
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"))
        notifier = getNotifier()
    }

    override fun onResume() {
        super.onResume()
        Log.d(logTag, "onResume")
        CoroutineScope(IO).launch {
            if (!requestBluetoothOn()) Log.e(logTag, "Bluetooth not turned on")
            if (!requestLocationPermission()) Log.e(logTag, "Location permission not granted")
            if (!requestLocationOn()) Log.e(logTag, "Location not turned on")
            beaconManager.bind(this@MainActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(logTag, "onPause")
        beaconManager.removeRangeNotifier(notifier)
        beaconManager.unbind(this)
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
        Log.d(logTag, locationManager.getProviders(true).toString())
        return if (locationManager.getProviders(true).size > 0)
            true
        else {
            withContext(Main) { dialog.show() }
            false
        }
    }

    private fun getNotifier(): RangeNotifier {
        return RangeNotifier { beacons, _ ->
            if (beacons.isNotEmpty())
                beacons.forEach { beacon -> Log.d(logTag, beacon.toString()) }
            else
                Log.d(logTag, "No beacons found")
        }
    }

    override fun onBeaconServiceConnect() {
        Log.d(logTag, "onBeaconServiceConnect")
        val region = Region(BuildConfig.APPLICATION_ID, null, null, null)
        try {
            beaconManager.addRangeNotifier(notifier)
            beaconManager.startRangingBeaconsInRegion(region)
            Log.d(logTag, "Notifier connected")
        } catch (e: RemoteException) {
            Log.e(logTag, e.message?:"No message")
        }
    }
}
