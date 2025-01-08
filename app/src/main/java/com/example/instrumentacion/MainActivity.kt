package com.example.instrumentacion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.InputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var humidityText: TextView
    private lateinit var bluetoothToggle: ToggleButton

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var isConnected = false

    private val deviceAddress = "98:D3:31:F5:A3:00" // Dirección MAC del módulo HC-05
    private val myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        progressBar = findViewById(R.id.progress_circular)
        humidityText = findViewById(R.id.humidity_text)
        bluetoothToggle = findViewById(R.id.bluetooth_toggle)

        // Configurar el adaptador Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        checkPermissions()

        // Manejo del botón para conectar/desconectar Bluetooth
        bluetoothToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                connectToBluetooth()
            } else {
                disconnectBluetooth()
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun connectToBluetooth() {
        bluetoothAdapter?.let { adapter ->
            if (!adapter.isEnabled) {
                showToast("Por favor, activa Bluetooth.")
                return
            }

            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID)
                bluetoothSocket?.connect()
                isConnected = true
                showToast("Conexión Bluetooth establecida")
                readBluetoothData()
            } catch (e: Exception) {
                e.printStackTrace()
                isConnected = false
                showToast("No se pudo conectar al dispositivo Bluetooth")
            }
        } ?: showToast("Adaptador Bluetooth no disponible")
    }

    private fun disconnectBluetooth() {
        try {
            bluetoothSocket?.close()
            isConnected = false
            showToast("Conexión Bluetooth cerrada")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readBluetoothData() {
        if (!isConnected) return

        val inputStream: InputStream? = bluetoothSocket?.inputStream
        val buffer = ByteArray(1024)

        Thread {
            while (isConnected) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: 0
                    if (bytesRead > 0) {
                        val receivedData = String(buffer, 0, bytesRead).trim()
                        processReceivedData(receivedData)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }.start()
    }

    private fun processReceivedData(data: String) {
        try {
            // Intentar convertir el dato recibido a un número flotante
            val humidityValue = data.toFloat()

            // Actualizar la UI en el hilo principal
            runOnUiThread {
                humidityText.text = "$humidityValue%"
                progressBar.progress = humidityValue.toInt()
            }
        } catch (e: NumberFormatException) {
            // Si el dato no es un número válido, ignorarlo
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                showToast("Permisos necesarios no otorgados")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectBluetooth()
    }
}

