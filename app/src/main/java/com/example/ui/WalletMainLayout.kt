package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Document
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WalletMainLayout(viewModel: WalletViewModel) {
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isLocked) {
                WalletBottomNavigation(viewModel)
            }
        },
        containerColor = WalletDarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = isLocked,
                transitionSpec = {
                    slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                },
                label = "lock_transition"
            ) { locked ->
                if (locked) {
                    LockScreen(viewModel)
                } else {
                    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                    when (currentTab) {
                        "cards" -> CardsDashboard(viewModel, snackbarHostState)
                        "scanner" -> AuthoritiesScanView(viewModel)
                        "settings" -> SettingsScreen(viewModel, snackbarHostState)
                    }
                }
            }
        }
    }
}

/**
 * High-Security biometric & PIN lock panel
 */
@Composable
fun LockScreen(viewModel: WalletViewModel) {
    val currentPin by viewModel.currentPinInput.collectAsStateWithLifecycle()
    val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
    var showInstallQr by remember { mutableStateOf(false) }
    
    // Rotate and pulse animations for the simulated biometric scanner
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_ring")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle_rotation"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(24.dp)
            .testTag("lock_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Identity Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "App Shield Emblem",
                tint = VibrantPrimaryPurple,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "WALLET ID GR",
                color = VibrantBodyText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "GOVERNMENT SECURED LOGINS",
                color = VibrantSecondaryClay.copy(alpha = 0.7f),
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Biometric Choice Tabs & Hologram
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Choice Tabs (Fingerprint / Face ID)
            var selectedBiometricTab by remember { mutableStateOf("fingerprint") }
            
            Row(
                modifier = Modifier
                    .background(VibrantPurpleContainer.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .border(1.dp, VibrantPurpleBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("fingerprint" to "Fingerprint", "faceid" to "Face ID").forEach { (tabKey, tabLabel) ->
                    val isTabSelected = selectedBiometricTab == tabKey
                    val tabIcon = if (tabKey == "fingerprint") Icons.Default.Fingerprint else Icons.Default.Face
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isTabSelected) VibrantPrimaryPurple else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedBiometricTab = tabKey }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = tabLabel,
                                tint = if (isTabSelected) Color.White else VibrantSecondaryClay,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tabLabel,
                                color = if (isTabSelected) Color.White else VibrantSecondaryClay,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { viewModel.unlockWithBiometrics() }
                    .testTag("biometric_sensor_click"),
                contentAlignment = Alignment.Center
            ) {
                // outer scanning circle
                Box(
                    modifier = Modifier
                        .size(110.dp * scalePulse)
                        .border(
                            2.dp,
                            Brush.sweepGradient(listOf(VibrantPrimaryPurple, EmeraldShield, VibrantPrimaryPurple)),
                            CircleShape
                        )
                )

                // pulsing security scanner core
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(VibrantPurpleContainer, CircleShape)
                        .border(1.dp, VibrantPurpleBorder, CircleShape)
                        .clickable { viewModel.unlockWithBiometrics() },
                    contentAlignment = Alignment.Center
                ) {
                    val biometricIcon = if (selectedBiometricTab == "fingerprint") Icons.Default.Fingerprint else Icons.Default.Face
                    Icon(
                        imageVector = biometricIcon,
                        contentDescription = "Simulated Biometrics",
                        tint = VibrantPrimaryPurple,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            val biometricText = if (selectedBiometricTab == "fingerprint") {
                "TOUCH SENSOR TO SIMULATE FINGERPRINT SCAN"
            } else {
                "LOOK AT SCREEN TO SIMULATE FACE ID SCAN"
            }
            Text(
                text = biometricText,
                color = VibrantPrimaryPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { viewModel.unlockWithBiometrics() }
            )
        }

        // Numeric PIN Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PIN Display Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < currentPin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (filled) VibrantPrimaryPurple else Color.White,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (filled) Color.Transparent else VibrantBorderLight,
                                shape = CircleShape
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 10-Key Grid
            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('C', '0', '⌫')
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, VibrantBorderLight, CircleShape)
                                    .clickable {
                                        when (key) {
                                            'C' -> viewModel.lockApp()
                                            '⌫' -> viewModel.deletePinChar()
                                            else -> viewModel.appendPinChar(key)
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key.toString(),
                                    color = if (key == 'C' || key == '⌫') VibrantPrimaryPurple else VibrantBodyText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Default test passcode: $savedPin",
                color = VibrantSecondaryClay.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showInstallQr = true },
                modifier = Modifier.testTag("show_install_qr_lock_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = VibrantPrimaryPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "INSTALL ON MOBILE",
                    color = VibrantPrimaryPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            if (showInstallQr) {
                AlertDialog(
                    onDismissRequest = { showInstallQr = false },
                    title = {
                        Text(
                            text = "Install App on Mobile",
                            fontWeight = FontWeight.Bold,
                            color = VibrantBodyText
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Scan this QR code with your mobile or tablet camera to open the application and download the off-line secure APK installer immediately.",
                                color = VibrantSecondaryClay,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            QrCodeView(
                                payload = "https://ais-pre-ih4fqecs37eycxjovjtg7u-432268787034.europe-west2.run.app",
                                modifier = Modifier
                                    .size(180.dp)
                                    .border(1.dp, VibrantBorderLight, RoundedCornerShape(16.dp))
                            )
                            
                            Text(
                                text = "URL: https://ais-pre-ih4fqecs37eycxjovjtg7u-432268787034.europe-west2.run.app",
                                color = VibrantPrimaryPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInstallQr = false }) {
                            Text("CLOSE", fontWeight = FontWeight.Bold, color = VibrantPrimaryPurple)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

/**
 * Secondary Bottom Navigation Bar for authenticated sessions
 */
@Composable
fun WalletBottomNavigation(viewModel: WalletViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.border(1.dp, VibrantBorderLight, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            selected = currentTab == "cards",
            onClick = { viewModel.setTab("cards") },
            icon = { Icon(Icons.Default.CreditCard, contentDescription = "My Wallet ID Cards") },
            label = { Text("My Wallet", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VibrantPrimaryPurple,
                selectedTextColor = VibrantPrimaryPurple,
                unselectedIconColor = VibrantSubText.copy(alpha = 0.7f),
                unselectedTextColor = VibrantSubText.copy(alpha = 0.7f),
                indicatorColor = VibrantPurpleContainer
            ),
            modifier = Modifier.testTag("tab_cards_btn")
        )
        NavigationBarItem(
            selected = currentTab == "scanner",
            onClick = { viewModel.setTab("scanner") },
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Authority Inspection Scanner") },
            label = { Text("Scanner Panel", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VibrantPrimaryPurple,
                selectedTextColor = VibrantPrimaryPurple,
                unselectedIconColor = VibrantSubText.copy(alpha = 0.7f),
                unselectedTextColor = VibrantSubText.copy(alpha = 0.7f),
                indicatorColor = VibrantPurpleContainer
            ),
            modifier = Modifier.testTag("tab_scanner_btn")
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { viewModel.setTab("settings") },
            icon = { Icon(Icons.Default.Lock, contentDescription = "Authentication Settings") },
            label = { Text("Security", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VibrantPrimaryPurple,
                selectedTextColor = VibrantPrimaryPurple,
                unselectedIconColor = VibrantSubText.copy(alpha = 0.7f),
                unselectedTextColor = VibrantSubText.copy(alpha = 0.7f),
                indicatorColor = VibrantPurpleContainer
            ),
            modifier = Modifier.testTag("tab_settings_btn")
        )
    }
}

/**
 * Smart Dashboard listing all verified government cards
 */
@Composable
fun CardsDashboard(viewModel: WalletViewModel, snackbarHostState: SnackbarHostState) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val filteredDocs by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val selectedDocId by viewModel.selectedDocumentId.collectAsStateWithLifecycle()
    val isAddSheetOpen by viewModel.isAddSheetOpen.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Dashboard Header Title (Vibrant App Bar layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GOOD MORNING",
                        color = VibrantSecondaryClay.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Wallet id GR",
                        color = VibrantBodyText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lock Device Icon
                    IconButton(
                        onClick = { viewModel.lockApp() },
                        modifier = Modifier
                            .background(VibrantPurpleContainer, CircleShape)
                            .size(36.dp)
                            .testTag("dashboard_lock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Device Wallet",
                            tint = VibrantPrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // JD Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(VibrantPurpleContainer, CircleShape)
                            .border(2.dp, VibrantPurpleBorder, CircleShape)
                            .clickable { viewModel.lockApp() }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(VibrantPrimaryPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Security Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, VibrantPurpleBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = VibrantBannerBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(VibrantPrimaryPurple, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Security Status",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Biometric Lock Active",
                            color = VibrantBannerText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Docs are encrypted & secured",
                            color = VibrantSubText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Search by name or number...", color = VibrantSubText.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VibrantPrimaryPurple) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VibrantBodyText)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibrantPrimaryPurple,
                    unfocusedBorderColor = VibrantBorderLight,
                    focusedTextColor = VibrantBodyText,
                    unfocusedTextColor = VibrantBodyText,
                    focusedLabelColor = VibrantPrimaryPurple,
                    unfocusedLabelColor = VibrantSubText,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filtering Choice Chips
            val filters = listOf(
                "ALL" to "All",
                "PASSPORT" to "Passports",
                "DRIVING_LICENSE" to "Licenses",
                "RESIDENCE_PERMIT" to "Permits"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (filterType, filterLabel) ->
                    val isSelected = typeFilter == filterType
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) VibrantPrimaryPurple else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else VibrantBorderLight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setTypeFilter(filterType) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_chip_$filterType"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterLabel,
                            color = if (isSelected) Color.White else VibrantSecondaryClay,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Cards Scrollable area
            if (filteredDocs.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, VibrantBorderLight, RoundedCornerShape(20.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Source,
                            contentDescription = "No items",
                            tint = VibrantSubText.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO CREDENTIALS ENLISTED",
                            color = VibrantBodyText,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Register card by tapping the floating action button below.",
                            color = VibrantSubText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            isExpanded = selectedDocId == doc.id,
                            onToggle = {
                                viewModel.selectDocument(
                                    if (selectedDocId == doc.id) null else doc.id
                                )
                            },
                            onDelete = {
                                viewModel.deleteDocument(doc.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Government card deleted securely.")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Credential
        FloatingActionButton(
            onClick = { viewModel.toggleAddSheet(true) },
            containerColor = VibrantPrimaryPurple,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dashboard_fab_add"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document File")
                Spacer(modifier = Modifier.width(4.dp))
                Text("SCAN CARD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Expanded Bottom Sheet Modal for Creating/Registering Government Card
        if (isAddSheetOpen) {
            AddDocumentSheet(viewModel, snackbarHostState)
        }
    }
}

/**
 * Creation Custom Bottom Drawer Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentSheet(viewModel: WalletViewModel, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var type by remember { mutableStateOf("PASSPORT") } // PASSPORT, DRIVING_LICENSE, RESIDENCE_PERMIT
    var docNum by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("1995-12-08") }
    var issueDate by remember { mutableStateOf("2025-01-10") }
    var expiryDate by remember { mutableStateOf("2035-01-10") }
    var issuer by remember { mutableStateOf("") }
    var specs by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { viewModel.toggleAddSheet(false) },
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = VibrantBorderLight) },
        modifier = Modifier.testTag("add_doc_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ENLIST NEW CREDENTIAL",
                color = VibrantBodyText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "PASSPORT" to "Passport",
                    "DRIVING_LICENSE" to "License",
                    "RESIDENCE_PERMIT" to "Permit"
                ).forEach { (optType, optLabel) ->
                    val isSelected = type == optType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) VibrantPrimaryPurple else Color.White,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else VibrantBorderLight,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = optType
                                // Update smart defaults when switching type
                                when (optType) {
                                    "PASSPORT" -> {
                                        issuer = "UK HER MAJESTY PASSPORT OFFICE"
                                        specs = "Passport Code: GBR"
                                    }
                                    "DRIVING_LICENSE" -> {
                                        issuer = "CALIFORNIA DMV"
                                        specs = "Classes: A, B, AM"
                                    }
                                    "RESIDENCE_PERMIT" -> {
                                        issuer = "MINISTRY OF INTERIOR"
                                        specs = "Permanent Residence - Employment"
                                    }
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("type_opt_$optType"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optLabel,
                            color = if (isSelected) Color.White else VibrantSecondaryClay,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Diagnostic autofill helper buttons supporting developer testing
            Button(
                onClick = {
                    docNum = "US-${(100000..999999).random()}"
                    name = "ELIZABETH TAYLOR"
                    nationality = "UNITED STATES"
                    birthDate = "1991-08-30"
                    issueDate = "2026-03-01"
                    expiryDate = "2036-03-01"
                    if (type == "PASSPORT") {
                        issuer = "US STATE DEPT"
                        specs = "Passport Code: USA"
                    } else if (type == "DRIVING_LICENSE") {
                        issuer = "TEXAS DPS"
                        specs = "Class: BM"
                    } else {
                        issuer = "US DEPT CIVIL"
                        specs = "Permanent Visa B2"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = VibrantPrimaryPurple
                ),
                border = BorderStroke(1.dp, VibrantBorderLight),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ONE-TAP AUTOFILL TEMPLATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Input Fields
            GovTextField("FULL NAME (as displayed on ID)", name, { name = it }, "fullName")
            GovTextField("DOCUMENT INDEX NUMBER", docNum, { docNum = it }, "docNum")
            GovTextField("NATIONALITY OF BEARER", nationality, { nationality = it }, "nationality")
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    GovTextField("DATE OF ISSUE", issueDate, { issueDate = it }, "issueDate")
                }
                Box(modifier = Modifier.weight(1f)) {
                    GovTextField("DATE OF EXPIRY", expiryDate, { expiryDate = it }, "expiryDate")
                }
            }

            GovTextField("ISSUING AGENCY / AUTHORITY", issuer, { issuer = it }, "issuer")
            GovTextField("ADDITIONAL SPECIFICATIONS (Classes / Visa Type)", specs, { specs = it }, "specs")

            Spacer(modifier = Modifier.height(10.dp))

            // Submit Button
            Button(
                onClick = {
                    if (name.isBlank() || docNum.isBlank() || issuer.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill core identification details.")
                        }
                    } else {
                        viewModel.addDocument(
                            type = type,
                            docNum = docNum,
                            name = name,
                            nationality = nationality,
                            birthDate = birthDate,
                            expiryDate = expiryDate,
                            issueDate = issueDate,
                            issuer = issuer,
                            additional = specs
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Government card enlisted successfully.")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimaryPurple, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_credential_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("REGISTER CARD SECURELY", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun GovTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTagKey: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = VibrantSubText.copy(alpha = 0.8f)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VibrantPrimaryPurple,
            unfocusedBorderColor = VibrantBorderLight,
            focusedTextColor = VibrantBodyText,
            unfocusedTextColor = VibrantBodyText,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_$testTagKey"),
        singleLine = true
    )
}

/**
 * simulated scan portal for authority checkpoints
 */
@Composable
fun AuthoritiesScanView(viewModel: WalletViewModel) {
    val scanData by viewModel.scannedVerificationData.collectAsStateWithLifecycle()
    var inputPayload by remember { mutableStateOf("") }
    
    // Laser sweep animations
    val infiniteTransition = rememberInfiniteTransition(label = "laser_radar")
    val sweepFloat by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep_laser"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp)
            .testTag("authority_scanner_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "POLICE & AUTHORITY CHECKPOINT",
                    color = VibrantBodyText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Simulated encrypted government QR detector node",
                    color = VibrantSecondaryClay.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            // Radar Scanning Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.dp, VibrantBorderLight, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Diagonal crosshair markers representing active optical detection
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val edge = 32.dp.toPx()
                    val stroke = 3.dp.toPx()
                    // Top Left
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(0f, 0f), size = Size(edge, stroke))
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(0f, 0f), size = Size(stroke, edge))
                    // Top Right
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(this.size.width - edge, 0f), size = Size(edge, stroke))
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(this.size.width - stroke, 0f), size = Size(stroke, edge))
                    // Bottom Left
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(0f, this.size.height - stroke), size = Size(edge, stroke))
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(0f, this.size.height - edge), size = Size(stroke, edge))
                    // Bottom Right
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(this.size.width - edge, this.size.height - stroke), size = Size(edge, stroke))
                    drawRect(VibrantPrimaryPurple, topLeft = Offset(this.size.width - stroke, this.size.height - edge), size = Size(stroke, edge))

                    // Green pulsing sweeping radar scan line
                    val scanY = this.size.height * sweepFloat
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.8f),
                        start = Offset(0f, scanY),
                        end = Offset(this.size.width, scanY),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = VibrantPrimaryPurple.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(74.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = "READY FOR LOCAL DECRYPTION",
                        color = VibrantBodyText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Simulates instant police QR inspection",
                        color = VibrantSubText,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Scan Preset Buttons (allows direct authority check simulations!)
            Text(
                text = "SIMULATE SCAN TRIGGER:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VibrantSecondaryClay,
                letterSpacing = 0.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Preset 1
                Button(
                    onClick = {
                        viewModel.setScannedVerificationData(
                            "VERIFIED_SECURE_ID\nTYPE: PASSPORT\nNO: EP8843901\nOWNER: JOHNATHAN ROSE\nNAT: UNITED KINGDOM\nSTATUS: VALID GOVERNMENT PASS"
                        )
                    },
                    modifier = Modifier.weight(1f).height(40.dp).testTag("trigger_scan_1"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VibrantBodyText),
                    border = BorderStroke(1.dp, VibrantBorderLight),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Passport QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Preset 2
                Button(
                    onClick = {
                        viewModel.setScannedVerificationData(
                            "VERIFIED_SECURE_ID\nTYPE: DRIVING_LICENSE\nNO: DL-CAC-9122\nOWNER: MARKUS REEVES\nNAT: GERMANY\nSTATUS: VALID DRIVER B/C"
                        )
                    },
                    modifier = Modifier.weight(1f).height(40.dp).testTag("trigger_scan_2"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VibrantBodyText),
                    border = BorderStroke(1.dp, VibrantBorderLight),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("License QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Manual string entry scanning helper (User types and submits to verify)
            OutlinedTextField(
                value = inputPayload,
                onValueChange = { inputPayload = it },
                label = { Text("Or paste scanned QR text...", color = VibrantSubText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibrantPrimaryPurple,
                    unfocusedBorderColor = VibrantBorderLight,
                    focusedTextColor = VibrantBodyText,
                    unfocusedTextColor = VibrantBodyText,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("manual_payload_input"),
                trailingIcon = {
                    IconButton(onClick = {
                        if (inputPayload.isNotEmpty()) {
                            viewModel.setScannedVerificationData(inputPayload)
                            inputPayload = ""
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Scan text", tint = VibrantPrimaryPurple)
                    }
                }
            )

            // Result Alert box overlay (if QR check has completed)
            AnimatedVisibility(
                visible = scanData != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (scanData != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, VibrantPurpleBorder, RoundedCornerShape(16.dp))
                            .testTag("scan_result_card"),
                        colors = CardDefaults.cardColors(containerColor = VibrantBannerBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = VibrantPrimaryPurple, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("RFID QR VALIDATED", color = VibrantPrimaryPurple, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.setScannedVerificationData(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VibrantBodyText.copy(alpha = 0.6f))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Formatted scan report
                            Text(
                                text = scanData ?: "",
                                color = VibrantBodyText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = VibrantPrimaryPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ENCRYPTED GOVERN-KEY VALIDATED SECURELY", color = VibrantPrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Settings & Security customization Screen
 */
@Composable
fun SettingsScreen(viewModel: WalletViewModel, snackbarHostState: SnackbarHostState) {
    val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
    var rawInputPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Column {
            Text(
                text = "SECURITY CENTER",
                color = VibrantBodyText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Customize local PIN keys, encryptions, and security logs.",
                color = VibrantSecondaryClay.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Section 1: Change Pin Dashboard Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VibrantBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Password, contentDescription = null, tint = VibrantPrimaryPurple)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Change Entry Passcode PIN", color = VibrantBodyText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "The entry PIN locks access to the wallet on app launch. Modify the 4-digit code below.",
                    color = VibrantSecondaryClay,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rawInputPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) rawInputPin = it },
                        label = { Text("4-digit Numeric PIN", color = VibrantSubText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibrantPrimaryPurple,
                            unfocusedBorderColor = VibrantBorderLight,
                            focusedTextColor = VibrantBodyText,
                            unfocusedTextColor = VibrantBodyText,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_pin_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = {
                            if (rawInputPin.length != 4) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("PIN must be exactly 4 digits.")
                                }
                            } else {
                                viewModel.changePin(rawInputPin)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Success! Passcode changed to $rawInputPin")
                                }
                                rawInputPin = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimaryPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("submit_change_pin_btn")
                    ) {
                        Text("UPDATE PIN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 2: Biometric Trust Center Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VibrantBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VibrantPrimaryPurple)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Digital Trust Certificate", color = VibrantBodyText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "Encrypted biometric fingerprints and Face ID profiles are compiled locally on device. No sensitive identity strings are transmitted online to any cloud services. Government verification remains 100% serverless.",
                    color = VibrantSecondaryClay,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE ENCRYPTION:", color = VibrantSecondaryClay.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("AES-256 GCM SECURE", color = VibrantPrimaryPurple, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LOCAL STORAGE:", color = VibrantSecondaryClay.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("SQLITE PERSISTENT ROOM", color = VibrantPrimaryPurple, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Section 3: Install App on Mobile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, VibrantBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = VibrantPrimaryPurple)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Install App on Mobile", color = VibrantBodyText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "Scan this dynamic QR code with your physical smartphone or tablet's camera to run and install this Secure Digital Wallet on your mobile device instantly.",
                    color = VibrantSecondaryClay,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrCodeView(
                        payload = "https://ais-pre-ih4fqecs37eycxjovjtg7u-432268787034.europe-west2.run.app",
                        modifier = Modifier
                            .size(160.dp)
                            .border(1.dp, VibrantBorderLight, RoundedCornerShape(16.dp))
                    )

                    Text(
                        text = "Live App URL for Mobile Sharing:",
                        color = VibrantSecondaryClay.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VibrantBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, VibrantBorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "https://ais-pre-ih4fqecs37eycxjovjtg7u-432268787034.europe-west2.run.app",
                            color = VibrantPrimaryPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Live App Link", "https://ais-pre-ih4fqecs37eycxjovjtg7u-432268787034.europe-west2.run.app")
                                    clipboard.setPrimaryClip(clip)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied live app link to clipboard!")
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Could not access system clipboard.")
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = VibrantPrimaryPurple,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Safe Guidelines List
        Text(
            text = "AUTHORITY COMPLIANCE GUIDELINES:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VibrantSecondaryClay,
            letterSpacing = 1.sp
        )

        val instructions = listOf(
            "Present QR Code at standard checkpoints when asked: Local authorities check signatures using the secure decrypters.",
            "Secure biometric logins guarantee that your government credentials remain hidden when handing over unlocked phones.",
            "You can store a Custom Residence Permit, UK/EU/US Passport, and Driving License, each generating visual QR payload matrices."
        )

        instructions.forEachIndexed { idx, inst ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${idx + 1}.", color = VibrantPrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(inst, color = VibrantBodyText, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
