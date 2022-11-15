package com.ardian.erclinkbt.extension

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat


/**
 * Created by Ardian Iqbal Yusmartito on 13/10/22
 * Github : https://github.com/ALU-syntax
 * Twitter : https://twitter.com/mengkerebe
 * Instagram : https://www.instagram.com/ardian_iqbal_
 * LinkedIn : https://www.linkedin.com/in/ardianiqbal
 */
fun Activity.hasPermission(permission : String)= ContextCompat.checkSelfPermission(
    this,
    permission) == PackageManager.PERMISSION_GRANTED

fun Activity.Toast(message : String){
    android.widget.Toast.makeText(this, message , android.widget.Toast.LENGTH_SHORT).show()
}