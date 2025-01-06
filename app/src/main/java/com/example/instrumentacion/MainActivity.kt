package com.example.instrumentacion // Asegúrate de que coincida con tu paquete

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

    private val deviceAddress = "XX:XX:XX:XX:XX:XX" // Dirección MAC del módulo HC-05
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

    // Verificar permisos
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

    // Comprobar si un permiso está otorgado
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Conexión Bluetooth
    @SuppressLint("MissingPermission")
    private fun connectToBluetooth() {
        // Verificar si el adaptador Bluetooth está habilitado
        bluetoothAdapter?.let { adapter ->
            if (!adapter.isEnabled) {
                showToast("Por favor, activa Bluetooth.")
                return
            }

            // Verificar permisos para Android 12 o superior
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                        1
                    )
                    return
                }
            } else { // Para versiones anteriores a Android 12
                if (!hasPermission(Manifest.permission.BLUETOOTH) || !hasPermission(Manifest.permission.BLUETOOTH_ADMIN)) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                        1
                    )
                    return
                }
            }

            // Intentar conexión Bluetooth si los permisos han sido otorgados
            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                    ?: throw Exception("Dispositivo no encontrado")

                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID)
                bluetoothSocket?.connect()
                isConnected = true

                // Leer datos del Bluetooth
                readBluetoothData()
            } catch (e: Exception) {
                e.printStackTrace()
                isConnected = false
                showToast("No se pudo conectar al dispositivo Bluetooth")
            }
        } ?: showToast("Adaptador Bluetooth no disponible")
    }

    // Desconectar Bluetooth
    private fun disconnectBluetooth() {
        try {
            bluetoothSocket?.close()
            isConnected = false
            showToast("Conexión Bluetooth cerrada")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Leer datos del módulo Bluetooth
    private fun readBluetoothData() {
        if (!isConnected) return

        val inputStream: InputStream? = bluetoothSocket?.inputStream
        val buffer = ByteArray(1024)

        Thread {
            while (isConnected) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: 0
                    if (bytesRead > 0) {
                        val receivedData = String(buffer, 0, bytesRead)
                        val humidityValue = extractHumidity(receivedData)

                        // Actualizar la UI en el hilo principal
                        runOnUiThread {
                            humidityText.text = "$humidityValue%"
                            progressBar.progress = humidityValue.toInt()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }.start()
    }

    // Extraer el valor de humedad del mensaje recibido
    private fun extractHumidity(data: String): Float {
        return data.substringAfter("Humedad: ").substringBefore("%").toFloatOrNull() ?: 0.0f
    }

    // Manejar la respuesta de los permisos
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

    // Mostrar un mensaje al usuario
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Cerrar conexión al destruir la actividad
    override fun onDestroy() {
        super.onDestroy()
        disconnectBluetooth()
    }
}


