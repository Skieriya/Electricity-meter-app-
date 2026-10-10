package com.example.idatum.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.idatum.model.Discom
import com.example.idatum.service.RootDetectionService
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.FieldEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (userId: String, discom: Discom) -> Unit
) {
    var userId by remember { mutableStateOf("TECH_7721") }
    var password by remember { mutableStateOf("IntelliSmart@2024") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var selectedDiscom by remember { mutableStateOf(Discom.SBPDCL) }
    var isDiscomExpanded by remember { mutableStateOf(false) }

    // Offline OTP Mode dialog
    var isOfflineOtpMode by remember { mutableStateOf(false) }
    var offlineOtp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val rootService = remember { RootDetectionService() }
    val isSecure = remember { !rootService.isDeviceRooted() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon & Name
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "i-DATUM",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Hand-Held Unit (HHU) Smart Utility v6.1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Integrity Badge
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSecure) FieldEmerald.copy(alpha = 0.15f) else Color(0x22DC2626)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSecure) FieldEmerald else Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSecure) "Device Verified & Secure" else "Root Detected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSecure) FieldEmerald else Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // DISCOM Selection
                    ExposedDropdownMenuBox(
                        expanded = isDiscomExpanded,
                        onExpandedChange = { isDiscomExpanded = !isDiscomExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedDiscom.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Utility / DISCOM") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDiscomExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_discom"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isDiscomExpanded,
                            onDismissRequest = { isDiscomExpanded = false }
                        ) {
                            Discom.values().forEach { discom ->
                                DropdownMenuItem(
                                    text = { Text(discom.displayName) },
                                    onClick = {
                                        selectedDiscom = discom
                                        isDiscomExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // User ID
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text("Field Officer / Technician ID") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_user_id"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it }
                            )
                            Text("Remember Me", fontSize = 12.sp)
                        }

                        TextButton(onClick = { isOfflineOtpMode = !isOfflineOtpMode }) {
                            Text(if (isOfflineOtpMode) "Normal Login" else "Offline OTP Login", fontSize = 12.sp)
                        }
                    }

                    if (isOfflineOtpMode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = offlineOtp,
                            onValueChange = { offlineOtp = it },
                            label = { Text("Supervisor 6-Digit OTP") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_offline_otp"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (userId.isEmpty() || password.isEmpty()) {
                                errorMessage = "Please enter both User ID and Password."
                            } else {
                                onLoginSuccess(userId, selectedDiscom)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login")
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In to HHU", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
