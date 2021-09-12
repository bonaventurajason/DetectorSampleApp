package com.bonaventurajason.detectorsampletest.utils

import android.Manifest
import android.content.Context
import pub.devrel.easypermissions.EasyPermissions

object CameraUtility {
    fun hasCameraPermission(context: Context) =
        EasyPermissions.hasPermissions(
            context,
            Manifest.permission.CAMERA
        )
}