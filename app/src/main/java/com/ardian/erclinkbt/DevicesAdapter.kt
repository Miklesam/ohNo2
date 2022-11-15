package com.ardian.erclinkbt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ardian.erclinkbt.extension.inflate
import kotlinx.android.synthetic.main.adapter_discovered_devices.view.*
import kotlin.coroutines.coroutineContext


/**
 * Created by Ardian Iqbal Yusmartito on 13/10/22
 * Github : https://github.com/ALU-syntax
 * Twitter : https://twitter.com/mengkerebe
 * Instagram : https://www.instagram.com/ardian_iqbal_
 * LinkedIn : https://www.linkedin.com/in/ardianiqbal
 */
class DevicesAdapter(
    val listener : RecyclerViewClickListener) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>(){

    val listBluetooth = ArrayList<MyBluetoothDevice>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        parent.inflate(R.layout.adapter_discovered_devices)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listBluetooth[position])
    }

    override fun getItemCount(): Int = listBluetooth.size

    interface RecyclerViewClickListener{
        fun onClick(v : View, position: Int)
    }

    fun addDevices(device: MyBluetoothDevice){
        listBluetooth.add(device)
        notifyItemInserted(itemCount)
    }

    fun clearDevices(){
        listBluetooth.clear()
        notifyDataSetChanged()
    }

    inner class ViewHolder (private val view : View): RecyclerView.ViewHolder(view), View.OnClickListener{

        @SuppressLint("MissingPermission")
        fun bind(device : MyBluetoothDevice){
            view.text_view_device_name.text = device.device.name ?: device.device.address
            view.text_view_rssi.text = device.rssi.toString()
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            listener.onClick(p0!!, adapterPosition)
        }
    }

}

data class MyBluetoothDevice(val device: BluetoothDevice, val rssi:Short)