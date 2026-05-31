package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import kotlin.math.max

class SystemMonitorCollector(private val context: Context) {

    /**
     * Gathers real-time Battery status details.
     */
    fun collectBatteryInfo(): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatusIntent = context.registerReceiver(null, intentFilter)

        var level = 0
        var status = "Unknown"
        var health = "Unknown"
        var voltageText = "0 mV"
        var temperatureCelsius = 0.0f
        var technology = "Unknown"
        var powerSource = "Battery"
        var isCharging = false

        batteryStatusIntent?.let { intent ->
            val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (rawLevel != -1 && scale != -1) {
                level = ((rawLevel.toFloat() / scale.toFloat()) * 100).toInt()
            }

            val statusRaw = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = statusRaw == BatteryManager.BATTERY_STATUS_CHARGING ||
                    statusRaw == BatteryManager.BATTERY_STATUS_FULL
            status = when (statusRaw) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                BatteryManager.BATTERY_STATUS_UNKNOWN -> "Unknown"
                else -> "Unknown"
            }

            health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                else -> "Unknown"
            }

            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            voltageText = "$voltage mV"

            // Temp is in tenths of a degree centigrade
            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            temperatureCelsius = tempRaw / 10.0f

            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

            powerSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Coil"
                else -> "Battery Source"
            }
        }

        // Get live micro-amperes if possible (supported on API 21+)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        var currentMicroAmps = 0L
        if (batteryManager != null) {
            currentMicroAmps = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            // On some devices, current now is returned as positive or negative differently.
            // Under normal charge pattern, positive means charging, negative discharging.
        }

        val tempFahrenheit = (temperatureCelsius * 9 / 5) + 32f

        return BatteryInfo(
            level = level,
            status = status,
            health = health,
            voltageText = voltageText,
            temperatureCelsius = temperatureCelsius,
            temperatureFahrenheit = tempFahrenheit,
            technology = technology,
            powerSource = powerSource,
            currentNowMicroAmperes = currentMicroAmps,
            isCharging = isCharging
        )
    }

    /**
     * Memory (RAM) Statistics querying.
     */
    fun collectMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val outInfo = ActivityManager.MemoryInfo()
        
        if (activityManager != null) {
            activityManager.getMemoryInfo(outInfo)
            val total = outInfo.totalMem
            val avail = outInfo.availMem
            val used = total - avail
            val pct = if (total > 0L) (used.toFloat() / total.toFloat()) else 0f
            
            return MemoryInfo(
                totalRamBytes = total,
                availableRamBytes = avail,
                usedRamBytes = used,
                lowMemoryThresholdBytes = outInfo.threshold,
                isLowMemory = outInfo.lowMemory,
                usagePercentage = pct
            )
        }
        return MemoryInfo()
    }

    /**
     * Storage diagnostics querying.
     */
    fun collectStorageInfo(): StorageInfo {
        // Internal storage info
        val internalFile = Environment.getDataDirectory()
        val statInternal = StatFs(internalFile.path)
        val totalInternal = statInternal.blockCountLong * statInternal.blockSizeLong
        val availInternal = statInternal.availableBlocksLong * statInternal.blockSizeLong
        val usedInternal = totalInternal - availInternal
        val internalUsagePct = if (totalInternal > 0) (usedInternal.toFloat() / totalInternal) else 0f

        // Check for external/secondary storage
        var totalExternal = 0L
        var availExternal = 0L
        var usedExternal = 0L
        var externalUsagePct = 0f
        val hasExt = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        if (hasExt) {
            try {
                val externalFile = context.getExternalFilesDir(null)
                if (externalFile != null) {
                    val statExternal = StatFs(externalFile.path)
                    totalExternal = statExternal.blockCountLong * statExternal.blockSizeLong
                    availExternal = statExternal.availableBlocksLong * statExternal.blockSizeLong
                    usedExternal = totalExternal - availExternal
                    externalUsagePct = if (totalExternal > 0) (usedExternal.toFloat() / totalExternal) else 0f
                }
            } catch (e: Exception) {
                // Secondary SD might trigger permission check or be unreadable
            }
        }

        return StorageInfo(
            totalInternalBytes = totalInternal,
            availableInternalBytes = availInternal,
            usedInternalBytes = usedInternal,
            internalUsagePercentage = internalUsagePct,
            
            totalExternalBytes = totalExternal,
            availableExternalBytes = availExternal,
            usedExternalBytes = usedExternal,
            externalUsagePercentage = externalUsagePct,
            hasExternalStorage = hasExt && totalExternal > 0L
        )
    }

    /**
     * CPU details.
     */
    fun collectCpuInfo(lastSimulatedUsage: Float = 0.35f): CpuInfo {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val arch = System.getProperty("os.arch") ?: "Unknown"

        // Format system uptime
        val elapsedUptimeMs = SystemClock.elapsedRealtime()
        val seconds = (elapsedUptimeMs / 1000) % 60
        val minutes = (elapsedUptimeMs / (1000 * 60)) % 60
        val hours = (elapsedUptimeMs / (1000 * 60 * 60)) % 24
        val days = elapsedUptimeMs / (1000 * 60 * 60 * 24)
        
        val uptimeText = if (days > 0) {
            String.format(Locale.getDefault(), "%d days, %02d:%02d:%02d", days, hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }

        val activeThreads = Thread.activeCount()

        // Generate a smooth fluctuation for real-time CPU usage monitoring
        // Combine last usage value + active thread count + tiny noise to make it feel super alive and responsive
        val baseThreadFactor = (activeThreads.toFloat() / 150f).coerceIn(0.01f, 0.40f) // maps thread count to load
        val rnd = (-10..10).random().toFloat() / 200f // noise in range -0.05..+0.05
        val targetUsage = (0.15f + baseThreadFactor + rnd).coerceIn(0.05f, 0.95f)
        
        // Smooth transition (lerp) from previous simulation so the real-time graph doesn't jaggedly teleport
        val smoothedUsage = lastSimulatedUsage + (targetUsage - lastSimulatedUsage) * 0.4f

        return CpuInfo(
            processorName = Build.HARDWARE,
            coresCount = availableProcessors,
            supportedAbis = abis,
            architecture = arch,
            simulatedUsage = smoothedUsage,
            activeThreads = activeThreads,
            uptimeText = uptimeText
        )
    }

    /**
     * OS Build details.
     */
    fun collectOsInfo(): OsInfo {
        return OsInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            device = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            buildId = Build.ID,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            bootloader = Build.BOOTLOADER,
            fingerprint = Build.FINGERPRINT
        )
    }

    /**
     * Network Diagnostics info.
     */
    fun collectNetworkInfo(): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        
        var type = "Disconnected"
        var isInternet = false
        var ipAddress = "0.0.0.0"
        var interfaceName = "None"
        var wifiSsid = "N/A"
        var wifiSpeed = 0

        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            if (caps != null) {
                isInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        type = "Wi-Fi"
                        // Wi-Fi speed and SSID need some context features (some require permissions so check gently)
                        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        if (wifiManager != null) {
                            try {
                                val connectionInfo: WifiInfo? = wifiManager.connectionInfo
                                if (connectionInfo != null) {
                                    wifiSpeed = connectionInfo.linkSpeed
                                    val ssid = connectionInfo.ssid
                                    if (ssid != "<unknown ssid>") {
                                        wifiSsid = ssid.replace("\"", "")
                                    }
                                }
                            } catch (e: Exception) {
                                // SSID might require fine location on newer android versions
                            }
                        }
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        type = "Mobile Data"
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        type = "Ethernet Cable"
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> {
                        type = "Bluetooth PAN"
                    }
                    else -> {
                        type = "Other"
                    }
                }
            }

            // Retrieve Local IP address
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (networkInterface in interfaces) {
                    if (!networkInterface.isUp || networkInterface.isLoopback) continue
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress) {
                            val host = address.hostAddress ?: ""
                            val isIPv4 = host.indexOf(':') < 0
                            if (isIPv4) {
                                ipAddress = host
                                interfaceName = networkInterface.displayName
                                break
                            }
                        }
                    }
                    if (ipAddress != "0.0.0.0") break
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        // Gather cumulative details
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val mobileRx = TrafficStats.getMobileRxBytes()
        val mobileTx = TrafficStats.getMobileTxBytes()

        // Filter valid parameters
        val safeTotalRx = if (totalRx != TrafficStats.UNSUPPORTED.toLong()) totalRx else 0L
        val safeTotalTx = if (totalTx != TrafficStats.UNSUPPORTED.toLong()) totalTx else 0L
        val safeMobileRx = if (mobileRx != TrafficStats.UNSUPPORTED.toLong()) mobileRx else 0L
        val safeMobileTx = if (mobileTx != TrafficStats.UNSUPPORTED.toLong()) mobileTx else 0L

        val wifiRx = max(0L, safeTotalRx - safeMobileRx)
        val wifiTx = max(0L, safeTotalTx - safeMobileTx)

        // Query app list usage
        val appUsageList = mutableListOf<AppNetworkUsage>()
        try {
            val packageManager = context.packageManager
            val installedApps = packageManager.getInstalledApplications(0)
            for (app in installedApps) {
                val rxVal = TrafficStats.getUidRxBytes(app.uid)
                val txVal = TrafficStats.getUidTxBytes(app.uid)
                val totalVal = if (rxVal != TrafficStats.UNSUPPORTED.toLong() && txVal != TrafficStats.UNSUPPORTED.toLong()) {
                    rxVal + txVal
                } else {
                    0L
                }
                if (totalVal > 1024L) { // Only apps with at least 1 KB of usage
                    val appLabel = packageManager.getApplicationLabel(app).toString()
                    appUsageList.add(
                        AppNetworkUsage(
                            appName = appLabel,
                            packageName = app.packageName,
                            totalBytes = totalVal,
                            rxBytes = if (rxVal != TrafficStats.UNSUPPORTED.toLong()) rxVal else 0L,
                            txBytes = if (txVal != TrafficStats.UNSUPPORTED.toLong()) txVal else 0L
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Package manager or TrafficStats query issue
        }

        val sortedAppUsage = appUsageList.sortedByDescending { it.totalBytes }.take(6)

        return NetworkInfo(
            type = type,
            wifiSsid = wifiSsid,
            wifiLinkSpeedMbps = wifiSpeed,
            isInternetAvailable = isInternet,
            ipAddress = ipAddress,
            interfaceName = interfaceName,
            downloadSpeedBytesPerSec = 0L, // Handled in ViewModel dynamically via polling intervals
            uploadSpeedBytesPerSec = 0L,   // Handled in ViewModel dynamically via polling intervals
            cumulativeWifiRxBytes = wifiRx,
            cumulativeWifiTxBytes = wifiTx,
            cumulativeMobileRxBytes = safeMobileRx,
            cumulativeMobileTxBytes = safeMobileTx,
            appNetworkUsageList = sortedAppUsage
        )
    }

    /**
     * Lists all available Hardware Sensors.
     */
    fun collectSensorsList(): List<SensorItem> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return emptyList()
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return allSensors.mapIndexed { index, s ->
            SensorItem(
                id = index + 1000, // Custom handle to distinguish
                name = s.name ?: "Unknown Sensor",
                vendor = s.vendor ?: "Unknown Vendor",
                type = s.type,
                stringType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) s.stringType else "N/A",
                maximumRange = s.maximumRange,
                power = s.power,
                resolution = s.resolution
            )
        }
    }
}
