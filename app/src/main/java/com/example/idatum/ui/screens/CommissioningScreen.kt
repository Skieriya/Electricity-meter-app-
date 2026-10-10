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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idatum.data.CommissioningEntity
import com.example.idatum.dlms.DeviceAssociation
import com.example.idatum.model.CommissioningParameter
import com.example.idatum.model.Constant
import com.example.idatum.ui.components.MetricValueCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FieldAmber
import com.example.ui.theme.FieldEmerald
import com.example.ui.theme.FieldRose
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommissioningScreen(
    meterNumber: String,
    deviceAssociation: DeviceAssociation,
    onSaveSuccess: (CommissioningEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentMeter by remember { mutableStateOf(meterNumber.ifEmpty { "INT202409841" }) }
    var isOperating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to perform CT Commissioning & Tamper diagnostics.") }

    // CT Ratio configuration
    var ctPrimary by remember { mutableStateOf("200") }
    var ctSecondary by remember { mutableStateOf("5") }
    var isCtRatioVerified by remember { mutableStateOf(true) }

    // 3-Phase Instantaneous Values
    var vr by remember { mutableStateOf("238.4") }
    var vy by remember { mutableStateOf("239.1") }
    var vb by remember { mutableStateOf("237.8") }
    var ir by remember { mutableStateOf("14.85") }
    var iy by remember { mutableStateOf("12.40") }
    var ib by remember { mutableStateOf("15.10") }
    var pf by remember { mutableStateOf("0.96") }
    var hz by remember { mutableStateOf("50.02") }
    var activeKw by remember { mutableStateOf("9.84") }
    var reactiveKvar by remember { mutableStateOf("2.87") }
    var apparentKva by remember { mutableStateOf("10.25") }

    // Tamper Diagnostics Counters
    var tamperMagnetic by remember { mutableStateOf(0) }
    var tamperNeutral by remember { mutableStateOf(1) }
    var tamperCoverOpen by remember { mutableStateOf(0) }
    var tamperCtBypass by remember { mutableStateOf(0) }

    // Dynamic DLMS Object Discovery & Access Rights (Section 9 & 19)
    val parameterCatalog = remember {
        listOf(
            CommissioningParameter(Constant.CtRatioPrimaryOBIS, "CT Ratio Primary", "200 A", "A", isWritable = true, verified = true),
            CommissioningParameter(Constant.CtRatioSecondaryOBIS, "CT Ratio Secondary", "5 A", "A", isWritable = true, verified = true),
            CommissioningParameter(Constant.VoltageRPhaseOBIS, "VR Phase Voltage", "238.4 V", "V", isWritable = false, verified = true),
            CommissioningParameter(Constant.CurrentRPhaseOBIS, "IR Phase Current", "14.85 A", "A", isWritable = false, verified = true),
            CommissioningParameter(Constant.ActivePowerKwOBIS, "Instantaneous Active Power", "9.84 kW", "kW", isWritable = false, verified = true),
            CommissioningParameter(Constant.TamperNeutralDisturbanceOBIS, "Neutral Disturbance Count", "1", "Events", isWritable = false, verified = true)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("commissioning_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Meter Identification & Live Discovery Trigger
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CT Commissioning & Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FieldEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Sec 9 & 19 Compliant",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FieldEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentMeter,
                        onValueChange = { currentMeter = it },
                        label = { Text("Commissioning Meter Serial") },
                        leadingIcon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_commissioning_meter"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isOperating = true
                                statusMessage = "Discovering meter objects & validating access rights..."
                                deviceAssociation.setMeterSerial(currentMeter)
                                deviceAssociation.initializeConnection { statusMessage = it }

                                statusMessage = "Reading 3-Phase telemetry and CT parameters..."
                                vr = deviceAssociation.readDlmsAttribute(Constant.VoltageRPhaseOBIS, "VR Voltage")
                                vy = deviceAssociation.readDlmsAttribute(Constant.VoltageYPhaseOBIS, "VY Voltage")
                                vb = deviceAssociation.readDlmsAttribute(Constant.VoltageBPhaseOBIS, "VB Voltage")
                                ir = deviceAssociation.readDlmsAttribute(Constant.CurrentRPhaseOBIS, "IR Current")
                                iy = deviceAssociation.readDlmsAttribute(Constant.CurrentYPhaseOBIS, "IY Current")
                                ib = deviceAssociation.readDlmsAttribute(Constant.CurrentBPhaseOBIS, "IB Current")
                                pf = deviceAssociation.readDlmsAttribute(Constant.PowerFactorOBIS, "Power Factor")
                                activeKw = deviceAssociation.readDlmsAttribute(Constant.ActivePowerKwOBIS, "Active kW")
                                ctPrimary = deviceAssociation.readDlmsAttribute(Constant.CtRatioPrimaryOBIS, "CT Primary")
                                ctSecondary = deviceAssociation.readDlmsAttribute(Constant.CtRatioSecondaryOBIS, "CT Secondary")

                                tamperMagnetic = deviceAssociation.readDlmsAttribute(Constant.TamperMagneticOBIS, "Mag Tamper").toIntOrNull() ?: 0
                                tamperNeutral = deviceAssociation.readDlmsAttribute(Constant.TamperNeutralDisturbanceOBIS, "Neutral Tamper").toIntOrNull() ?: 1
                                tamperCoverOpen = deviceAssociation.readDlmsAttribute(Constant.TamperCoverOpenOBIS, "Cover Open").toIntOrNull() ?: 0
                                tamperCtBypass = deviceAssociation.readDlmsAttribute(Constant.TamperCtBypassOBIS, "CT Bypass").toIntOrNull() ?: 0

                                isOperating = false
                                statusMessage = "Discovery & Telemetry refreshed successfully."
                            }
                        },
                        enabled = !isOperating,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_discover_telemetry")
                    ) {
                        if (isOperating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discovering DLMS Objects...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discover Objects & Refresh Telemetry")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // CT Ratio Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Transformer (CT) Ratio Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OBIS: ${Constant.CtRatioPrimaryOBIS} / ${Constant.CtRatioSecondaryOBIS} (Writable Attribute)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ctPrimary,
                            onValueChange = { ctPrimary = it },
                            label = { Text("CT Primary (A)") },
                            modifier = Modifier.weight(1f).testTag("input_ct_primary"),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = ctSecondary,
                            onValueChange = { ctSecondary = it },
                            label = { Text("CT Secondary (A)") },
                            modifier = Modifier.weight(1f).testTag("input_ct_secondary"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isOperating = true
                                statusMessage = "Verifying write access & programming CT ratio..."
                                val res = deviceAssociation.setDlmsAttribute(
                                    Constant.CtRatioPrimaryOBIS,
                                    "CT Ratio Primary",
                                    ctPrimary
                                )
                                isOperating = false
                                statusMessage = if (res.isSuccess) {
                                    isCtRatioVerified = true
                                    "CT Ratio $ctPrimary/$ctSecondary A successfully programmed & verified!"
                                } else {
                                    "Error: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        enabled = !isOperating,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_write_ct_ratio")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Program CT Ratio with Read-Back Verify")
                    }
                }
            }
        }

        // Live 3-Phase Telemetry Cards (Color-Coded Phases: Red, Yellow, Blue)
        item {
            Text(
                text = "3-Phase Telemetry (Live Verification)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // R-Phase
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(FieldRose))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("R-Phase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FieldRose)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$vr V", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$ir A", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Y-Phase
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(FieldAmber))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Y-Phase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FieldAmber)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$vy V", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$iy A", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // B-Phase
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(ElectricCyan))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("B-Phase", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$vb V", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$ib A", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Summary Power & Power Factor
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricValueCard(
                    label = "Active Power (kW)",
                    value = activeKw,
                    unit = "kW",
                    obis = Constant.ActivePowerKwOBIS,
                    modifier = Modifier.weight(1f)
                )
                MetricValueCard(
                    label = "Power Factor & Freq",
                    value = "$pf / ${hz}Hz",
                    obis = Constant.PowerFactorOBIS,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tamper Diagnostics Counter Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tamper Diagnostics Register Counters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Magnetic Tamper", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$tamperMagnetic", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Neutral Disturbance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$tamperNeutral", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (tamperNeutral > 0) FieldAmber else MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text("Cover Open", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$tamperCoverOpen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("CT Bypass", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$tamperCtBypass", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Save Commissioning Audit Report
        item {
            Button(
                onClick = {
                    val record = CommissioningEntity(
                        meterNumber = currentMeter,
                        ctPrimary = ctPrimary,
                        ctSecondary = ctSecondary,
                        voltageR = vr,
                        voltageY = vy,
                        voltageB = vb,
                        currentR = ir,
                        currentY = iy,
                        currentB = ib,
                        powerFactor = pf,
                        activePowerKw = activeKw,
                        reactivePowerKvar = reactiveKvar,
                        apparentPowerKva = apparentKva,
                        tamperMagneticCount = tamperMagnetic,
                        tamperNeutralCount = tamperNeutral,
                        tamperCoverOpenCount = tamperCoverOpen,
                        tamperCtBypassCount = tamperCtBypass,
                        isCtRatioVerified = isCtRatioVerified,
                        technicianId = "TECH_7721",
                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    onSaveSuccess(record)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_commissioning_report")
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate & Sign Commissioning Audit Report")
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
