# 💧 Monitor de Humedad en Tiempo Real - Android & Arduino

Este proyecto consiste en una solución integral para el monitoreo de humedad, utilizando sensores de hardware y una aplicación móvil nativa para la visualización de datos en tiempo real.

## 🛠️ Tecnologías y Componentes
* **App Móvil:** Kotlin | Android Studio.
* **Hardware:** Arduino.
* **Comunicación:** Bluetooth.
* **Visualización:** Custom Views para gráficas circulares.

## ⚙️ Funcionamiento
1.  **Captura:** El **Arduino** recibe señales analógicas del sensor de humedad de suelo/ambiente.
2.  **Transmisión:** Los datos se envían a la aplicación móvil.
3.  **Procesamiento:** La app desarrollada en **Kotlin** traduce las señales crudas a valores porcentuales (0% - 100%).
4.  **Visualización:** Se muestra la información mediante una **gráfica circular (Gauge)** dinámica que cambia según el nivel de humedad detectado.

---

## 📸 Demostración Visual

### Interfaz de la Aplicación
![sensor de humedad](https://github.com/user-attachments/assets/510d32e8-1d52-4192-8768-a5e52247f267)
