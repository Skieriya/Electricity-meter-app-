package com.example.idatum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.idatum.dlms.AssociationState
import com.example.idatum.dlms.MeterConnectionInfo
import com.example.ui.theme.*

data class HomeTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val testTag: String
)

@Composable
fun HomeScreen(
    connectionInfo: MeterConnectionInfo,
    probeState: AssociationState,
    pendingSyncCount: Int,
    onNavigate: (String) -> Unit,
    onQuickConnect: () -> Unit
) {
    val tiles = listOf(
        HomeTile("read_meter", "Read Meter", "Instantaneous, BP, LP", Icons.Default.Speed, LightPrimary, "tile_read_meter"),
        HomeTile("reconnect", "Reconnect", "Relay Connect / Disconnect", Icons.Default.Power, FieldEmerald, "tile_reconnect"),
        HomeTile("md_reset", "MD Reset", "Demand Reset OBIS 0.0.10", Icons.Default.RestartAlt, FieldAmber, "tile_md_reset"),
        HomeTile("metering_mode", "Metering Mode", "Prepaid / Postpaid / Tamper", Icons.Default.Tune, ElectricTeal, "tile_metering_mode"),
        HomeTile("tou", "TOU Profile", "Passive Day Tariffs", Icons.Default.Schedule, Color(0xFF7C3AED), "tile_tou"),
        HomeTile("commissioning", "CT Commission", "Ratio, Telemetry & Tamper", Icons.Default.PrecisionManufacturing, Color(0xFF0284C7), "tile_commissioning"),
        HomeTile("ping_check", "Meter Ping/Check", "NIC Ping, RTC Drift, Batt", Icons.Default.NetworkPing, Color(0xFF0D9488), "tile_ping_check"),
        HomeTile("read_nic", "Read NIC", "Qualcomm Cellular Module", Icons.Default.SimCard, Color(0xFF4F46E5), "tile_read_nic"),
        HomeTile("tickets", "View Tickets", "WFM Orders & Workflows", Icons.Default.Assignment, Color(0xFFD97706), "tile_tickets"),
        HomeTile("upload_files", "Upload Files", "Offline MDM Sync Queue", Icons.Default.CloudSync, FieldEmerald, "tile_upload_files"),
        HomeTile("logs", "View Logs", "DLMS HDLC APDU Inspector", Icons.Default.Terminal, Color(0xFF334155), "tile_logs"),
        HomeTile("location", "Location / Map", "GIS Survey & Navigation", Icons.Default.LocationOn, Color(0xFFE11D48), "tile_location")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Visual Card
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.idatum_hero_banner_1791470030526),
                        contentDescription = "i-DATUM Optical Probe Telemetry",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xDD0A2540))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xAA000000)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HHU PROBE: ${connectionInfo.interfaceType.name}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Meter: ${connectionInfo.meterSerialNumber}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Status: ${connectionInfo.hlsStatus}",
                                    color = Color(0xFFCBE6FF),
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = onQuickConnect,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (probeState == AssociationState.AUTHENTICATED) FieldEmerald else ElectricTeal
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("home_quick_connect_btn")
                            ) {
                                Icon(
                                    imageVector = if (probeState == AssociationState.AUTHENTICATED) Icons.Default.Check else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (probeState == AssociationState.AUTHENTICATED) "Active" else "Connect",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Telemetry Strip
        item(span = { GridItemSpan(2) }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Relay", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = connectionInfo.relayState,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (connectionInfo.relayState.startsWith("Connected")) FieldEmerald else FieldAmber
                        )
                    }
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = connectionInfo.meteringMode.take(10), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Inv. Counter", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "#${connectionInfo.invocationCounterMR}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Header for Operations Grid
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Field Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "DISCOM Compliant",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Operation Grid Tiles
        items(tiles) { tile ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigate(tile.id) }
                    .testTag(tile.testTag)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tile.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = tile.title,
                            tint = tile.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tile.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tile.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Bottom spacer
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
