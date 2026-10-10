package com.example.idatum.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.idatum.data.IDatumRepository
import com.example.idatum.dlms.AssociationState
import com.example.idatum.dlms.DeviceAssociation
import com.example.idatum.model.Discom
import com.example.idatum.ui.components.IDatumTopBar
import com.example.idatum.ui.screens.*
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val READ_METER = "read_meter"
    const val RECONNECT = "reconnect"
    const val MD_RESET = "md_reset"
    const val METERING_MODE = "metering_mode"
    const val TOU = "tou"
    const val COMMISSIONING = "commissioning"
    const val PING_CHECK = "ping_check"
    const val READ_NIC = "read_nic"
    const val TICKETS = "tickets"
    const val UPLOAD_FILES = "upload_files"
    const val LOGS = "logs"
    const val LOCATION = "location"
    const val SETTINGS = "settings"
    const val HELP = "help"
}

@Composable
fun IDatumApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { IDatumRepository(context) }
    val deviceAssociation = remember { DeviceAssociation.getInstance(context) }

    // Seed tickets on startup
    LaunchedEffect(Unit) {
        repository.seedInitialTicketsIfEmpty()
    }

    val probeState by deviceAssociation.associationState.collectAsState()
    val connectionInfo by deviceAssociation.connectionInfo.collectAsState()

    val tickets by repository.getAllTickets().collectAsState(initial = emptyList())
    val syncItems by repository.getAllSyncItems().collectAsState(initial = emptyList())
    val auditLogs by repository.getLatestAuditLogs().collectAsState(initial = emptyList())

    val pendingSyncCount = syncItems.count { it.status == "Pending" }

    var currentScreen by remember { mutableStateOf(Routes.HOME) }
    var isAuthenticated by remember { mutableStateOf(true) } // Default authenticated for field operations
    var currentOfficerId by remember { mutableStateOf("TECH_7721") }
    var currentDiscom by remember { mutableStateOf(Discom.SBPDCL) }

    var selectedMeterNumber by remember { mutableStateOf("") }
    var activeTicketId by remember { mutableStateOf<String?>(null) }

    var showProbeDialog by remember { mutableStateOf(false) }

    // Handle back button navigation
    if (currentScreen != Routes.HOME) {
        BackHandler {
            currentScreen = Routes.HOME
        }
    }

    if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = { user, discom ->
                currentOfficerId = user
                currentDiscom = discom
                isAuthenticated = true
                currentScreen = Routes.HOME
            }
        )
        return
    }

    // Quick Probe Connect Dialog
    if (showProbeDialog) {
        AlertDialog(
            onDismissRequest = { showProbeDialog = false },
            title = { Text("Optical Probe Hardware Status") },
            text = {
                Column {
                    Text("Selected Port: ${connectionInfo.interfaceType.name}")
                    Text("Baud Rate: ${connectionInfo.baudRate} bps")
                    Text("State: ${probeState.name}")
                    Text("Target Meter: ${connectionInfo.meterSerialNumber}")
                    Text("Security Level: HLS Level 5 (GMAC)")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProbeDialog = false
                        coroutineScope.launch {
                            deviceAssociation.initializeConnection()
                        }
                    }
                ) {
                    Text(if (probeState == AssociationState.AUTHENTICATED) "Re-Connect" else "Connect Probe")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProbeDialog = false
                        coroutineScope.launch {
                            deviceAssociation.disconnect()
                        }
                    }
                ) {
                    Text("Disconnect")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            IDatumTopBar(
                title = when (currentScreen) {
                    Routes.HOME -> "i-DATUM"
                    Routes.READ_METER -> "Read Meter"
                    Routes.RECONNECT -> "Relay Reconnect"
                    Routes.MD_RESET -> "MD Reset"
                    Routes.METERING_MODE -> "Metering Mode"
                    Routes.TOU -> "TOU Profile"
                    Routes.COMMISSIONING -> "CT Commissioning"
                    Routes.PING_CHECK -> "Meter Ping & Health"
                    Routes.READ_NIC -> "Read Cellular NIC"
                    Routes.TICKETS -> "WFM Tickets"
                    Routes.UPLOAD_FILES -> "Upload Files"
                    Routes.LOGS -> "DLMS APDU Logs"
                    Routes.LOCATION -> "GIS Location"
                    Routes.SETTINGS -> "Settings"
                    Routes.HELP -> "Help & Specs"
                    else -> "i-DATUM"
                },
                subtitle = when (currentScreen) {
                    Routes.HOME -> "${currentDiscom.code} • $currentOfficerId"
                    else -> "Connected: ${connectionInfo.meterSerialNumber}"
                },
                showBackButton = (currentScreen != Routes.HOME),
                onBackClick = { currentScreen = Routes.HOME },
                probeState = probeState,
                probeType = connectionInfo.interfaceType,
                pendingSyncCount = pendingSyncCount,
                onProbeClick = { showProbeDialog = true },
                onSyncClick = { currentScreen = Routes.UPLOAD_FILES }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = (currentScreen == Routes.HOME),
                    onClick = { currentScreen = Routes.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = (currentScreen == Routes.TICKETS),
                    onClick = { currentScreen = Routes.TICKETS },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Tickets") },
                    label = { Text("Tickets") },
                    modifier = Modifier.testTag("nav_tickets")
                )
                NavigationBarItem(
                    selected = (currentScreen == Routes.UPLOAD_FILES),
                    onClick = { currentScreen = Routes.UPLOAD_FILES },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingSyncCount > 0) Badge { Text("$pendingSyncCount") }
                            }
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync")
                        }
                    },
                    label = { Text("Sync") },
                    modifier = Modifier.testTag("nav_sync")
                )
                NavigationBarItem(
                    selected = (currentScreen == Routes.LOGS),
                    onClick = { currentScreen = Routes.LOGS },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Logs") },
                    label = { Text("Logs") },
                    modifier = Modifier.testTag("nav_logs")
                )
                NavigationBarItem(
                    selected = (currentScreen == Routes.SETTINGS),
                    onClick = { currentScreen = Routes.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Routes.HOME -> HomeScreen(
                    connectionInfo = connectionInfo,
                    probeState = probeState,
                    pendingSyncCount = pendingSyncCount,
                    onNavigate = { routeId ->
                        when (routeId) {
                            "read_meter" -> currentScreen = Routes.READ_METER
                            "reconnect" -> currentScreen = Routes.RECONNECT
                            "md_reset" -> currentScreen = Routes.MD_RESET
                            "metering_mode" -> currentScreen = Routes.METERING_MODE
                            "tou" -> currentScreen = Routes.TOU
                            "commissioning" -> currentScreen = Routes.COMMISSIONING
                            "ping_check" -> currentScreen = Routes.PING_CHECK
                            "read_nic" -> currentScreen = Routes.READ_NIC
                            "tickets" -> currentScreen = Routes.TICKETS
                            "upload_files" -> currentScreen = Routes.UPLOAD_FILES
                            "logs" -> currentScreen = Routes.LOGS
                            "location" -> currentScreen = Routes.LOCATION
                        }
                    },
                    onQuickConnect = {
                        coroutineScope.launch {
                            deviceAssociation.initializeConnection()
                        }
                    }
                )

                Routes.READ_METER -> ReadMeterScreen(
                    meterNumber = selectedMeterNumber,
                    deviceAssociation = deviceAssociation,
                    onSaveSuccess = { readModel ->
                        coroutineScope.launch {
                            repository.saveManualRead(readModel, activeTicketId)
                            Toast.makeText(context, "Readout saved and queued for sync!", Toast.LENGTH_SHORT).show()
                            currentScreen = Routes.HOME
                        }
                    }
                )

                Routes.RECONNECT -> ReconnectScreen(
                    deviceAssociation = deviceAssociation
                )

                Routes.MD_RESET -> MdResetScreen(
                    deviceAssociation = deviceAssociation
                )

                Routes.METERING_MODE -> MeteringModeScreen(
                    deviceAssociation = deviceAssociation
                )

                Routes.TOU -> TouScreen(
                    deviceAssociation = deviceAssociation
                )

                Routes.COMMISSIONING -> CommissioningScreen(
                    meterNumber = selectedMeterNumber,
                    deviceAssociation = deviceAssociation,
                    onSaveSuccess = { commRecord ->
                        coroutineScope.launch {
                            repository.saveCommissioningRecord(commRecord)
                            Toast.makeText(context, "Commissioning report generated & queued for sync!", Toast.LENGTH_SHORT).show()
                            currentScreen = Routes.HOME
                        }
                    }
                )

                Routes.PING_CHECK, Routes.READ_NIC -> PingCheckScreen(
                    deviceAssociation = deviceAssociation
                )

                Routes.TICKETS -> TicketScreen(
                    tickets = tickets,
                    onSelectTicketForRead = { ticket ->
                        selectedMeterNumber = ticket.meterNumber
                        activeTicketId = ticket.ticketId
                        currentScreen = Routes.READ_METER
                    },
                    onSelectTicketForReconnect = { ticket ->
                        selectedMeterNumber = ticket.meterNumber
                        activeTicketId = ticket.ticketId
                        currentScreen = Routes.RECONNECT
                    }
                )

                Routes.UPLOAD_FILES -> UploadFilesScreen(
                    syncItems = syncItems,
                    onSyncAll = {
                        coroutineScope.launch {
                            val count = repository.syncAllPending()
                            Toast.makeText(context, "Synchronized $count records to MDM!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Routes.LOGS -> LogsScreen(
                    logs = auditLogs,
                    onClearLogs = {
                        coroutineScope.launch {
                            repository.clearAuditLogs()
                        }
                    }
                )

                Routes.LOCATION -> LocationScreen()

                Routes.SETTINGS -> SettingsScreen(
                    deviceAssociation = deviceAssociation,
                    onClearOfflineFiles = {
                        coroutineScope.launch {
                            val count = repository.clearOfflineFiles()
                            Toast.makeText(context, "Deleted $count cached files.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Routes.HELP -> HelpScreen()
            }
        }
    }
}
