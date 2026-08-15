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
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MandatoryTermsGateScreen
import com.example.ui.screens.TrackingMapScreen
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
    RASTREO("Mis Envíos & Mapa", Icons.Default.Map, "tab_client_rastreo")
}

enum class DriverNavTab(val title: String, val icon: ImageVector, val testTag: String) {
    PANEL("Pedidos Disponibles", Icons.Default.DirectionsBike, "tab_driver_panel"),
    RASTREO_GPS("Navegación & Mapa", Icons.Default.Map, "tab_driver_map")
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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(YaVaYellowPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "YaVa!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentRole == UserRole.CONDUCTOR) "Panel de Socio Repartidor" else "Portal de Cliente",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YaVaYellowPrimary
                                )
                            }
                        }

                        // Role & Account Info in Top Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentRole == UserRole.CONDUCTOR) Color(0xFF1E293B) else YaVaYellowPrimary
                            ) {
                                Text(
                                    text = if (currentRole == UserRole.CONDUCTOR) "Repartidor" else "Cliente",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentRole == UserRole.CONDUCTOR) Color.White else Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.logoutUser() },
                                modifier = Modifier.size(32.dp).testTag("btn_top_bar_logout")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Cerrar Sesión",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                if (currentRole == UserRole.CLIENTE) {
                    ClientNavTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedClientTab == index,
                            onClick = { selectedClientTab = index },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedClientTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = YaVaYellowPrimary,
                                indicatorColor = YaVaYellowPrimary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                } else {
                    DriverNavTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedDriverTab == index,
                            onClick = { selectedDriverTab = index },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedDriverTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = YaVaYellowPrimary,
                                indicatorColor = YaVaYellowPrimary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRole == UserRole.CLIENTE && selectedClientTab != 1) {
                FloatingActionButton(
                    onClick = { selectedClientTab = 1 },
                    containerColor = YaVaYellowPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("fab_quick_shipment")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Envío")
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
            if (currentRole == UserRole.CLIENTE) {
                when (selectedClientTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToRequest = { selectedClientTab = 1 },
                        onNavigateToTracking = { selectedClientTab = 2 },
                        onNavigateToDriverPortal = { }
                    )
                    1 -> CustomerRequestScreen(
                        viewModel = viewModel,
                        onOrderCreatedAndTrack = { selectedClientTab = 2 }
                    )
                    2 -> TrackingMapScreen(viewModel = viewModel)
                }
            } else {
                when (selectedDriverTab) {
                    0 -> DriverPortalScreen(viewModel = viewModel)
                    1 -> TrackingMapScreen(viewModel = viewModel)
                }
            }
        }
    }
}
