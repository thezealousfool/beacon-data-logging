package com.vivekroy.navcoglogging

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class ActivityResult(
    val resultCode: Int,
    val data: Intent?)

class PermissionResult(
    val permissions: Array<out String>,
    val grantResults: IntArray
)

abstract class AsyncActivity: AppCompatActivity() {

    private var activityCurrentCode : Int = 0
    private var permissionCurrentCode : Int = 0
    private val activityResultByCode = mutableMapOf<Int, CompletableDeferred<ActivityResult?>>()
    private val permissionResultByCode = mutableMapOf<Int, CompletableDeferred<PermissionResult>>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityResultByCode[requestCode]
            ?.complete(ActivityResult(resultCode, data))
            ?: super.onActivityResult(requestCode, resultCode, data)
        activityResultByCode.remove(requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionResultByCode[requestCode]
            ?.complete(PermissionResult(permissions, grantResults))
            ?: super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultByCode.remove(requestCode)
    }

    fun launchIntentAsync(intent: Intent): Deferred<ActivityResult?>
    {
        val activityResult = CompletableDeferred<ActivityResult?>()

        if (intent.resolveActivity(packageManager) != null) {
            val resultCode = activityCurrentCode++
            activityResultByCode[resultCode] = activityResult
            startActivityForResult(intent, resultCode)
        } else activityResult.complete(null)
        return activityResult
    }

    fun requestPermissionAsync(permissions: Array<out String>): Deferred<PermissionResult> {
        val result = CompletableDeferred<PermissionResult>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
                result.complete(PermissionResult(permissions,
                    IntArray(permissions.size) { PackageManager.PERMISSION_GRANTED }))
            } else {
                val resultCode = permissionCurrentCode++
                permissionResultByCode[resultCode] = result
                requestPermissions(permissions, resultCode)
            }
        } else result.complete(PermissionResult(permissions,
                IntArray(permissions.size) { PackageManager.PERMISSION_GRANTED }))
        return result
    }
}