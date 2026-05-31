package com.example

import android.hardware.Sensor

/**
 * Data classification for different modules of the System Monitor.
 */

data class BatteryInfo(
    val level: Int = 0,
    val status: String = "Unknown",
    val health: String = "Unknown",
    val voltageText: String = "0 mV",
    val temperatureCelsius: Float = 0.0f,
    val temperatureFahrenheit: Float = 32.0f,
    val technology: String = "Unknown",
    val powerSource: String = "Battery",
    val currentNowMicroAmperes: Long = 0L,  // Positive means charging, negative discharging
    val isCharging: Boolean = false
)

data class CpuInfo(
    val processorName: String = "Unknown Code",
    val coresCount: Int = 1,
    val supportedAbis: String = "Unknown",
    val architecture: String = "Unknown",
    val simulatedUsage: Float = 0.0f, // Active real-time load estimation
    val activeThreads: Int = 1,
    val uptimeText: String = "00:00:00"
)

data class MemoryInfo(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val lowMemoryThresholdBytes: Long = 0L,
    val isLowMemory: Boolean = false,
    val usagePercentage: Float = 0.0f
)

data class StorageInfo(
    val totalInternalBytes: Long = 0L,
    val availableInternalBytes: Long = 0L,
    val usedInternalBytes: Long = 0L,
    val internalUsagePercentage: Float = 0.0f,
    
    val totalExternalBytes: Long = 0L,
    val availableExternalBytes: Long = 0L,
    val usedExternalBytes: Long = 0L,
    val externalUsagePercentage: Float = 0.0f,
    val hasExternalStorage: Boolean = false
)

data class AppNetworkUsage(
    val appName: String,
    val packageName: String,
    val totalBytes: Long = 0L,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L
)

data class NetworkInfo(
    val type: String = "Disconnected",
    val wifiSsid: String = "N/A",
    val wifiLinkSpeedMbps: Int = 0,
    val isInternetAvailable: Boolean = false,
    val ipAddress: String = "0.0.0.0",
    val interfaceName: String = "None",
    val downloadSpeedBytesPerSec: Long = 0L,
    val uploadSpeedBytesPerSec: Long = 0L,
    val cumulativeWifiRxBytes: Long = 0L,
    val cumulativeWifiTxBytes: Long = 0L,
    val cumulativeMobileRxBytes: Long = 0L,
    val cumulativeMobileTxBytes: Long = 0L,
    val appNetworkUsageList: List<AppNetworkUsage> = emptyList()
)

data class OsInfo(
    val model: String = "Unknown Device",
    val manufacturer: String = "Unknown",
    val device: String = "Unknown",
    val board: String = "Unknown",
    val hardware: String = "Unknown",
    val androidVersion: String = "Unknown",
    val sdkInt: Int = 0,
    val buildId: String = "Unknown",
    val securityPatch: String = "Unknown",
    val bootloader: String = "Unknown",
    val fingerprint: String = "Unknown"
)

data class SensorItem(
    val id: Int,
    val name: String,
    val vendor: String,
    val type: Int,
    val stringType: String,
    val maximumRange: Float,
    val power: Float,
    val resolution: Float
)

data class ActiveSensorTelemetry(
    val sensorId: Int,
    val name: String,
    val values: FloatArray = floatArrayOf(0f, 0f, 0f),
    val timestamp: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ActiveSensorTelemetry
        if (sensorId != other.sensorId) return false
        if (name != other.name) return false
        if (!values.contentEquals(other.values)) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = sensorId
        result = 31 * result + name.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
