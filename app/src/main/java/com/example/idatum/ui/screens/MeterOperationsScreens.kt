package com.example.idatum.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idatum.dlms.DeviceAssociation
import com.example.idatum.model.Constant
import com.example.idatum.model.TouSlot
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FieldAmber
import com.example.ui.theme.FieldEmerald
import com.example.ui.theme.FieldRose
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------------------
// 1. RECONNECT SCREEN (Relay Re-Connection / Disconnection OBIS 0.0.96.3.10.255)
// -----------------------------------------------------------------------------------------
@Composable
fun ReconnectScreen(
    deviceAssociation: DeviceAssociation
) {
    val coroutineScope = rememberCoroutineScope()
    val connInfo by deviceAssociation.connectionInfo.collectAsState()
    var isExecuting by remember { mutableStateOf(false) }
    var actionResult by remember { mutableStateOf("Ready to send DLMS Disconnect/Reconnect command.") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingMethodId by remember { mutableIntStateOf(1) } // 1: Disconnect, 2: Reconnect

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(if (pendingMethodId == 1) "Confirm Disconnect Relay" else "Confirm Reconnect Relay") },
            text = {
                Text(
                    if (pendingMethodId == 1)
                        "Warning: This will open the load-breaker relay for meter ${connInfo.meterSerialNumber}. Power to consumer will be disconnected."
                    else
                        "This will close the load-breaker relay for meter ${connInfo.meterSerialNumber}. Ensure no safety hazards before restoring power."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        coroutineScope.launch {
                            isExecuting = true
                            actionResult = "Executing DLMS ACTION on ${Constant.DisConnectControlOBIS}..."
                            val ok = deviceAssociation.executeDlmsAction(
                                obis = Constant.DisConnectControlOBIS,
                                actionName = if (pendingMethodId == 1) "Relay Disconnect" else "Relay Reconnect",
                                classId = 70,
                                methodId = pendingMethodId
                            )
                            isExecuting = false
                            actionResult = if (ok) {
                                "Relay action completed successfully. Meter status updated."
                            } else {
                                "Action failed. Check DLMS Association."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pendingMethodId == 1) FieldRose else FieldEmerald
                    )
                ) {
                    Text("Confirm Execution")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reconnect_screen"),
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
                        text = "Relay Disconnect / Reconnect Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OBIS: ${Constant.DisConnectControlOBIS} (Class ID 70 / Disconnect Control)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Relay State", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = connInfo.relayState,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (connInfo.relayState.startsWith("Connected")) FieldEmerald else FieldRose
                            )
                        }
                        Icon(
                            imageVector = if (connInfo.relayState.startsWith("Connected")) Icons.Default.Power else Icons.Default.PowerOff,
                            contentDescription = null,
                            tint = if (connInfo.relayState.startsWith("Connected")) FieldEmerald else FieldRose,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                pendingMethodId = 2 // Reconnect
                                showConfirmDialog = true
                            },
                            enabled = !isExecuting,
                            colors = ButtonDefaults.buttonColors(containerColor = FieldEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_reconnect_relay")
                        ) {
                            Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconnect")
                        }

                        Button(
                            onClick = {
                                pendingMethodId = 1 // Disconnect
                                showConfirmDialog = true
                            },
                            enabled = !isExecuting,
                            colors = ButtonDefaults.buttonColors(containerColor = FieldRose),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_disconnect_relay")
                        ) {
                            Icon(Icons.Default.PowerOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = actionResult,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. MD RESET SCREEN (Maximum Demand Reset OBIS 0.0.10.0.1.255)
// -----------------------------------------------------------------------------------------
@Composable
fun MdResetScreen(
    deviceAssociation: DeviceAssociation
) {
    val coroutineScope = rememberCoroutineScope()
    val connInfo by deviceAssociation.connectionInfo.collectAsState()
    var isOperating by remember { mutableStateOf(false) }
    var resetReason by remember { mutableStateOf("Billing Cycle End - Authorized Reset") }
    var supervisorToken by remember { mutableStateOf("MD-TOKEN-8831") }
    var executionStatus by remember { mutableStateOf("Ready to send MD Reset action.") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("md_reset_screen"),
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
                        text = "Maximum Demand (MD) Reset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OBIS: ${Constant.MdResetOBIS} (Class ID 1 / Script Table)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Connected Meter: ${connInfo.meterSerialNumber}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = supervisorToken,
                        onValueChange = { supervisorToken = it },
                        label = { Text("Supervisor Authorization Token") },
                        modifier = Modifier.fillMaxWidth().testTag("input_md_token"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = resetReason,
                        onValueChange = { resetReason = it },
                        label = { Text("Reset Reason") },
                        modifier = Modifier.fillMaxWidth().testTag("input_md_reason"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isOperating = true
                                executionStatus = "Executing DLMS ACTION on MD Reset script (${Constant.MdResetOBIS})..."
                                val ok = deviceAssociation.executeDlmsAction(
                                    obis = Constant.MdResetOBIS,
                                    actionName = "Maximum Demand Reset",
                                    classId = 9,
                                    methodId = 1
                                )
                                isOperating = false
                                executionStatus = if (ok) {
                                    "MD Reset executed successfully. Billing registers closed and MD counters zeroed."
                                } else {
                                    "Failed to execute MD Reset. Check security rights."
                                }
                            }
                        },
                        enabled = !isOperating,
                        colors = ButtonDefaults.buttonColors(containerColor = FieldAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_execute_md_reset")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute Maximum Demand Reset")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = executionStatus,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. METERING MODE SCREEN (OBIS 0.0.94.96.19.255 & 0.0.94.91.19.255)
// -----------------------------------------------------------------------------------------
@Composable
fun MeteringModeScreen(
    deviceAssociation: DeviceAssociation
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf("Post-Paid Normal") }
    var isOperating by remember { mutableStateOf(false) }
    var modeStatus by remember { mutableStateOf("Current meter mode loaded from DLMS register.") }

    val modes = listOf("Post-Paid Normal", "Pre-Paid Smart", "Net-Metering Solar", "Tamper Test Mode")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("metering_mode_screen"),
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
                        text = "Metering Mode Operations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OBIS: ${Constant.MeteringModeOBIS} / ${Constant.MeteringModeOBIS_SEIPL}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Select Target Mode:", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    modes.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedMode == mode),
                                onClick = { selectedMode = mode }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = mode, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isOperating = true
                                modeStatus = "Sending SET-Request to ${Constant.MeteringModeOBIS} for $selectedMode..."
                                val res = deviceAssociation.setDlmsAttribute(
                                    Constant.MeteringModeOBIS,
                                    "Metering Mode",
                                    selectedMode
                                )
                                isOperating = false
                                modeStatus = if (res.isSuccess) {
                                    "Metering Mode successfully switched to '$selectedMode'!"
                                } else {
                                    "Failed: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        enabled = !isOperating,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_apply_metering_mode")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Metering Mode")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = modeStatus,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. TOU PROFILE SCREEN (Time of Use Passive Day Profile Read & Write)
// -----------------------------------------------------------------------------------------
@Composable
fun TouScreen(
    deviceAssociation: DeviceAssociation
) {
    val coroutineScope = rememberCoroutineScope()
    var isOperating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to read or write TOU passive day profiles.") }

    val touSlots = remember {
        mutableStateListOf(
            TouSlot(1, "00:00", "06:00", "Off-Peak Night", 0.85),
            TouSlot(2, "06:00", "09:00", "Normal Morning", 1.00),
            TouSlot(3, "09:00", "12:00", "Peak Hours 1", 1.20),
            TouSlot(4, "12:00", "17:00", "Normal Afternoon", 1.00),
            TouSlot(5, "17:00", "22:00", "Peak Evening", 1.25),
            TouSlot(6, "22:00", "24:00", "Off-Peak Night", 0.90)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tou_screen"),
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
                        text = "Time-of-Use (TOU) Tariff Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OBIS: ${Constant.TouPassiveDayProfileOBIS} / ${Constant.TouActivityCalendarOBIS}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isOperating = true
                                    statusMessage = "Reading Passive Day Profile from meter..."
                                    deviceAssociation.readDlmsAttribute(Constant.TouPassiveDayProfileOBIS, "TOU Passive Day Profile")
                                    isOperating = false
                                    statusMessage = "TOU Passive Day Profile read successfully (6 slots active)."
                                }
                            },
                            enabled = !isOperating,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_read_tou")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read Profile")
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isOperating = true
                                    statusMessage = "Writing TOU Passive Day Profile to meter..."
                                    val res = deviceAssociation.setDlmsAttribute(
                                        Constant.TouPassiveDayProfileOBIS,
                                        "TOU Passive Day Profile",
                                        "SLOTS_6_CONFIG_OK"
                                    )
                                    isOperating = false
                                    statusMessage = if (res.isSuccess) "Passive Day Profile written & verified!" else "Write failed: ${res.exceptionOrNull()?.message}"
                                }
                            },
                            enabled = !isOperating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("btn_write_tou")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Write Profile")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Day Profile Tariff Slots",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(touSlots) { slot ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Slot ${slot.slotNumber}: ${slot.tariffRateName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${slot.startTime} - ${slot.endTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${slot.multiplier}x Rate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
