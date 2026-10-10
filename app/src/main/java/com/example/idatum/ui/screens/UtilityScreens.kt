package com.example.idatum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idatum.dlms.DeviceAssociation
import com.example.idatum.model.DefaultSecurityKeyModel
import com.example.idatum.model.ProbeInterfaceType
import com.example.idatum.service.RootDetectionService
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FieldEmerald
import com.example.ui.theme.FieldRose
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------------------
// 1. SETTINGS SCREEN (Probe Interface, Baud rate, Security Keys, Offline mode)
// -----------------------------------------------------------------------------------------
@Composable
fun SettingsScreen(
    deviceAssociation: DeviceAssociation,
    onClearOfflineFiles: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedInterface by remember { mutableStateOf(deviceAssociation.activeInterface) }
    var selectedBaud by remember { mutableIntStateOf(deviceAssociation.baudRate) }
    var isOfflineModeEnabled by remember { mutableStateOf(true) }
    var securityKeys by remember { mutableStateOf(DefaultSecurityKeyModel()) }
    var statusFeedback by remember { mutableStateOf("") }

    val rootService = remember { RootDetectionService() }
    val integrityStatus = remember { rootService.getDeviceIntegrityStatus() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Optical Probe Hardware Interface
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Optical Probe Interface",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ProbeInterfaceType.values().forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedInterface == type),
                                onClick = {
                                    selectedInterface = type
                                    deviceAssociation.setInterface(type)
                                    statusFeedback = "Probe interface set to ${type.name}"
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = when (type) {
                                        ProbeInterfaceType.BLUETOOTH -> "Bluetooth Optical Probe (Gurux.Bluetooth)"
                                        ProbeInterfaceType.SERIAL_USB -> "Serial / USB OTG Probe (Gurux.Serial)"
                                        ProbeInterfaceType.SIMULATOR -> "Smart Meter Optical Probe Simulator / Lab Mode"
                                    },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Baud Rate:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(9600, 19200, 57600, 115200).forEach { baud ->
                            FilterChip(
                                selected = (selectedBaud == baud),
                                onClick = {
                                    selectedBaud = baud
                                    deviceAssociation.baudRate = baud
                                },
                                label = { Text("$baud") },
                                modifier = Modifier.testTag("baud_chip_$baud")
                            )
                        }
                    }
                }
            }
        }

        // Offline Mode & Storage Management (Section 12)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Field Offline Mode & Local Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Field Offline Mode", fontWeight = FontWeight.Medium)
                            Text("Allows probe operation without active cellular connectivity", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isOfflineModeEnabled,
                            onCheckedChange = { isOfflineModeEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onClearOfflineFiles()
                            statusFeedback = "Offline cached files cleared."
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldRose),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_clear_offline_cache")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Local Offline Files (Supervisor Action)")
                    }
                }
            }
        }

        // Default Security Keys (Section 10 DefaultSecurityKeyModel)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DLMS Default Security Keys (HLS / GMAC)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Low Level Security (LLS): ${securityKeys.lls}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("High Level Security (HLS): ${securityKeys.hls}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Encryption Key (EK): ${securityKeys.ek.take(16)}...", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Authentication Key (AK): ${securityKeys.ak.take(16)}...", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Device Integrity: $integrityStatus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FieldEmerald)
                }
            }
        }

        if (statusFeedback.isNotEmpty()) {
            item {
                Text(text = statusFeedback, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. LOCATION & MAP VIEW SCREEN (Section 4 GoogleLocationView / MeterMapPage)
// -----------------------------------------------------------------------------------------
@Composable
fun LocationScreen() {
    var lat by remember { mutableStateOf("25.609324") }
    var lng by remember { mutableStateOf("85.123518") }
    var accuracy by remember { mutableStateOf("±3.2 meters (GNSS Fixed)") }
    var address by remember { mutableStateOf("Bailey Road, Sub-Division Central, Patna, Bihar, India") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("location_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GPS Coordinates & GIS Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Latitude:", fontWeight = FontWeight.SemiBold)
                        Text(lat, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Longitude:", fontWeight = FontWeight.SemiBold)
                        Text(lng, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Accuracy:", fontWeight = FontWeight.SemiBold)
                        Text(accuracy, color = FieldEmerald, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Reverse Geocoded Address:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text(address, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Nearby Assigned Meters on Map", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "INT202409841 - 0.05 km (Rajesh Sharma)",
                        "INT202409842 - 0.28 km (Pooja Patel)",
                        "INT202409843 - 0.65 km (Amit Kumar Verma)"
                    ).forEach { meterInfo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(meterInfo, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. HELP & ABOUT SCREEN (User Manual, Version Specs, DISCOM Info)
// -----------------------------------------------------------------------------------------
@Composable
fun HelpScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("help_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("i-DATUM HHU Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Package: com.intellismartinfra.IntelliSmart.HHU", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text("Version: 6.1.0 (Assembly 6.1.0.0)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "i-DATUM is a Hand-Held Unit (HHU) field tool engineered for smart electric meter readout, relay reconnect/disconnect control, maximum demand reset, TOU profile programming, CT commissioning, and ticket synchronization.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Field Quick Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Align optical probe securely with the optical communication port on the smart meter front panel.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("2. Select probe connection mode in Settings (Bluetooth or USB OTG).", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3. Tap 'Connect' or launch 'Read Meter' to initiate SNRM and AARQ association.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("4. Once readout completes, records are saved offline with AES-256 encryption.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("5. Return to network coverage and use 'Upload Files' to push records to MDM.", fontSize = 12.sp)
                }
            }
        }
    }
}
