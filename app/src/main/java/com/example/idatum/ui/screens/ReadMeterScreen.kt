package com.example.idatum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idatum.dlms.DeviceAssociation
import com.example.idatum.model.Constant
import com.example.idatum.model.ManualReadModel
import com.example.idatum.model.ProfileItem
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
fun ReadMeterScreen(
    meterNumber: String,
    deviceAssociation: DeviceAssociation,
    onSaveSuccess: (ManualReadModel) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentMeterNumber by remember { mutableStateOf(meterNumber.ifEmpty { "INT202409841" }) }
    var isReading by remember { mutableStateOf(false) }
    var readStatusMessage by remember { mutableStateOf("Ready to read DLMS parameters.") }

    // Measured registers
    var kwhImport by remember { mutableStateOf("1248.65") }
    var kwhExport by remember { mutableStateOf("0.00") }
    var kvahImport by remember { mutableStateOf("1312.40") }
    var kvahExport by remember { mutableStateOf("0.00") }
    var kwMD by remember { mutableStateOf("4.82") }
    var kvaMD by remember { mutableStateOf("5.16") }
    var meterClock by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) }
    var remarks by remember { mutableStateOf("Routine field inspection - display normal") }

    // Image attachments indicators
    var hasKwMdPhoto by remember { mutableStateOf(false) }
    var hasKvaMdPhoto by remember { mutableStateOf(false) }

    // Profiles from Section 7 readout.json
    val profiles = remember {
        mutableStateListOf(
            ProfileItem("1", isEnable = true, isChecked = true, scaler = "1.0.94.91.6.255", profile = "1.0.98.1.0.255", objectName = "Billing Profile Data", shortName = "BP"),
            ProfileItem("2", isEnable = true, isChecked = true, scaler = "1.0.94.91.5.255", profile = "1.0.99.2.0.255", objectName = "Daily Load Profile", shortName = "DP"),
            ProfileItem("3", isEnable = true, isChecked = true, scaler = "1.0.94.91.3.255", profile = "1.0.94.91.0.255", objectName = "Instantaneous Profile", shortName = "IP"),
            ProfileItem("4", isEnable = true, isChecked = false, scaler = "1.0.94.91.4.255", profile = "1.0.99.1.0.255", objectName = "Block Load Profile", shortName = "LP"),
            ProfileItem("6", isEnable = true, isChecked = false, scaler = "", profile = "0.0.94.91.10.255", objectName = "Name Plate Profile", shortName = "NP")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("read_meter_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Meter Serial Input & Read Trigger Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Meter Identification & Association",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentMeterNumber,
                        onValueChange = { currentMeterNumber = it },
                        label = { Text("Meter Serial Number") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_meter_number"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isReading = true
                                    readStatusMessage = "Establishing DLMS Association..."
                                    deviceAssociation.setMeterSerial(currentMeterNumber)
                                    deviceAssociation.initializeConnection { readStatusMessage = it }

                                    readStatusMessage = "Reading Billing & Instantaneous registers..."
                                    kwhImport = deviceAssociation.readDlmsAttribute(Constant.KwhImportOBIS, "kWh Import")
                                    kvahImport = deviceAssociation.readDlmsAttribute(Constant.KvahImportOBIS, "kVAh Import")
                                    kwMD = deviceAssociation.readDlmsAttribute(Constant.KwMDOBIS, "kW MD")
                                    kvaMD = deviceAssociation.readDlmsAttribute(Constant.KvaMDOBIS, "kVA MD")
                                    meterClock = deviceAssociation.readDlmsAttribute(Constant.ClockOBIS, "Meter RTC")

                                    isReading = false
                                    readStatusMessage = "Readout completed successfully."
                                }
                            },
                            enabled = !isReading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_read_data")
                        ) {
                            if (isReading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reading...")
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Read Data")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = readStatusMessage,
                        fontSize = 12.sp,
                        color = if (isReading) MaterialTheme.colorScheme.primary else FieldEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Target Profiles Selector
        item {
            Text(
                text = "Target DLMS Profiles (readout.json)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(profiles) { profile ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { profile.isChecked = !profile.isChecked }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = profile.isChecked,
                            onCheckedChange = { profile.isChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${profile.shortName} - ${profile.objectName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "OBIS: ${profile.profile}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (profile.scaler.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Scaled",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recovered Measured Registers (Section 6)
        item {
            Text(
                text = "Captured Meter Registers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricValueCard(
                    label = "kWh Import",
                    value = kwhImport,
                    unit = "kWh",
                    obis = Constant.KwhImportOBIS,
                    icon = Icons.Default.ElectricBolt,
                    colorAccent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricValueCard(
                    label = "kVAh Import",
                    value = kvahImport,
                    unit = "kVAh",
                    obis = Constant.KvahImportOBIS,
                    icon = Icons.Default.FlashOn,
                    colorAccent = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricValueCard(
                    label = "kW Max Demand (MD)",
                    value = kwMD,
                    unit = "kW",
                    obis = Constant.KwMDOBIS,
                    icon = Icons.Default.TrendingUp,
                    colorAccent = FieldAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricValueCard(
                    label = "kVA Max Demand (MD)",
                    value = kvaMD,
                    unit = "kVA",
                    obis = Constant.KvaMDOBIS,
                    icon = Icons.Default.Speed,
                    colorAccent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Photo Proofs & Remarks
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Image Proofs & Audit Evidence",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { hasKwMdPhoto = !hasKwMdPhoto },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_kw_md_photo")
                        ) {
                            Icon(
                                imageVector = if (hasKwMdPhoto) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (hasKwMdPhoto) FieldEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasKwMdPhoto) "kW MD Proof ✓" else "Capture kW MD")
                        }

                        OutlinedButton(
                            onClick = { hasKvaMdPhoto = !hasKvaMdPhoto },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_kva_md_photo")
                        ) {
                            Icon(
                                imageVector = if (hasKvaMdPhoto) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (hasKvaMdPhoto) FieldEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasKvaMdPhoto) "kVA MD Proof ✓" else "Capture kVA MD")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Field Remarks / Audit Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_remarks"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val readRecord = ManualReadModel(
                                meterNumber = currentMeterNumber,
                                meterDTTM = meterClock,
                                kwhImport = kwhImport,
                                kvahImport = kvahImport,
                                kwhExport = kwhExport,
                                kvahExport = kvahExport,
                                kwMD = kwMD,
                                kvaMD = kvaMD,
                                kwMDImage = if (hasKwMdPhoto) "photo_kw_md_${currentMeterNumber}.jpg" else "",
                                kvaMDImage = if (hasKvaMdPhoto) "photo_kva_md_${currentMeterNumber}.jpg" else "",
                                remarks = remarks,
                                audit = "Authenticated HLS Level 5 | Invocation #${deviceAssociation.connectionInfo.value.invocationCounterMR}"
                            )
                            onSaveSuccess(readRecord)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_submit_readout")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Offline & Queue for MDM Sync")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
