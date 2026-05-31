package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.TrafficStats
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class SystemMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val collector = SystemMonitorCollector(application)
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // Alert threshold configurations (Room)
    private val alertDb = AlertRoomDatabase.getDatabase(application)
    private val alertDao = alertDb.alertThresholdDao()
    private val alertRepository = AlertRepository(alertDao)

    private val _alertThresholds = MutableStateFlow<List<AlertThreshold>>(emptyList())
    val alertThresholds: StateFlow<List<AlertThreshold>> = _alertThresholds.asStateFlow()

    // Sliding speeds and historical references
    private var lastTotalRxBytes: Long = 0L
    private var lastTotalTxBytes: Long = 0L
    private var lastSpeedCalculationTime: Long = 0L
    private val lastTriggeredAlerts = mutableMapOf<String, Boolean>()

    // UI state flows
    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _cpuInfo = MutableStateFlow(CpuInfo())
    val cpuInfo: StateFlow<CpuInfo> = _cpuInfo.asStateFlow()

    private val _memoryInfo = MutableStateFlow(MemoryInfo())
    val memoryInfo: StateFlow<MemoryInfo> = _memoryInfo.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _networkInfo = MutableStateFlow(NetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    private val _osInfo = MutableStateFlow(OsInfo())
    val osInfo: StateFlow<OsInfo> = _osInfo.asStateFlow()

    private val _sensorsList = MutableStateFlow<List<SensorItem>>(emptyList())
    val sensorsList: StateFlow<List<SensorItem>> = _sensorsList.asStateFlow()

    private val _selectedSensor = MutableStateFlow<SensorItem?>(null)
    val selectedSensor: StateFlow<SensorItem?> = _selectedSensor.asStateFlow()

    private val _sensorTelemetry = MutableStateFlow<ActiveSensorTelemetry?>(null)
    val sensorTelemetry: StateFlow<ActiveSensorTelemetry?> = _sensorTelemetry.asStateFlow()

    // Sliding window of historical values for real-time Canvas charts
    private val _cpuHistory = MutableStateFlow<List<Float>>(List(25) { 0.25f })
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _memoryHistory = MutableStateFlow<List<Float>>(List(25) { 0.40f })
    val memoryHistory: StateFlow<List<Float>> = _memoryHistory.asStateFlow()

    private val _batteryHistory = MutableStateFlow<List<Float>>(List(25) { 0.50f })
    val batteryHistory: StateFlow<List<Float>> = _batteryHistory.asStateFlow()

    private var pollingJob: Job? = null
    private var activeSystemSensor: Sensor? = null

    // Register active sensor event listener
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event != null && event.sensor.type == activeSystemSensor?.type) {
                _sensorTelemetry.value = ActiveSensorTelemetry(
                    sensorId = activeSystemSensor?.type ?: 0,
                    name = activeSystemSensor?.name ?: "Unknown",
                    values = event.values.clone(),
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    init {
        // Load initial states
        refreshAllStaticData()
        startPolling()
        observeAlertThresholds()
    }

    private fun observeAlertThresholds() {
        viewModelScope.launch {
            try {
                // Populate defaults if none exist
                val existing = alertRepository.getAllThresholdsList()
                if (existing.isEmpty()) {
                    alertRepository.saveThreshold(AlertThreshold("battery_level", "Battery level", false, 20f, "LESS_THAN", "%"))
                    alertRepository.saveThreshold(AlertThreshold("cpu_temp", "CPU Temperature", false, 45f, "GREATER_THAN", "°C"))
                    alertRepository.saveThreshold(AlertThreshold("avail_ram", "Available RAM", false, 1.5f, "LESS_THAN", "GB"))
                }
            } catch (e: Exception) {
                // Ignore transient db lock during initial launch
            }

            alertRepository.allThresholds.collect { thresholds ->
                _alertThresholds.value = thresholds
            }
        }
    }

    fun saveAlertThreshold(threshold: AlertThreshold) {
        viewModelScope.launch {
            alertRepository.saveThreshold(threshold)
        }
    }

    private fun refreshAllStaticData() {
        _osInfo.value = collector.collectOsInfo()
        _sensorsList.value = collector.collectSensorsList()
        _storageInfo.value = collector.collectStorageInfo()
        _networkInfo.value = collector.collectNetworkInfo()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var lastCpuUsage = 0.30f
            while (isActive) {
                // Collect dynamic status
                val batt = collector.collectBatteryInfo()
                val mem = collector.collectMemoryInfo()
                val cpu = collector.collectCpuInfo(lastCpuUsage)
                lastCpuUsage = cpu.simulatedUsage
                val net = collector.collectNetworkInfo()
                val stor = collector.collectStorageInfo() // Refresh storage occasionally

                // Speed calculation
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val now = System.currentTimeMillis()
                val elapsedSecs = if (lastSpeedCalculationTime > 0L) (now - lastSpeedCalculationTime) / 1000f else 1f

                val downSpeed = if (lastTotalRxBytes > 0L && elapsedSecs > 0f) {
                    maxOf(0L, ((currentRx - lastTotalRxBytes) / elapsedSecs).toLong())
                } else 0L

                val upSpeed = if (lastTotalTxBytes > 0L && elapsedSecs > 0f) {
                    maxOf(0L, ((currentTx - lastTotalTxBytes) / elapsedSecs).toLong())
                } else 0L

                lastTotalRxBytes = currentRx
                lastTotalTxBytes = currentTx
                lastSpeedCalculationTime = now

                val finalNet = net.copy(
                    downloadSpeedBytesPerSec = downSpeed,
                    uploadSpeedBytesPerSec = upSpeed
                )

                _batteryInfo.value = batt
                _memoryInfo.value = mem
                _cpuInfo.value = cpu
                _networkInfo.value = finalNet
                _storageInfo.value = stor

                // Check customizable metric thresholds and fire notifications when crossed
                checkAlertConditions(batt, mem)

                // Update charts sliding windows
                appendCpuHistory(cpu.simulatedUsage)
                appendMemoryHistory(mem.usagePercentage)
                appendBatteryHistory(batt.level.toFloat() / 100f)

                // Poll interval of 1000ms
                delay(1000)
            }
        }
    }

    private fun checkAlertConditions(batt: BatteryInfo, mem: MemoryInfo) {
        val currentThresholds = _alertThresholds.value
        for (threshold in currentThresholds) {
            if (!threshold.isEnabled) continue

            var conditionMet = false
            var currentVal = 0f
            when (threshold.id) {
                "battery_level" -> {
                    currentVal = batt.level.toFloat()
                    conditionMet = if (threshold.comparisonType == "LESS_THAN") {
                        currentVal < threshold.thresholdValue
                    } else {
                        currentVal > threshold.thresholdValue
                    }
                }
                "cpu_temp" -> {
                    currentVal = batt.temperatureCelsius
                    conditionMet = if (threshold.comparisonType == "LESS_THAN") {
                        currentVal < threshold.thresholdValue
                    } else {
                        currentVal > threshold.thresholdValue
                    }
                }
                "avail_ram" -> {
                    val availGb = mem.availableRamBytes.toFloat() / (1024f * 1024f * 1024f)
                    currentVal = availGb
                    conditionMet = if (threshold.comparisonType == "LESS_THAN") {
                        currentVal < threshold.thresholdValue
                    } else {
                        currentVal > threshold.thresholdValue
                    }
                }
            }

            val wasTriggered = lastTriggeredAlerts[threshold.id] ?: false
            if (conditionMet) {
                if (!wasTriggered) {
                    lastTriggeredAlerts[threshold.id] = true
                    sendAlertNotification(
                        threshold.metricName,
                        "Warning: Crossed custom alert threshold of ${threshold.thresholdValue.toInt()}${threshold.unit} (Current: ${String.format(Locale.getDefault(), "%.1f", currentVal)}${threshold.unit})",
                        threshold.id.hashCode()
                    )
                }
            } else {
                lastTriggeredAlerts[threshold.id] = false
            }
        }
    }

    private fun sendAlertNotification(title: String, message: String, notificationId: Int) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "performance_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Performance Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Customizable system performance threshold exceeded warnings"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun appendCpuHistory(newValue: Float) {
        val currentList = _cpuHistory.value.toMutableList()
        if (currentList.size >= 30) {
            currentList.removeAt(0)
        }
        currentList.add(newValue)
        _cpuHistory.value = currentList
    }

    private fun appendMemoryHistory(newValue: Float) {
        val currentList = _memoryHistory.value.toMutableList()
        if (currentList.size >= 30) {
            currentList.removeAt(0)
        }
        currentList.add(newValue)
        _memoryHistory.value = currentList
    }

    private fun appendBatteryHistory(newValue: Float) {
        val currentList = _batteryHistory.value.toMutableList()
        if (currentList.size >= 30) {
            currentList.removeAt(0)
        }
        currentList.add(newValue)
        _batteryHistory.value = currentList
    }

    fun selectSensor(sensorItem: SensorItem) {
        // Unregister any old sensor first
        unregisterActiveSensor()

        _selectedSensor.value = sensorItem
        val sManager = sensorManager ?: return

        // Lookup specific matching hardware sensor type
        val matchingSensors = sManager.getSensorList(Sensor.TYPE_ALL)
        // Find by name accuracy or type
        val chosenSensor = matchingSensors.find { it.name == sensorItem.name && it.type == sensorItem.type }
        
        if (chosenSensor != null) {
            activeSystemSensor = chosenSensor
            // SENSOR_DELAY_UI is fast enough (approx 60,000 microsecond interval) and very battery-friendly
            sManager.registerListener(sensorEventListener, chosenSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Null telemetry if hardware is modeled but physically absent
            _sensorTelemetry.value = null
        }
    }

    fun clearSelectedSensor() {
        unregisterActiveSensor()
        _selectedSensor.value = null
        _sensorTelemetry.value = null
    }

    private fun unregisterActiveSensor() {
        sensorManager?.let { sManager ->
            sManager.unregisterListener(sensorEventListener)
        }
        activeSystemSensor = null
    }

    fun forceDiagnosticsRefresh() {
        refreshAllStaticData()
        // brief spike response for UX feel
        viewModelScope.launch {
            appendCpuHistory(0.92f)
            delay(120)
            appendCpuHistory(0.78f)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        unregisterActiveSensor()
    }
}
