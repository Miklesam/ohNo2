package com.ardian.erclinkbt.extension

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


/**
 * Created by Ardian Iqbal Yusmartito on 13/10/22
 * Github : https://github.com/ALU-syntax
 * Twitter : https://twitter.com/mengkerebe
 * Instagram : https://www.instagram.com/ardian_iqbal_
 * LinkedIn : https://www.linkedin.com/in/ardianiqbal
 */
fun ViewGroup.inflate(layoutRes: Int): View = LayoutInflater.from(context).inflate(
    layoutRes,
    this,
    false
)