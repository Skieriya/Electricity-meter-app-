package com.example.idatum.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.idatum.data.AuditLogEntity
import com.example.idatum.data.SyncQueueEntity
import com.example.idatum.data.TicketEntity
import com.example.idatum.dlms.DeviceAssociation
import com.example.idatum.model.MeterDataModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FieldAmber
import com.example.ui.theme.FieldEmerald
import com.example.ui.theme.FieldRose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------------------
// 1. VIEW TICKETS SCREEN (WFM Ticket Workflow)
// -----------------------------------------------------------------------------------------
@Composable
fun TicketScreen(
    tickets: List<TicketEntity>,
    onSelectTicketForRead: (TicketEntity) -> Unit,
    onSelectTicketForReconnect: (TicketEntity) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filtered = when (selectedFilter) {
        "Pending" -> tickets.filter { it.status == "Pending" }
        "Completed" -> tickets.filter { it.status == "Completed" }
        else -> tickets
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("ticket_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Completed").forEach { filter ->
                    FilterChip(
                        selected = (selectedFilter == filter),
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tickets found for '$selectedFilter'", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        items(filtered) { ticket ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("ticket_card_${ticket.ticketId}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ticket.ticketId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (ticket.status == "Completed") FieldEmerald.copy(alpha = 0.15f) else FieldAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = ticket.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ticket.status == "Completed") FieldEmerald else FieldAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Meter: ${ticket.meterNumber} (${ticket.meterMake})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Consumer: ${ticket.consumerName} (${ticket.consumerNumber})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Subdivision: ${ticket.subDivision}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Event: ${ticket.eventType}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectTicketForRead(ticket) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_ticket_read_${ticket.ticketId}")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read Meter", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSelectTicketForReconnect(ticket) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_ticket_reconnect_${ticket.ticketId}")
                        ) {
                            Icon(Icons.Default.Power, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconnect", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. UPLOAD FILES / OFFLINE SYNC SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun UploadFilesScreen(
    syncItems: List<SyncQueueEntity>,
    onSyncAll: () -> Unit
) {
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var syncMessage by remember { mutableStateOf("Pending offline records waiting for MDM synchronization.") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("upload_files_screen"),
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
                        text = "MDM Offline Synchronization Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Collected Readout, Commissioning & Relay action records are stored locally with AES-256 encryption.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                syncMessage = "Uploading queued packets to MDM REST endpoints..."
                                delay(900)
                                onSyncAll()
                                isSyncing = false
                                syncMessage = "All pending records successfully synchronized with MDM!"
                            }
                        },
                        enabled = !isSyncing && syncItems.any { it.status == "Pending" },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_sync_all")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchronizing to MDM...")
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync All Pending Records (${syncItems.count { it.status == "Pending" }})")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Queue History (${syncItems.size} items)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (syncItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records in sync queue. Create a reading to test.", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        items(syncItems) { item ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = item.itemType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = item.referenceId, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = item.payloadJson,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = item.createdAt, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.status == "Synced") FieldEmerald.copy(alpha = 0.15f) else FieldAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.status == "Synced") FieldEmerald else FieldAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. VIEW LOGS / DLMS APDU PACKET INSPECTOR
// -----------------------------------------------------------------------------------------
@Composable
fun LogsScreen(
    logs: List<AuditLogEntity>,
    onClearLogs: () -> Unit
) {
    var filterDirection by remember { mutableStateOf("All") }
    val filtered = when (filterDirection) {
        "TX" -> logs.filter { it.direction == "TX" }
        "RX" -> logs.filter { it.direction == "RX" }
        else -> logs
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("logs_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "TX", "RX").forEach { dir ->
                        FilterChip(
                            selected = (filterDirection == dir),
                            onClick = { filterDirection = dir },
                            label = { Text(dir) }
                        )
                    }
                }

                TextButton(onClick = onClearLogs) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No APDU logs recorded. Run an operation to capture packets.", color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        items(filtered) { log ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (log.direction == "TX") MaterialTheme.colorScheme.primaryContainer else FieldEmerald.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = log.direction,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.direction == "TX") MaterialTheme.colorScheme.onPrimaryContainer else FieldEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${log.protocol} | ${log.apduType}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(text = log.timestamp, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = log.summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                    if (log.hexBytes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.hexBytes,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. METER PING / CHECK / READ NIC SCREEN
// -----------------------------------------------------------------------------------------
@Composable
fun PingCheckScreen(
    deviceAssociation: DeviceAssociation
) {
    val coroutineScope = rememberCoroutineScope()
    var meterIp by remember { mutableStateOf("10.12.84.195") }
    var isPinging by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf("Latency: 38 ms | Packets: 4/4 Received (0% Loss) | RSSI: -68 dBm") }
    var isCheckingHealth by remember { mutableStateOf(false) }

    var meterHealth by remember {
        mutableStateOf(
            MeterDataModel(
                serialNumber = "INT202409841",
                rtcDrift = "+2 seconds",
                coverOpenStatus = "Normal (Closed)",
                meterHealthStatus = "Healthy",
                relayStatus = "Connected",
                rtcBatteryStatus = "Good (3.62V)",
                status = "All Internal Checks Passed"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("ping_check_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ping Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Meter Ping (NIC IP Communication)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = meterIp,
                        onValueChange = { meterIp = it },
                        label = { Text("Meter NIC IPv4 / IPv6 Address") },
                        modifier = Modifier.fillMaxWidth().testTag("input_ping_ip"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isPinging = true
                                pingResult = "Pinging $meterIp (ICMP / DLMS Ping)..."
                                delay(600)
                                isPinging = false
                                pingResult = "Response from $meterIp: bytes=32 time=32ms TTL=54 RSSI=-65 dBm"
                            }
                        },
                        enabled = !isPinging,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_ping_meter")
                    ) {
                        Icon(Icons.Default.NetworkPing, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPinging) "Pinging..." else "Ping Meter NIC")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = pingResult, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Meter Health Diagnostic Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meter Internal Health Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isCheckingHealth = true
                                    delay(500)
                                    isCheckingHealth = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Health")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RTC Clock Drift:", fontSize = 13.sp)
                        Text(meterHealth.rtcDrift, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FieldEmerald)
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RTC Battery Status:", fontSize = 13.sp)
                        Text(meterHealth.rtcBatteryStatus, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FieldEmerald)
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terminal Cover Status:", fontSize = 13.sp)
                        Text(meterHealth.coverOpenStatus, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overall Diagnostic:", fontSize = 13.sp)
                        Text(meterHealth.meterHealthStatus, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FieldEmerald)
                    }
                }
            }
        }

        // Qualcomm Cellular NIC Module Card (Section 4 ReadMeterNicView)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Qualcomm Cellular NIC Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("IMEI: 864920048192019", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("ICCID: 8991820491820194812", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("APN: iot.airtel.com / static", fontSize = 12.sp)
                    Text("Network Mode: NB-IoT / LTE-M (Band 3/8)", fontSize = 12.sp)
                    Text("Signal Strength: -68 dBm (Excellent)", fontSize = 12.sp, color = FieldEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
