package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DirectorLoginDialog
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.IconButton
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.AuthGatewayScreen
import com.example.ui.screens.CustomerRequestScreen
import com.example.ui.screens.DriverPortalScreen
import com.example.ui.screens.GeminiAiAssistantScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MandatoryTermsGateScreen
import com.example.ui.screens.TrackingMapScreen
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.theme.YaVaTheme
import com.example.ui.theme.YaVaYellowPrimary
import com.example.ui.viewmodel.UserRole
import com.example.ui.viewmodel.YaVaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YaVaTheme {
                YaVaApp()
            }
        }
    }
}

// Navigation Tabs filtered dynamically per role
enum class ClientNavTab(val title: String, val icon: ImageVector, val testTag: String) {
    INICIO("Inicio", Icons.Default.Home, "tab_client_inicio"),
    SOLICITAR("Solicitar Envío", Icons.Default.LocalShipping, "tab_client_solicitar"),
    RASTREO("Mis Envíos & Mapa", Icons.Default.Map, "tab_client_rastreo"),
    GEMINI_IA("Gemini IA", Icons.Default.AutoAwesome, "tab_client_gemini_ia")
}

enum class DriverNavTab(val title: String, val icon: ImageVector, val testTag: String) {
    PANEL("Pedidos Disponibles", Icons.Default.DirectionsBike, "tab_driver_panel"),
    RASTREO_GPS("Navegación & Mapa", Icons.Default.Map, "tab_driver_map"),
    GEMINI_IA("Gemini Asistente", Icons.Default.AutoAwesome, "tab_driver_gemini_ia")
}

enum class AdminNavTab(val title: String, val icon: ImageVector, val testTag: String) {
    DIRECTOR("Director", Icons.Default.Shield, "tab_admin_portal"),
    ENVIOS("Crear Envío", Icons.Default.LocalShipping, "tab_admin_orders"),
    SOCIOS("Conductores", Icons.Default.DirectionsBike, "tab_admin_drivers"),
    MAPA("Mapa en Vivo", Icons.Default.Map, "tab_admin_map"),
    GEMINI_IA("ARA & Gemini", Icons.Default.AutoAwesome, "tab_admin_gemini")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YaVaApp(viewModel: YaVaViewModel = viewModel()) {
    val isTermsAccepted by viewModel.isTermsAccepted.collectAsState()
    val isUserAuthenticated by viewModel.isUserAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val authenticatedUserName by viewModel.authenticatedUserName.collectAsState()
    val authenticatedUserEmail by viewModel.authenticatedUserEmail.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var selectedClientTab by remember { mutableIntStateOf(0) }
    var selectedDriverTab by remember { mutableIntStateOf(0) }
    var selectedAdminTab by remember { mutableIntStateOf(0) }
    var showRoleSwitcherMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    // MANDATORY STEP 1: FIRST-TIME TERMS & CONDITIONS GATE
    if (!isTermsAccepted) {
        MandatoryTermsGateScreen(
            onAccept = {
                viewModel.acceptTermsAndConditions()
            }
        )
        return
    }

    // MANDATORY STEP 2: AUTHENTICATION FILTER GATE (LOGIN / REGISTER)
    if (!isUserAuthenticated) {
        AuthGatewayScreen(
            viewModel = viewModel
        )
        return
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Logo & Brand Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(YaVaYellowPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "YaVa!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(YaVaYellowPrimary)
                                )
                            }
                            Text(
                                text = when (currentRole) {
                                    UserRole.ADMIN -> "Director General"
                                    UserRole.CONDUCTOR -> "Socio Repartidor"
                                    UserRole.CLIENTE -> "Portal Cliente"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = YaVaYellowPrimary
                            )
                        }
                    }

                    // Role & Account Info in Top Bar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Surface(
                                onClick = { showRoleSwitcherMenu = true },
                                shape = RoundedCornerShape(20.dp),
                                color = when (currentRole) {
                                    UserRole.ADMIN -> YaVaYellowPrimary
                                    UserRole.CONDUCTOR -> Color(0xFF1E293B)
                                    UserRole.CLIENTE -> YaVaYellowPrimary.copy(alpha = 0.15f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    when (currentRole) {
                                        UserRole.ADMIN -> YaVaYellowPrimary
                                        UserRole.CONDUCTOR -> Color(0xFF334155)
                                        UserRole.CLIENTE -> YaVaYellowPrimary.copy(alpha = 0.4f)
                                    }
                                ),
                                modifier = Modifier.testTag("chip_role_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (currentRole) {
                                                    UserRole.ADMIN -> Color.Black
                                                    UserRole.CONDUCTOR -> Color(0xFF38BDF8)
                                                    UserRole.CLIENTE -> YaVaYellowPrimary
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (currentRole) {
                                            UserRole.ADMIN -> "Director"
                                            UserRole.CONDUCTOR -> "Repartidor"
                                            UserRole.CLIENTE -> "Cliente"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (currentRole) {
                                            UserRole.ADMIN -> Color.Black
                                            UserRole.CONDUCTOR -> Color.White
                                            UserRole.CLIENTE -> YaVaYellowPrimary
                                        }
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showRoleSwitcherMenu,
                                onDismissRequest = { showRoleSwitcherMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Director General (Admin)") },
                                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = YaVaYellowPrimary) },
                                    onClick = {
                                        viewModel.setRole(UserRole.ADMIN)
                                        showRoleSwitcherMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Vista Cliente (Remitente)") },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    onClick = {
                                        viewModel.setRole(UserRole.CLIENTE)
                                        showRoleSwitcherMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Vista Socio Repartidor") },
                                    leadingIcon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                                    onClick = {
                                        viewModel.setRole(UserRole.CONDUCTOR)
                                        showRoleSwitcherMenu = false
                                    }
                                )
                            }
                        }

                        Surface(
                            onClick = { viewModel.logoutUser() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("btn_top_bar_logout")
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Cerrar Sesión",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (currentRole) {
                        UserRole.CLIENTE -> {
                            ClientNavTab.entries.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedClientTab == index,
                                    onClick = { selectedClientTab = index },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.title, modifier = Modifier.size(20.dp)) },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selectedClientTab == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = YaVaYellowPrimary,
                                        indicatorColor = YaVaYellowPrimary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                        UserRole.CONDUCTOR -> {
                            DriverNavTab.entries.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedDriverTab == index,
                                    onClick = { selectedDriverTab = index },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.title, modifier = Modifier.size(20.dp)) },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selectedDriverTab == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = YaVaYellowPrimary,
                                        indicatorColor = YaVaYellowPrimary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                        UserRole.ADMIN -> {
                            AdminNavTab.entries.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedAdminTab == index,
                                    onClick = { selectedAdminTab = index },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.title, modifier = Modifier.size(20.dp)) },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selectedAdminTab == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = YaVaYellowPrimary,
                                        indicatorColor = YaVaYellowPrimary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val shouldShowFab = (currentRole == UserRole.CLIENTE && selectedClientTab != 1) || (currentRole == UserRole.ADMIN && selectedAdminTab != 1)
            if (shouldShowFab) {
                FloatingActionButton(
                    onClick = {
                        if (currentRole == UserRole.ADMIN) {
                            selectedAdminTab = 1
                        } else {
                            selectedClientTab = 1
                        }
                    },
                    containerColor = YaVaYellowPrimary,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.testTag("fab_quick_shipment")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Envío", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nuevo Envío", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRole) {
                UserRole.CLIENTE -> {
                    when (selectedClientTab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToRequest = { selectedClientTab = 1 },
                            onNavigateToTracking = { selectedClientTab = 2 },
                            onNavigateToDriverPortal = { viewModel.setRole(UserRole.CONDUCTOR) }
                        )
                        1 -> CustomerRequestScreen(
                            viewModel = viewModel,
                            onOrderCreatedAndTrack = { selectedClientTab = 2 }
                        )
                        2 -> TrackingMapScreen(viewModel = viewModel)
                        3 -> GeminiAiAssistantScreen(viewModel = viewModel)
                    }
                }
                UserRole.CONDUCTOR -> {
                    when (selectedDriverTab) {
                        0 -> DriverPortalScreen(viewModel = viewModel)
                        1 -> TrackingMapScreen(viewModel = viewModel)
                        2 -> GeminiAiAssistantScreen(viewModel = viewModel)
                    }
                }
                UserRole.ADMIN -> {
                    when (selectedAdminTab) {
                        0 -> AdminPortalScreen(viewModel = viewModel)
                        1 -> CustomerRequestScreen(
                            viewModel = viewModel,
                            onOrderCreatedAndTrack = { selectedAdminTab = 3 }
                        )
                        2 -> DriverPortalScreen(viewModel = viewModel)
                        3 -> TrackingMapScreen(viewModel = viewModel)
                        4 -> GeminiAiAssistantScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
