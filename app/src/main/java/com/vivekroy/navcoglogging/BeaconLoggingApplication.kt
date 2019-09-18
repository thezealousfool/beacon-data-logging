package com.vivekroy.navcoglogging

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import java.util.*

class BeaconLoggingApplication : Application(), BeaconConsumer {

    private val logTag = "BLA:Vvk"

    lateinit var beaconManager: BeaconManager
    private var mainActivity: MainActivity? = null

    private lateinit var db : AppDatabase

    override fun onCreate() {
        super.onCreate()
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"))
        val pendingIntent = PendingIntent.getActivity(this,0,
            Intent(this, this::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Scanning for Beacons")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "My Notification Channel ID",
                "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "My Notification Channel Description"
            val notificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notification.setChannelId(channel.id)
        }
        beaconManager.enableForegroundServiceScanning(notification.build(), 121)
        beaconManager.setEnableScheduledScanJobs(false)
        beaconManager.backgroundBetweenScanPeriod = 0
        beaconManager.backgroundScanPeriod = 1000

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "com.vivekroy.navcoglogging").build()
    }

    fun disableMonitoring() {
        beaconManager.unbind(this)
        beaconManager.removeAllRangeNotifiers()
        Log.d(logTag, "Disabling monitoring")
    }

    fun enableMonitoring() {
        Log.d(logTag, "Enabling monitoring")
        beaconManager.bind(this)
    }

    fun setMainActivity(activity: MainActivity?) {
        mainActivity = activity
    }

    override fun onBeaconServiceConnect() {
        val notifier = RangeNotifier { beacons, _ ->
            val timestamp = Date().time / 1000
            if (beacons.isNotEmpty())
                sqlLog(beacons, timestamp)
            else
                Log.d(logTag, "No beacons found")
        }
        Log.d(logTag, "onBeaconServiceConnect")
        val region = Region(BuildConfig.APPLICATION_ID, null, null, null)
        try {
            beaconManager.addRangeNotifier(notifier)
            beaconManager.startRangingBeaconsInRegion(region)
            Log.d(logTag, "Notifier connected")
        } catch (e: RemoteException) {
            Log.e(logTag, e.message ?: "No message")
        }
    }

    fun sqlLog(beacons: Collection<Beacon>, timeInSecs: Long) {
        CoroutineScope(IO).launch {
            beacons.forEach {beacon ->
                db.beaconDao().insertBeacons(
                    BeaconEntity(
                    beacon.id1.toString(),
                    beacon.id2.toString(),
                    beacon.id3.toString(),
                    beacon.rssi,
                    timeInSecs))
            }
        }
    }
}