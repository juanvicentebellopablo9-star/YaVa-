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
import com.example.ui.screens.AdminPortalScreen
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

enum class NavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    INICIO("Inicio", Icons.Default.Home, "tab_inicio"),
    SOLICITAR("Solicitar", Icons.Default.LocalShipping, "tab_solicitar"),
    RASTREO("Mapa & Rastreo", Icons.Default.Map, "tab_rastreo"),
    CONDUCTOR("Socio Repartidor", Icons.Default.DirectionsBike, "tab_conductor"),
    ADMIN("Director", Icons.Default.Shield, "tab_admin")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YaVaApp(viewModel: YaVaViewModel = viewModel()) {
    val isTermsAccepted by viewModel.isTermsAccepted.collectAsState()
    val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDirectorLoginDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    // MANDATORY FIRST-TIME TERMS & CONDITIONS GATE
    if (!isTermsAccepted) {
        MandatoryTermsGateScreen(
            onAccept = {
                viewModel.acceptTermsAndConditions()
            }
        )
        return
    }

    // Director Auth Dialog
    if (showDirectorLoginDialog) {
        DirectorLoginDialog(
            onDismiss = { showDirectorLoginDialog = false },
            onLoginSuccess = { email, pass ->
                val ok = viewModel.authenticateAdmin(email, pass)
                if (ok) {
                    selectedTab = 4
                }
                ok
            }
        )
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
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "\"Acelerando Tus Sueños\"",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YaVaYellowPrimary
                                )
                            }
                        }

                        // Role Context Switcher in Top Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    viewModel.setRole(UserRole.CLIENTE)
                                    selectedTab = 0
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentRole == UserRole.CLIENTE) YaVaYellowPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Cliente",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentRole == UserRole.CLIENTE) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    viewModel.setRole(UserRole.CONDUCTOR)
                                    selectedTab = 3
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentRole == UserRole.CONDUCTOR) YaVaYellowPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Repartidor",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentRole == UserRole.CONDUCTOR) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    if (isAdminAuthenticated) {
                                        viewModel.setRole(UserRole.ADMIN)
                                        selectedTab = 4
                                    } else {
                                        showDirectorLoginDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentRole == UserRole.ADMIN) Color(0xFFFFD54F) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAdminAuthenticated) Icons.Default.Shield else Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = if (currentRole == UserRole.ADMIN) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Director",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (currentRole == UserRole.ADMIN) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                NavTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 4 && !isAdminAuthenticated) {
                                showDirectorLoginDialog = true
                            } else {
                                selectedTab = index
                                when (index) {
                                    0, 1, 2 -> viewModel.setRole(UserRole.CLIENTE)
                                    3 -> viewModel.setRole(UserRole.CONDUCTOR)
                                    4 -> viewModel.setRole(UserRole.ADMIN)
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
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
        },
        floatingActionButton = {
            if (selectedTab != 1 && currentRole == UserRole.CLIENTE) {
                FloatingActionButton(
                    onClick = { selectedTab = 1 },
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
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRequest = { selectedTab = 1 },
                    onNavigateToTracking = { selectedTab = 2 },
                    onNavigateToDriverPortal = { selectedTab = 3 },
                    onNavigateToAdmin = {
                        if (isAdminAuthenticated) {
                            selectedTab = 4
                        } else {
                            showDirectorLoginDialog = true
                        }
                    }
                )
                1 -> CustomerRequestScreen(
                    viewModel = viewModel,
                    onOrderCreatedAndTrack = { selectedTab = 2 }
                )
                2 -> TrackingMapScreen(viewModel = viewModel)
                3 -> DriverPortalScreen(viewModel = viewModel)
                4 -> {
                    if (isAdminAuthenticated) {
                        AdminPortalScreen(viewModel = viewModel)
                    } else {
                        // Protected Admin view
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = YaVaYellowPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Acceso Protegido de Director",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "El Panel de Administración Global está reservado exclusivamente para el Director Juan Vicente Bello Pablo (juanvicentebellopablo9@gmail.com).",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showDirectorLoginDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = YaVaYellowPrimary, contentColor = Color.Black),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("btn_protected_admin_login")
                                    ) {
                                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Iniciar Sesión de Director", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
