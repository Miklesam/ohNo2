package com.ardian.erclinkbt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ardian.erclinkbt.extension.Toast
import com.ardian.erclinkbt.extension.hasPermission
import com.ardian.erclinkbt.extension.invisible
import com.ardian.erclinkbt.extension.visible
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private var listener : DevicesAdapter.RecyclerViewClickListener = object : DevicesAdapter.RecyclerViewClickListener {

        @SuppressLint("MissingPermission")
        override fun onClick(v: View, position: Int) {
            if (bluetoothAdapter.isEnabled){
                try{
                    val clientClass = ClientClass(deviceListAdapter.listBluetooth[position])
                    clientClass.start()

                    txt_status.text = "Connected"

                    whatSide = CLIENT

                }catch (e : IOException){
                    e.printStackTrace()
                }
            }else {
                // Bluetooth isn't enabled - prompt user to turn it on
                val intent1 = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intent1, ENABLE_BLUETOOTH)
            }
        }
    }

    private var sendReceive : SendReceive? = null
    private var isServer = 0
    private var currentLocation = Location("My")

    companion object {

        const val APP_NAME = "BtChat"
        val MY_UUID : UUID = UUID.fromString("69cf5823-e045-4a1f-bf93-365e4bf9ebef")
        const val ENABLE_BLUETOOTH = 1
        const val REQUEST_ENABLE_DISCOVERY = 2
        const val REQUEST_ACCESS_COARSE_LOCATION = 3

        const val STATE_LISTENING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTION_FAILED = 4
        const val STATE_MESSAGE_RECEIVED = 5

        var whatSide = 0
        const val SERVER = 1
        const val CLIENT = 2
    }



    private var bluetoothDiscoveryResult = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice = p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val Rssi = p1.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                val myDevice = MyBluetoothDevice(device, Rssi)
                if(device.name!= null)
                deviceListAdapter.addDevices(myDevice)
            }
        }
    }

    private var bluetoothDiscoveryMonitor = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    progress_bar.visible()
                    Toast("Scan Started..")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    progress_bar.invisible()
                    Toast("Scan Completed. Found ${deviceListAdapter.itemCount} devices.")
                }
            }
        }
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val deviceListAdapter = DevicesAdapter(listener)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUi()

        btn_listen.setOnClickListener{
            try{
                val serverClass = ServerClass()
                serverClass.start()

                mMessageView.appendSystemMessage(getString(R.string.msg_server_started))
                whatSide = SERVER
            } catch (e : IOException){
                e.printStackTrace()
            }
        }
        sendClickListener()

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val locationListener: LocationListener = MyLocationListener(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 5000, 10f, locationListener
        )
    }

    private fun sendClickListener(){
        btn_send.setOnClickListener{sendReceiveListener()}
    }

    private fun sendReceiveListener(){
        var string = ed_msg.text.toString()
        sendReceive!!.write(string.toByteArray())
        if (whatSide == SERVER){
            mMessageView.appendServerMessage(string)
        }else{
            mMessageView.appendClientMessage(string)
        }
    }


    @SuppressLint("HandlerLeak")
    val handler = object : Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                STATE_LISTENING -> txt_status.text = "Listening"

                STATE_CONNECTING -> txt_status.text = "Connecting"

                STATE_CONNECTED -> txt_status.text = "Connected"

                STATE_CONNECTION_FAILED -> txt_status.text = "Connection Failed"

                STATE_MESSAGE_RECEIVED ->{
                    val readBuff : ByteArray = msg.obj as ByteArray
                    var tempMsg = String(readBuff, 0, msg.arg1)
//                    txt_msg.text = tempMsg
                    if (whatSide == SERVER){

                        val coords = tempMsg.split("\n")
                        val loc = Location("")
                        loc.latitude = coords[0].toDouble()
                        loc.longitude = coords[1].toDouble()
                        val distance = currentLocation.distanceTo(loc)

                        mMessageView.appendClientMessage(distance.toString())
                    }else{
                        mMessageView.appendServerMessage(tempMsg)
                    }
                }
            }

        }
    }


    private fun initUi() {
        listview.adapter = deviceListAdapter
        listview.layoutManager = LinearLayoutManager(this)
        listview.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        btn_show_devices.setOnClickListener{ initBluetooth()}
    }


    @SuppressLint("MissingPermission")
    private fun initBluetooth() {
        if (bluetoothAdapter.isDiscovering) return

        if (bluetoothAdapter.isEnabled){
            enableDiscovery()
        }else {
            // Bluetooth isn't enabled - prompt user to turn it on
            val intent1 = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent1, ENABLE_BLUETOOTH)
        }

    }

    @SuppressLint("MissingPermission")
    private fun enableDiscovery() {
        val intent2 = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        startActivityForResult(intent2, REQUEST_ENABLE_DISCOVERY)
    }

    private fun monitorDiscovery(){
        registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery(){
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isDiscovering) {
                beginDiscovery()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ACCESS_COARSE_LOCATION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun beginDiscovery() {
        registerReceiver(bluetoothDiscoveryResult, IntentFilter(BluetoothDevice.ACTION_FOUND))
        deviceListAdapter.clearDevices()
        monitorDiscovery()
        bluetoothAdapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private inner class ServerClass : Thread(){
        private lateinit var serverSocket : BluetoothServerSocket

        init {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
            } catch (e : IOException){
                e.printStackTrace()
            }
        }

        override fun run() {
            var socket : BluetoothSocket? = null

            while(socket == null){
                try {
                    var message = Message.obtain()
                    message.what = STATE_LISTENING
                    handler.sendMessage(message)

                    socket = serverSocket.accept()
                } catch (e : IOException){
                    e.printStackTrace()
                    var message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }

                if (socket != null){
                    var message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)

                    sendReceive = SendReceive(socket)
                    sendReceive!!.start()
                }
            }
        }

        override fun destroy() {
            mMessageView.appendSystemMessage(getString(R.string.msg_client_disconnected))
        }

    }

    @SuppressLint("MissingPermission")
    private inner class ClientClass(val device1 : MyBluetoothDevice) : Thread(){
        private var device : MyBluetoothDevice
        private lateinit var socket : BluetoothSocket

        init {
            device = device1
            try {
                socket = device.device.createRfcommSocketToServiceRecord(MY_UUID)
            }catch (e : IOException){
                e.printStackTrace()
            }
        }

        override fun run() {
            try {
                socket.connect()
                var message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)

                sendReceive = SendReceive(socket)
                sendReceive!!.start()
                runOnUiThread{
                    mMessageView.appendSystemMessage(getString(R.string.msg_client_connected))
                }
            }catch (e : IOException){
                e.printStackTrace()
                var message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }

        override fun destroy() {
            mMessageView.appendSystemMessage(getString(R.string.msg_client_disconnected))
        }
    }

    private inner class SendReceive(val socket : BluetoothSocket) : Thread(){
        private var bluetoothSocket : BluetoothSocket
        private var inputStream : InputStream
        private var outputStream : OutputStream

        init {
            bluetoothSocket = socket
            var tempIn : InputStream? = null
            var tempOut : OutputStream? = null

            try {
                tempIn = bluetoothSocket.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e : IOException){
                e.printStackTrace()
            }
            inputStream = tempIn!!
            outputStream = tempOut!!
        }

        override fun run() {
            var buffer = ByteArray(1024)
            var bytes : Int

            while (true){
                try {
                    bytes = inputStream.read(buffer)
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget()
                }catch (e : IOException){
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes : ByteArray){
            try {
                outputStream.write(bytes)
            }catch (e:IOException){
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ACCESS_COARSE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    beginDiscovery()
                } else {
                    Toast("Permission required to scan for devices.")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH -> if (resultCode == Activity.RESULT_OK) {
                enableDiscovery()
            }
            REQUEST_ENABLE_DISCOVERY -> if (resultCode == Activity.RESULT_CANCELED) {
                Toast("Discovery cancelled.")
            } else {
                startDiscovery()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothDiscoveryMonitor)
        unregisterReceiver(bluetoothDiscoveryResult)
    }

    private inner class MyLocationListener(val context: Context) : LocationListener {
        override fun onLocationChanged(loc: Location) {
            currentLocation = loc
            Toast.makeText(
                context,
                "Location changed: Lat: " + loc.getLatitude().toString() + " Lng: "
                        + loc.getLongitude(), Toast.LENGTH_SHORT
            ).show()
            val longitude = loc.getLongitude()
            val latitude = loc.getLatitude()
            val s = """
               $longitude
               $latitude
               """.trimIndent()
            if(sendReceive!=null){
                sendReceive!!.write(s.toByteArray())
                if (whatSide == SERVER){
                    mMessageView.appendServerMessage(s)
                }else{
                    mMessageView.appendClientMessage(s)
                }
            }
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }
}