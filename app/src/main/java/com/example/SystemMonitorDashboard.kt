package com.example

import android.hardware.Sensor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorDashboard(
    viewModel: SystemMonitorViewModel,
    modifier: Modifier = Modifier
) {
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val cpuInfo by viewModel.cpuInfo.collectAsState()
    val memoryInfo by viewModel.memoryInfo.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
    val osInfo by viewModel.osInfo.collectAsState()
    val sensorsList by viewModel.sensorsList.collectAsState()
    
    val selectedSensor by viewModel.selectedSensor.collectAsState()
    val sensorTelemetry by viewModel.sensorTelemetry.collectAsState()
    
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val memoryHistory by viewModel.memoryHistory.collectAsState()

    var activeTab by remember { mutableStateOf("OVERVIEW") } // "OVERVIEW", "ALERTS", "SENSORS", "OS_INFO", "SETTINGS"
    
    // customizable theme preferences (settings)
    var bgThemeEnabled by remember { mutableStateOf(true) }
    var selectedColorGrading by remember { mutableStateOf("Emerald Palms") } // "Emerald Palms", "Golden Sand", "Sky Blue", "Default Indigo"

    val themeAccentColor = when (selectedColorGrading) {
        "Emerald Palms" -> Color(0xFF10B981) // Emerald green
        "Golden Sand" -> Color(0xFFD97706)  // Golden sand amber
        "Sky Blue" -> Color(0xFF0284C7)      // Clear gulf blue
        else -> CyberCyan                    // Classic premium Indigo
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (bgThemeEnabled) {
            Image(
                painter = painterResource(id = R.drawable.bg_default_1780234346970),
                contentDescription = "Tropical Resort Background Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberDarkBg.copy(alpha = 0.88f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberDarkBg)
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    color = CyberBlack.copy(alpha = if (bgThemeEnabled) 0.9f else 1.0f),
                    border = BorderStroke(width = 1.dp, color = BorderColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(themeAccentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "System Status Monitor Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Device Pro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "LIVE ANALYTICS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Refresh Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(CyberSurfaceVariant.copy(alpha = 0.8f))
                                .clickable { viewModel.forceDiagnosticsRefresh() }
                                .testTag("force_refresh_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Force Diagnostics Refresh",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                NavigationTabsRow(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        "OVERVIEW" -> {
                            OverviewTabContent(
                                cpuInfo = cpuInfo,
                                cpuHistory = cpuHistory,
                                memoryInfo = memoryInfo,
                                memoryHistory = memoryHistory,
                                batteryInfo = batteryInfo,
                                storageInfo = storageInfo,
                                networkInfo = networkInfo,
                                themeAccentColor = themeAccentColor
                            )
                        }
                        "ALERTS" -> {
                            AlertsTabContent(
                                viewModel = viewModel,
                                batteryInfo = batteryInfo,
                                memoryInfo = memoryInfo,
                                themeAccentColor = themeAccentColor
                            )
                        }
                        "SENSORS" -> {
                            SensorsTabContent(
                                sensorsList = sensorsList,
                                selectedSensor = selectedSensor,
                                sensorTelemetry = sensorTelemetry,
                                onSensorSelected = { viewModel.selectSensor(it) },
                                onClearSensor = { viewModel.clearSelectedSensor() },
                                themeAccentColor = themeAccentColor
                            )
                        }
                        "OS_INFO" -> {
                            OsInfoTabContent(
                                osInfo = osInfo,
                                themeAccentColor = themeAccentColor
                            )
                        }
                        "SETTINGS" -> {
                            SettingsTabContent(
                                bgThemeEnabled = bgThemeEnabled,
                                onBgThemeChange = { bgThemeEnabled = it },
                                selectedColorGrading = selectedColorGrading,
                                onColorGradingChange = { selectedColorGrading = it },
                                themeAccentColor = themeAccentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationTabsRow(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = CyberBlack, // Pure white under high density
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItemButton(
                title = "DIAGNOSTICS",
                icon = Icons.Outlined.Analytics,
                isActive = activeTab == "OVERVIEW",
                onClick = { onTabSelected("OVERVIEW") },
                modifier = Modifier.testTag("tab_overview")
            )
            TabItemButton(
                title = "SENSORS",
                icon = Icons.Outlined.Sensors,
                isActive = activeTab == "SENSORS",
                onClick = { onTabSelected("SENSORS") },
                modifier = Modifier.testTag("tab_sensors")
            )
            TabItemButton(
                title = "HARDWARE",
                icon = Icons.Outlined.DeveloperMode,
                isActive = activeTab == "OS_INFO",
                onClick = { onTabSelected("OS_INFO") },
                modifier = Modifier.testTag("tab_os")
            )
        }
    }
}

@Composable
fun RowScope.TabItemButton(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) CyberCyan else Color.Transparent
    val backgroundColor = if (isActive) CyberCyan.copy(alpha = 0.08f) else Color.Transparent
    val contentColor = if (isActive) CyberCyan else TextSecondary

    Box(
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$title Tab Icon",
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif, // High density sleek display
                color = contentColor
            )
        }
    }
}

@Composable
fun OverviewTabContent(
    cpuInfo: CpuInfo,
    cpuHistory: List<Float>,
    memoryInfo: MemoryInfo,
    memoryHistory: List<Float>,
    batteryInfo: BatteryInfo,
    storageInfo: StorageInfo,
    networkInfo: NetworkInfo,
    themeAccentColor: Color = CyberCyan
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Holographic gauge circle cockpit at the top
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REAL-TIME HUDS COCKPIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CustomConcentricGaugeHUD(
                        cpuUsage = cpuInfo.simulatedUsage,
                        ramUsage = memoryInfo.usagePercentage,
                        batteryLevel = batteryInfo.level.toFloat() / 100f,
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HUDLegendItem(
                            label = "PWR [BATTERY]",
                            value = "${batteryInfo.level}%",
                            color = CyberGreen
                        )
                        HUDLegendItem(
                            label = "MEM [RAM]",
                            value = String.format(Locale.US, "%.0f%%", memoryInfo.usagePercentage * 100),
                            color = CyberCyan
                        )
                        HUDLegendItem(
                            label = "OPS [CPU]",
                            value = String.format(Locale.US, "%.0f%%", cpuInfo.simulatedUsage * 100),
                            color = CyberAmber
                        )
                    }
                }
            }
        }

        // Live CPU detailed card with real-time waveform
        MetricPerformanceCard(
            title = "PROCESSOR TELEMETRY [CPU]",
            activeLoad = cpuInfo.simulatedUsage,
            history = cpuHistory,
            themeColor = CyberAmber,
            modifier = Modifier.testTag("cpu_metric_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricRowItem(label = "Processor Core", value = cpuInfo.processorName)
                MetricRowItem(label = "Active Cores", value = "${cpuInfo.coresCount} Logical Threads")
                MetricRowItem(label = "Architecture", value = cpuInfo.architecture)
                MetricRowItem(label = "System Uptime", value = cpuInfo.uptimeText)
                MetricRowItem(label = "Virtual ABIs", value = cpuInfo.supportedAbis, singleLine = true)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Memory (RAM) Detailed card
        MetricPerformanceCard(
            title = "MEMORY ALLOCATION [RAM]",
            activeLoad = memoryInfo.usagePercentage,
            history = memoryHistory,
            themeColor = CyberCyan,
            modifier = Modifier.testTag("memory_metric_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricRowItem(label = "Total RAM Installed", value = formatBytes(memoryInfo.totalRamBytes))
                MetricRowItem(label = "Currently Occupied", value = formatBytes(memoryInfo.usedRamBytes))
                MetricRowItem(label = "Available Free Size", value = formatBytes(memoryInfo.availableRamBytes))
                MetricRowItem(
                    label = "OOM Pressure State", 
                    value = if (memoryInfo.isLowMemory) "CRITICAL / LOW MEMORY" else "STABLE / HEURISTIC GOOD",
                    color = if (memoryInfo.isLowMemory) CyberRed else CyberGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Battery Diagnostics
        DiagnosticGridCard(
            title = "POWER MATRIX MONITOR",
            icon = Icons.Default.ElectricBolt,
            metricColor = CyberGreen
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matrix Charge Level",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = "Battery Status Icon",
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${batteryInfo.level}% [${batteryInfo.status}]",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Fancy linear slider bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(batteryInfo.level.toFloat() / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CyberGreen.copy(alpha = 0.6f), CyberGreen)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                MetricRowItem(label = "Aggregated Health", value = batteryInfo.health, color = if (batteryInfo.health == "Good") CyberGreen else CyberAmber)
                MetricRowItem(label = "Cell Tech", value = batteryInfo.technology)
                MetricRowItem(label = "Temperature Specs", value = String.format(Locale.US, "%.1f°C / %.1f°F", batteryInfo.temperatureCelsius, batteryInfo.temperatureFahrenheit))
                MetricRowItem(label = "Voltage Rating", value = batteryInfo.voltageText)
                MetricRowItem(label = "Core Power Source", value = batteryInfo.powerSource)

                val microAmp = batteryInfo.currentNowMicroAmperes
                if (microAmp != 0L) {
                    // Convert micro-amps to milli-amps
                    val milliAmp = microAmp.toFloat() / 1000f
                    MetricRowItem(
                        label = "Dynamic Amperage", 
                        value = String.format(Locale.US, "%.1f mA", milliAmp),
                        color = if (milliAmp > 0) CyberGreen else CyberRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disk Storage Metrics
        DiagnosticGridCard(
            title = "CYBERNETIC STORAGE ALLOCATION",
            icon = Icons.Default.Storage,
            metricColor = CyberCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Internal storage bar
                Text(
                    text = "INTERNAL SSD STORAGE [ROOT]",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                MetricSliderMeter(
                    fraction = storageInfo.internalUsagePercentage,
                    textLeft = "Used: ${formatBytes(storageInfo.usedInternalBytes)}",
                    textRight = "Total: ${formatBytes(storageInfo.totalInternalBytes)}",
                    meterColor = CyberCyan
                )

                if (storageInfo.hasExternalStorage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "EXTERNAL SD EXPANSION [SD_CARD]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    MetricSliderMeter(
                        fraction = storageInfo.externalUsagePercentage,
                        textLeft = "Used: ${formatBytes(storageInfo.usedExternalBytes)}",
                        textRight = "Total: ${formatBytes(storageInfo.totalExternalBytes)}",
                        meterColor = CyberAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Network diagnostics
        DiagnosticGridCard(
            title = "COMMUNICATION MATRIX INTERFACE",
            icon = Icons.Default.Wifi,
            metricColor = themeAccentColor
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricRowItem(
                    label = "Web Connection Status", 
                    value = if (networkInfo.isInternetAvailable) "INTERNET CONNECTED" else "OFFLINE NODE",
                    color = if (networkInfo.isInternetAvailable) CyberGreen else CyberRed
                )
                MetricRowItem(label = "Link Interface Mode", value = networkInfo.type)
                MetricRowItem(label = "Hardware Core IP", value = networkInfo.ipAddress)
                MetricRowItem(label = "Network Port ID", value = networkInfo.interfaceName)
                
                if (networkInfo.type == "Wi-Fi") {
                    MetricRowItem(label = "Wi-Fi SSID Key", value = networkInfo.wifiSsid)
                    if (networkInfo.wifiLinkSpeedMbps > 0) {
                        MetricRowItem(label = "Connection Bandwidth", value = "${networkInfo.wifiLinkSpeedMbps} Mbps")
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                
                // Real-time Upload/Download Speed
                Text(
                    text = "REAL-TIME BANDWIDTH METER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = themeAccentColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download Link",
                                tint = CyberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(text = "DOWNLINK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text(text = formatSpeed(networkInfo.downloadSpeedBytesPerSec), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload Link",
                                tint = themeAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(text = "UPLINK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text(text = formatSpeed(networkInfo.uploadSpeedBytesPerSec), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                
                // Cumulative overall usage
                Text(
                    text = "CUMULATIVE DATA TRANSFERRED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = themeAccentColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                MetricRowItem(
                    label = "Wi-Fi Received (DL)", 
                    value = formatBytes(networkInfo.cumulativeWifiRxBytes)
                )
                MetricRowItem(
                    label = "Wi-Fi Sent (UL)", 
                    value = formatBytes(networkInfo.cumulativeWifiTxBytes)
                )
                val totalWifi = networkInfo.cumulativeWifiRxBytes + networkInfo.cumulativeWifiTxBytes
                MetricRowItem(
                    label = "Total Wi-Fi Data", 
                    value = formatBytes(totalWifi),
                    color = themeAccentColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                MetricRowItem(
                    label = "Mobile Received (DL)", 
                    value = formatBytes(networkInfo.cumulativeMobileRxBytes)
                )
                MetricRowItem(
                    label = "Mobile Sent (UL)", 
                    value = formatBytes(networkInfo.cumulativeMobileTxBytes)
                )
                val totalMobile = networkInfo.cumulativeMobileRxBytes + networkInfo.cumulativeMobileTxBytes
                MetricRowItem(
                    label = "Total Mobile Data", 
                    value = formatBytes(totalMobile),
                    color = CyberAmber
                )

                if (networkInfo.appNetworkUsageList.isNotEmpty()) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "TOP APPLICATIONS NETWORK PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = themeAccentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val maxAppUsage = networkInfo.appNetworkUsageList.maxOfOrNull { it.totalBytes } ?: 1L
                    
                    networkInfo.appNetworkUsageList.forEach { appUsage ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(0.7f)) {
                                    Text(
                                        text = appUsage.appName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = appUsage.packageName,
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatBytes(appUsage.totalBytes),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = themeAccentColor,
                                    modifier = Modifier.weight(0.3f),
                                    textAlign = TextAlign.End
                                )
                            }
                            
                            val relativeProgress = (appUsage.totalBytes.toFloat() / maxAppUsage.toFloat()).coerceIn(0f, 1f)
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = relativeProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = themeAccentColor.copy(alpha = 0.8f),
                                trackColor = BorderColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SensorsTabContent(
    sensorsList: List<SensorItem>,
    selectedSensor: SensorItem?,
    sensorTelemetry: ActiveSensorTelemetry?,
    onSensorSelected: (SensorItem) -> Unit,
    onClearSensor: () -> Unit,
    themeAccentColor: Color = CyberCyan
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedSensor != null) {
            // Display live sandbox visualizer for selected sensor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberCyan)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE PHY-DATA FEED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = selectedSensor.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onClearSensor,
                            modifier = Modifier
                                .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sensor Sandbox View",
                                tint = CyberRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (sensorTelemetry != null) {
                        // Display telemetry values dynamically
                        SensorCoordinatesTracker(
                            telemetry = sensorTelemetry,
                            selectedSensor = selectedSensor
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyberCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Acquiring live telemetry...",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "HARDWARE SENSORS CAPTURED [${sensorsList.size}]",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = CyberGreen,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        if (sensorsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No compatible sensors found on device.",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sensorsList) { sensorItem ->
                    val isSelected = selectedSensor?.name == sensorItem.name && selectedSensor?.type == sensorItem.type
                    SensorItemRow(
                        sensorItem = sensorItem,
                        isSelected = isSelected,
                        onClick = { onSensorSelected(sensorItem) }
                    )
                }
            }
        }
    }
}

@Composable
fun SensorCoordinatesTracker(
    telemetry: ActiveSensorTelemetry,
    selectedSensor: SensorItem
) {
    // Generate simple live-scrolling canvas list for the sandbox to show fluctuation!
    val floatHistoryX = remember { mutableStateListOf<Float>() }
    val floatHistoryY = remember { mutableStateListOf<Float>() }
    val floatHistoryZ = remember { mutableStateListOf<Float>() }

    // Use current sensor value
    val xVal = telemetry.values.getOrElse(0) { 0f }
    val yVal = telemetry.values.getOrElse(1) { 0f }
    val zVal = telemetry.values.getOrElse(2) { 0f }

    // Limit length and insert new telemetry points
    LaunchedEffect(telemetry) {
        if (floatHistoryX.size >= 40) {
            floatHistoryX.removeAt(0)
            floatHistoryY.removeAt(0)
            floatHistoryZ.removeAt(0)
        }
        floatHistoryX.add(xVal)
        floatHistoryY.add(yVal)
        floatHistoryZ.add(zVal)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SensorTelemetryGauge(label = "AXIS_X", value = String.format(Locale.US, "%.3f", xVal), color = CyberCyan)
            SensorTelemetryGauge(label = "AXIS_Y", value = String.format(Locale.US, "%.3f", yVal), color = CyberGreen)
            SensorTelemetryGauge(label = "AXIS_Z", value = String.format(Locale.US, "%.3f", zVal), color = CyberAmber)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High fidelity sandbox line oscillograph
        Text(
            text = "HEURISTIC VECTOR WAVEFORMS",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(CyberBlack, RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw central grids
            val dInterval = width / 10f
            for (i in 1..9) {
                drawLine(
                    color = ChartGridLine,
                    start = Offset(i * dInterval, 0f),
                    end = Offset(i * dInterval, height),
                    strokeWidth = 1f
                )
            }
            drawLine(
                color = BorderColor.copy(alpha = 0.5f),
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = 2f
            )

            // Dynamic scale helper
            var maxMag = 12f
            floatHistoryX.forEach { if (kotlin.math.abs(it) > maxMag) maxMag = kotlin.math.abs(it) }
            floatHistoryY.forEach { if (kotlin.math.abs(it) > maxMag) maxMag = kotlin.math.abs(it) }
            floatHistoryZ.forEach { if (kotlin.math.abs(it) > maxMag) maxMag = kotlin.math.abs(it) }

            // 2. Draw lines for X, Y, Z coordinates
            drawOscilloscopeLine(floatHistoryX, CyberCyan, height, width, maxMag)
            drawOscilloscopeLine(floatHistoryY, CyberGreen, height, width, maxMag)
            drawOscilloscopeLine(floatHistoryZ, CyberAmber, height, width, maxMag)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBlack, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MetricRowItem(label = "Physical Vendor", value = selectedSensor.vendor)
            MetricRowItem(label = "Resolution Rating", value = "${selectedSensor.resolution}")
            MetricRowItem(label = "Hardware Power Draw", value = String.format(Locale.US, "%.3f mA", selectedSensor.power))
            MetricRowItem(label = "Maximum Range Cap", value = "${selectedSensor.maximumRange}")
        }
    }
}

fun drawScopeOscilloscopeLine(
    canvas: Canvas,
    history: List<Float>,
    color: Color,
    height: Float,
    width: Float,
    maxMag: Float
) {
    // Left empty helper
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOscilloscopeLine(
    history: List<Float>,
    color: Color,
    height: Float,
    width: Float,
    maxMag: Float
) {
    if (history.size < 2) return
    val stepX = width / 39f // matching limit of 40 points
    val path = Path()

    history.forEachIndexed { index, value ->
        // Normalize coordinates to fit in canvas vertical structure
        // Y=0 represents top, height is bottom, center is height/2
        val normalizedY = (height / 2) - (value / maxMag) * (height / 2.2f)
        val currentX = index * stepX

        if (index == 0) {
            path.moveTo(currentX, normalizedY)
        } else {
            path.lineTo(currentX, normalizedY)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
fun SensorTelemetryGauge(
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .background(CyberBlack, RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .widthIn(min = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SensorItemRow(
    sensorItem: SensorItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CyberCyan else BorderColor
    val cardBg = if (isSelected) CyberSurfaceVariant.copy(alpha = 0.3f) else MetricCardBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Sensors else Icons.Default.SensorsOff,
                contentDescription = "Sensor state icon",
                tint = if (isSelected) CyberCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sensorItem.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) CyberCyan else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = sensorItem.vendor,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "• TYPE ${sensorItem.type}",
                    fontSize = 11.sp,
                    color = CyberGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Expand item visual detail",
            tint = BorderColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RegistryRowItem(
    label: String,
    value: String,
    icon: ImageVector = Icons.Default.Info,
    color: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(0.45f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label Icon",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
fun OsInfoTabContent(
    osInfo: OsInfo,
    themeAccentColor: Color = CyberCyan
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        DiagnosticGridCard(
            title = "DEVICE & BRANDING TELEMETRY",
            icon = Icons.Default.PhoneAndroid,
            metricColor = CyberCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RegistryRowItem(label = "Marketing Device", value = osInfo.model, icon = Icons.Default.Smartphone)
                RegistryRowItem(label = "Manufacturer", value = osInfo.manufacturer, icon = Icons.Default.Business)
                RegistryRowItem(label = "Board Key", value = osInfo.board, icon = Icons.Default.DeveloperBoard)
                RegistryRowItem(label = "Chip Variant", value = osInfo.hardware, icon = Icons.Default.Memory)
                RegistryRowItem(label = "Product ID", value = osInfo.device, icon = Icons.Default.Grid3x3)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DiagnosticGridCard(
            title = "ANDROID SHELL ARCHITECTURE",
            icon = Icons.Default.SettingsSystemDaydream,
            metricColor = CyberGreen
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RegistryRowItem(label = "Android Release", value = osInfo.androidVersion, icon = Icons.Default.Android, color = CyberGreen)
                RegistryRowItem(label = "API SDK Value", value = "${osInfo.sdkInt}", icon = Icons.Default.Code, color = CyberGreen)
                RegistryRowItem(label = "Security Threshold", value = osInfo.securityPatch, icon = Icons.Default.Security)
                RegistryRowItem(label = "Build Fingerprint", value = osInfo.fingerprint, icon = Icons.Default.Fingerprint)
                RegistryRowItem(label = "Bootloader Hash", value = osInfo.bootloader, icon = Icons.Default.Folder)
                RegistryRowItem(label = "Platform ID", value = osInfo.buildId, icon = Icons.Default.Key)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MetricSliderMeter(
    fraction: Float,
    textLeft: String,
    textRight: String,
    meterColor: Color
) {
    val visualFraction = fraction.coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = textLeft,
                fontSize = 11.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%.0f%%", visualFraction * 100),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = meterColor,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BorderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(visualFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(meterColor.copy(alpha = 0.6f), meterColor)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = textRight,
            fontSize = 11.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun HUDLegendItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(130.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
    }
}

@Composable
fun CustomConcentricGaugeHUD(
    cpuUsage: Float,
    ramUsage: Float,
    batteryLevel: Float,
    modifier: Modifier = Modifier
) {
    // Concentric rotating rings
    val transition = rememberInfiniteTransition(label = "RingAnimation")
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val strokeW = 6.dp.toPx()
        val spacing = 12.dp.toPx()

        val halfStroke = strokeW / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Draw concentric backdrops & dynamic value sweeps
        // Outer sweep: Battery power (Green)
        val r1 = (diameter / 2) - halfStroke
        drawCircle(
            color = ChartGridLine,
            radius = r1,
            center = center,
            style = Stroke(width = strokeW)
        )
        drawArc(
            color = CyberGreen,
            startAngle = -90f + rotationAngle / 2f,
            sweepAngle = batteryLevel * 360f,
            useCenter = false,
            topLeft = Offset(center.x - r1, center.y - r1),
            size = Size(r1 * 2, r1 * 2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Middle sweep: RAM utilization (Cyan)
        val r2 = r1 - strokeW - spacing
        drawCircle(
            color = ChartGridLine,
            radius = r2,
            center = center,
            style = Stroke(width = strokeW)
        )
        drawArc(
            color = CyberCyan,
            startAngle = 180f - rotationAngle,
            sweepAngle = ramUsage * 360f,
            useCenter = false,
            topLeft = Offset(center.x - r2, center.y - r2),
            size = Size(r2 * 2, r2 * 2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Inner sweep: CPU computation load (Amber)
        val r3 = r2 - strokeW - spacing
        drawCircle(
            color = ChartGridLine,
            radius = r3,
            center = center,
            style = Stroke(width = strokeW)
        )
        drawArc(
            color = CyberAmber,
            startAngle = 45f + rotationAngle,
            sweepAngle = cpuUsage * 360f,
            useCenter = false,
            topLeft = Offset(center.x - r3, center.y - r3),
            size = Size(r3 * 2, r3 * 2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MetricPerformanceCard(
    title: String,
    activeLoad: Float,
    history: List<Float>,
    themeColor: Color,
    modifier: Modifier = Modifier,
    detailsContent: @Composable () -> Unit
) {
    val isCpu = title.contains("CPU")
    val cardBackground = if (isCpu) Color(0xFF2D2E32) else MetricCardBackground
    val cardBorderColor = if (isCpu) Color(0xFF3F444D) else BorderColor
    val textPrimaryColor = if (isCpu) Color.White else TextPrimary
    val textThemeColor = if (isCpu) Color(0xFF818CF8) else themeColor // Soft indigo highlight for dark CPU text

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = cardBackground,
        contentColor = textPrimaryColor,
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(textThemeColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = textPrimaryColor
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.1f%%", activeLoad * 100),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = textThemeColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Canvas Sparkline Chart
            RealtimeCanvasSparkline(
                history = history,
                lineColor = textThemeColor,
                gridColor = if (isCpu) Color(0x11FFFFFF) else ChartGridLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .testTag("telemetry_graph")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Extra metrics grid
            HorizontalDivider(color = cardBorderColor.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            detailsContent()
        }
    }
}

@Composable
fun RealtimeCanvasSparkline(
    history: List<Float>,
    lineColor: Color,
    gridColor: Color = ChartGridLine,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (history.size < 2) return@Canvas

        // Draw structural horizontal grids
        val gridLines = 4
        for (i in 0..gridLines) {
            val yPos = height * i / gridLines.toFloat()
            drawLine(
                color = gridColor,
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1f
            )
        }

        // Draw vertical timestamps grid
        val xPoints = 5
        for (i in 0..xPoints) {
            val xPos = width * i / xPoints.toFloat()
            drawLine(
                color = gridColor,
                start = Offset(xPos, 0f),
                end = Offset(xPos, height),
                strokeWidth = 1f
            )
        }

        val strokePx = 2.dp.toPx()
        val stepX = width / (history.size - 1).toFloat()
        
        val linePath = Path()
        val fillPath = Path()

        history.forEachIndexed { i, value ->
            // Invert value so 100% (1.0) is at top and 0% (0.0) is at bottom
            val yPos = height - (value.coerceIn(0f, 1f) * height)
            val xPos = i * stepX

            if (i == 0) {
                linePath.moveTo(xPos, yPos)
                fillPath.moveTo(xPos, height)
                fillPath.lineTo(xPos, yPos)
            } else {
                // Let's draw smooth cubic-curve or simple precise line segments
                linePath.lineTo(xPos, yPos)
                fillPath.lineTo(xPos, yPos)
            }
        }
        
        // Complete the polygon for the gradient fill path safely
        fillPath.lineTo((history.size - 1) * stepX, height)
        fillPath.close()

        // Draw area fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw path line outline
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun DiagnosticGridCard(
    title: String,
    icon: ImageVector,
    metricColor: Color,
    modifier: Modifier = Modifier,
    detailsContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MetricCardBackground),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(metricColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$title Icon Indicator",
                        tint = metricColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = BorderColor.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            detailsContent()
        }
    }
}

@Composable
fun MetricRowItem(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    singleLine: Boolean = false
) {
    val resolvedValueColor = if (color == Color.Unspecified) LocalContentColor.current else color
    val resolvedLabelColor = LocalContentColor.current.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = resolvedLabelColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = resolvedValueColor,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = if (singleLine) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f)
        )
    }
}

// Utility formatting bytes size
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    // Bound digit groups to safety limits
    val safeGroup = digitGroups.coerceIn(0, units.size - 1)
    return String.format(Locale.getDefault(), "%.2f %s", bytes / Math.pow(1024.0, safeGroup.toDouble()), units[safeGroup])
}

// Utility formatting connection speeds
fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    val digitGroups = (Math.log10(bytesPerSec.toDouble()) / Math.log10(1024.0)).toInt()
    val safeGroup = digitGroups.coerceIn(0, units.size - 1)
    return String.format(Locale.getDefault(), "%.1f %s", bytesPerSec / Math.pow(1024.0, safeGroup.toDouble()), units[safeGroup])
}

@Composable
fun AlertsTabContent(
    viewModel: SystemMonitorViewModel,
    batteryInfo: BatteryInfo,
    memoryInfo: MemoryInfo,
    themeAccentColor: Color = CyberCyan
) {
    val scrollState = rememberScrollState()
    val thresholds by viewModel.alertThresholds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiagnosticGridCard(
            title = "CUSTOM ALERT CONFLICT CENTRE",
            icon = Icons.Default.NotificationsActive,
            metricColor = themeAccentColor
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Configure customizable metrics thresholds below. When any value crosses your configured threshold, a system warning notification triggers instantly.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        thresholds.forEach { threshold ->
            val icon = when (threshold.id) {
                "battery_level" -> Icons.Default.BatteryAlert
                "cpu_temp" -> Icons.Default.Thermostat
                else -> Icons.Default.Memory
            }

            val currentMetricValue = when (threshold.id) {
                "battery_level" -> batteryInfo.level.toFloat()
                "cpu_temp" -> batteryInfo.temperatureCelsius
                else -> memoryInfo.availableRamBytes.toFloat() / (1024f * 1024f * 1024f)
            }

            val sliderRange = when (threshold.id) {
                "battery_level" -> 5f..95f
                "cpu_temp" -> 20f..85f
                else -> 0.1f..6.0f
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MetricCardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeAccentColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = threshold.metricName,
                                    tint = themeAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = threshold.metricName.uppercase(Locale.getDefault()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Current: ${String.format(Locale.getDefault(), "%.1f", currentMetricValue)}${threshold.unit}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = threshold.isEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.saveAlertThreshold(threshold.copy(isEnabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = themeAccentColor
                            )
                        )
                    }

                    if (threshold.isEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trigger when values are:",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val isLess = threshold.comparisonType == "LESS_THAN"
                                val activeBtnBg = themeAccentColor
                                val inactiveBtnBg = CyberSurfaceVariant
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isLess) activeBtnBg else inactiveBtnBg)
                                        .clickable {
                                            viewModel.saveAlertThreshold(threshold.copy(comparisonType = "LESS_THAN"))
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Below",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLess) Color.White else TextPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isLess) activeBtnBg else inactiveBtnBg)
                                        .clickable {
                                            viewModel.saveAlertThreshold(threshold.copy(comparisonType = "GREATER_THAN"))
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Above",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isLess) Color.White else TextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Limit Value:",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${threshold.thresholdValue.toInt()}${threshold.unit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeAccentColor
                            )
                        }

                        Slider(
                            value = threshold.thresholdValue,
                            onValueChange = { newVal ->
                                viewModel.saveAlertThreshold(threshold.copy(thresholdValue = newVal))
                            },
                            valueRange = sliderRange,
                            colors = SliderDefaults.colors(
                                thumbColor = themeAccentColor,
                                activeTrackColor = themeAccentColor,
                                inactiveTrackColor = BorderColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    bgThemeEnabled: Boolean,
    onBgThemeChange: (Boolean) -> Unit,
    selectedColorGrading: String,
    onColorGradingChange: (String) -> Unit,
    themeAccentColor: Color
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiagnosticGridCard(
            title = "THEMING OPTIONS PANEL",
            icon = Icons.Default.Palette,
            metricColor = themeAccentColor
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personalize the Device Pro HUD environment matching your style preferences. Changes apply globally across telemetry channels instantly.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        // Wallpaper Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MetricCardBackground),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeAccentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "Background Wallpaper Icon",
                            tint = themeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "BEACH RESORT WALLPAPER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Enjoy tropical palm beach resort background theme.",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = bgThemeEnabled,
                    onCheckedChange = onBgThemeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = themeAccentColor
                    )
                )
            }
        }

        // Color Grading Presets Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MetricCardBackground),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeAccentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Brand Accent Icon",
                            tint = themeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "THEME ACCENT GRADE PRESET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select a gorgeous color grading accent preset.",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                val presets = listOf(
                    "Emerald Palms" to Color(0xFF10B981),
                    "Golden Sand" to Color(0xFFD97706),
                    "Sky Blue" to Color(0xFF0284C7),
                    "Default Indigo" to Color(0xFF4F46E5)
                )

                presets.forEach { (presetName, presetColor) ->
                    val isSelected = selectedColorGrading == presetName
                    val selectionBorder = if (isSelected) BorderStroke(2.dp, themeAccentColor) else BorderStroke(1.dp, Color.Transparent)
                    val bgSelection = if (isSelected) themeAccentColor.copy(alpha = 0.05f) else Color.Transparent

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onColorGradingChange(presetName) },
                        border = selectionBorder,
                        color = bgSelection
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(presetColor)
                                )
                                Text(
                                    text = presetName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) themeAccentColor else TextPrimary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected Preset",
                                    tint = themeAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
