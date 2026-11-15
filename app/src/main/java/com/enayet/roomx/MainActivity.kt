package com.enayet.roomx

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this
        setContent {
            var dark by remember { mutableStateOf(ThemeStore.isDark(activity)) }
            RxTheme(dark) {
                SystemBars(dark)
                RoomXRoot(
                    dark = dark,
                    onToggleTheme = {
                        dark = !dark
                        ThemeStore.setDark(activity, dark)
                    }
                )
            }
        }
    }
}

@Composable
fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    val bg = LocalRx.current.bg
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = bg.toArgb()
        window.navigationBarColor = bg.toArgb()
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

@Composable
fun RoomXRoot(dark: Boolean, onToggleTheme: () -> Unit) {
    var splashDone by remember { mutableStateOf(false) }
    var loggedIn by remember { mutableStateOf(SupabaseApi.isLoggedIn) }
    if (!splashDone) {
        SplashScreen(onFinished = { splashDone = true })
    } else {
        AmbientBackground {
            if (loggedIn) {
                RoomXApp(
                    dark = dark,
                    onToggleTheme = onToggleTheme,
                    onLogout = {
                        SupabaseApi.signOut()
                        loggedIn = false
                    }
                )
            } else {
                AuthScreen(dark = dark, onToggleTheme = onToggleTheme, onDone = { loggedIn = true })
            }
        }
    }
}

@Composable
fun ThemeToggle(dark: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.size(40.dp).pressScale(onToggle).glass(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(if (dark) "☀️" else "🌙", fontSize = 18.sp)
    }
}

@Composable
fun TopBar(dark: Boolean, onToggle: () -> Unit) {
    val name = SupabaseApi.username?.takeIf { it.isNotBlank() } ?: "player"
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(rxBrush()),
            contentAlignment = Alignment.Center
        ) {
            Text("R", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "ROOMX",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        ThemeToggle(dark, onToggle)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(rxBrush()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AuthScreen(dark: Boolean, onToggleTheme: () -> Unit, onDone: () -> Unit) {
    val rx = LocalRx.current
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            ThemeToggle(dark, onToggleTheme)
        }
        Spacer(Modifier.height(24.dp))
        Appear(0) {
            Column {
                Text(
                    "ROOMX",
                    style = TextStyle(
                        brush = rxBrush(),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text("Connect. Communicate. Compete.", color = RxPurple)
            }
        }
        Spacer(Modifier.height(8.dp))
        Appear(120) {
            GlassPanel(Modifier.fillMaxWidth(), strong = true) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (isSignUp) "Create your account" else "Welcome back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSignUp) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username (3-20 characters)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (6+ characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    }
                    GradientButton(
                        text = if (loading) "Please wait..." else if (isSignUp) "Sign up" else "Log in",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        onClick = {
                            val cleanEmail = email.trim()
                            val cleanName = username.trim()
                            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                error = "Enter a valid email."
                            } else if (password.length < 6) {
                                error = "Password must be at least 6 characters."
                            } else if (isSignUp && (cleanName.length < 3 || cleanName.length > 20)) {
                                error = "Username must be 3 to 20 characters."
                            } else {
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        if (isSignUp) {
                                            SupabaseApi.signUp(cleanEmail, password, cleanName)
                                        } else {
                                            SupabaseApi.signIn(cleanEmail, password)
                                        }
                                        onDone()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Something went wrong."
                                    }
                                    loading = false
                                }
                            }
                        }
                    )
                }
            }
        }
        TextButton(
            onClick = {
                isSignUp = !isSignUp
                error = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isSignUp) "Already have an account? Log in" else "New here? Create an account",
                color = rx.muted
            )
        }
    }
}

fun shareRoom(ctx: Context, name: String, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Join my ROOMX room \"$name\" with code $code")
    }
    try {
        ctx.startActivity(Intent.createChooser(send, "Share room"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "No app found to share", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RoomXApp(dark: Boolean, onToggleTheme: () -> Unit, onLogout: () -> Unit) {
    val rx = LocalRx.current
    val tabs = listOf(
        GlassTab("Home", Icons.Default.Home),
        GlassTab("Rooms", Icons.Default.Email),
        GlassTab("Tournaments", Icons.Default.Star),
        GlassTab("Alerts", Icons.Default.Notifications),
        GlassTab("Profile", Icons.Default.AccountCircle)
    )
    val scope = rememberCoroutineScope()
    var selected by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var rooms by remember { mutableStateOf<List<RoomInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var createdRoom by remember { mutableStateOf<RoomInfo?>(null) }
    var openRoom by remember { mutableStateOf<RoomInfo?>(null) }
    var leaving by remember { mutableStateOf<RoomInfo?>(null) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            rooms = SupabaseApi.myRooms()
        } catch (e: Exception) {
            error = e.message ?: "Could not load rooms."
        }
        loading = false
    }

    val currentRoom = openRoom
    if (currentRoom != null) {
        ChatScreen(
            room = currentRoom,
            onBack = {
                openRoom = null
                reload++
            }
        )
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = rx.text,
            topBar = { TopBar(dark, onToggleTheme) },
            bottomBar = {
                GlassBottomBar(tabs = tabs, selected = selected, onSelect = { selected = it })
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                Crossfade(
                    targetState = selected,
                    animationSpec = tween(250),
                    label = "tabs"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(
                            rooms = rooms,
                            loading = loading,
                            onCreateRoom = { dialog = "create" },
                            onJoinRoom = { dialog = "join" },
                            onTournaments = { selected = 2 },
                            onViewAll = { selected = 1 },
                            onOpen = { openRoom = it }
                        )
                        1 -> RoomsScreen(
                            rooms = rooms,
                            loading = loading,
                            error = error,
                            onRetry = { reload++ },
                            onCreate = { dialog = "create" },
                            onJoin = { dialog = "join" },
                            onOpen = { openRoom = it },
                            onLeave = { leaving = it }
                        )
                        2 -> TournamentsScreen()
                        3 -> NotificationsScreen()
                        else -> ProfileScreen(dark, onToggleTheme, onLogout)
                    }
                }
            }
        }

        if (dialog == "create") {
            CreateRoomDialog(
                onDismiss = { dialog = null },
                onCreated = { room ->
                    dialog = null
                    createdRoom = room
                    reload++
                    selected = 1
                }
            )
        } else if (dialog == "join") {
            JoinRoomDialog(
                onDismiss = { dialog = null },
                onJoined = {
                    dialog = null
                    reload++
                    selected = 1
                }
            )
        }

        createdRoom?.let { room ->
            RoomCreatedDialog(room = room, onDone = { createdRoom = null })
        }

        leaving?.let { room ->
            AlertDialog(
                onDismissRequest = { leaving = null },
                title = { Text("Leave room?") },
                text = { Text("You will leave \"${room.name}\". You can join again with the code.") },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            try {
                                SupabaseApi.leaveRoom(room.id)
                            } catch (e: Exception) {
                                error = e.message ?: "Could not leave the room."
                            }
                            leaving = null
                            reload++
                        }
                    }) { Text("Leave") }
                },
                dismissButton = {
                    TextButton(onClick = { leaving = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun Chip(text: String, tint: Color = RxBlue) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val rx = LocalRx.current
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) rxBrush()
                        else androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (active) Color.White else rx.muted
                )
            }
        }
    }
}

@Composable
fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    val rx = LocalRx.current
    GlassPanel(modifier) {
        Column {
            Text(
                value.toString(),
                style = TextStyle(
                    brush = rxBrush(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(label, color = rx.muted, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyState(title: String, note: String) {
    val rx = LocalRx.current
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(note, color = rx.muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun RoomCard(room: RoomInfo, compact: Boolean, onOpen: () -> Unit, onLeave: () -> Unit) {
    val rx = LocalRx.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    GlowCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(rxBrush()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        room.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(room.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        "${room.members}/${room.capacity} members",
                        color = rx.muted,
                        fontSize = 13.sp
                    )
                }
                if (room.status == "closed") {
                    Chip("Closed", Color(0xFFF59E0B))
                }
            }
            if (!compact && room.description.isNotBlank()) {
                Text(room.description, color = rx.muted, fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(room.code)
                Chip(
                    when (room.role) {
                        "owner" -> "Owner"
                        "moderator" -> "Moderator"
                        else -> "Member"
                    },
                    RxPurple
                )
            }
            if (!compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(room.code))
                        Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy code") }
                    TextButton(onClick = { shareRoom(ctx, room.name, room.code) }) {
                        Text("Share")
                    }
                    if (room.role != "owner") {
                        TextButton(onClick = onLeave) { Text("Leave") }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    rooms: List<RoomInfo>,
    loading: Boolean,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onTournaments: () -> Unit,
    onViewAll: () -> Unit,
    onOpen: (RoomInfo) -> Unit
) {
    val rx = LocalRx.current
    val name = SupabaseApi.username?.takeIf { it.isNotBlank() } ?: "player"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Appear(0) {
            GlassPanel(Modifier.fillMaxWidth(), strong = true) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Welcome back,", color = rx.muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hey, ", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            name,
                            style = TextStyle(
                                brush = rxBrush(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(" 👋", fontSize = 28.sp)
                    }
                    Text(
                        "Your communities, conversations, and competitions — all in one place.",
                        color = rx.muted
                    )
                    Spacer(Modifier.height(4.dp))
                    GradientButton(
                        text = "+  Create room",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateRoom
                    )
                    GlassButton(
                        text = "Join with code",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onJoinRoom
                    )
                    GlassButton(
                        text = "Browse tournaments",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onTournaments
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                Appear(150) {
                    StatCard(rooms.size, "My rooms", Modifier.fillMaxWidth())
                }
            }
            Box(Modifier.weight(1f)) {
                Appear(230) {
                    StatCard(rooms.count { it.role == "owner" }, "Rooms you own", Modifier.fillMaxWidth())
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Your rooms",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "View all →",
                color = RxBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        if (loading && rooms.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        } else if (rooms.isEmpty()) {
            EmptyState(
                "No rooms yet",
                "Create your first room, or join a friend's room with a code."
            )
        } else {
            rooms.take(3).forEachIndexed { index, room ->
                Appear(delayMs = 300 + index * 90) {
                    RoomCard(room, compact = true, onOpen = { onOpen(room) }, onLeave = {})
                }
            }
        }
    }
}

@Composable
fun RoomsScreen(
    rooms: List<RoomInfo>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onOpen: (RoomInfo) -> Unit,
    onLeave: (RoomInfo) -> Unit
) {
    val rx = LocalRx.current
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }

    val shown = rooms.filter { room ->
        val tabOk = when (tab) {
            1 -> room.role == "owner"
            2 -> room.role != "owner"
            else -> true
        }
        val q = query.trim()
        val searchOk = q.isBlank() ||
            room.name.contains(q, ignoreCase = true) ||
            room.code.contains(q, ignoreCase = true)
        tabOk && searchOk
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Appear(0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Rooms", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Create, join, and manage your communities.", color = rx.muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassButton("Join", Modifier.weight(1f), onClick = onJoin)
            GradientButton("+  Create", Modifier.weight(1f), onClick = onCreate)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search rooms by name or code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        SegmentedTabs(listOf("All", "Owned", "Joined"), tab) { tab = it }

        if (loading && rooms.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
        if (!loading && error == null && shown.isEmpty()) {
            EmptyState(
                "No rooms found",
                if (rooms.isEmpty()) "Create a room, or join one with a code."
                else "Try a different search or tab."
            )
        }
        shown.forEachIndexed { index, room ->
            Appear(delayMs = index * 80) {
                RoomCard(room, compact = false, onOpen = { onOpen(room) }, onLeave = { onLeave(room) })
            }
        }
    }
}

@Composable
fun TournamentsScreen() {
    val rx = LocalRx.current
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val soon = { Toast.makeText(ctx, "Tournaments are coming soon", Toast.LENGTH_SHORT).show() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Appear(0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🏆 Tournaments", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Host a competition or join one with a code.", color = rx.muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassButton("Join with code", Modifier.weight(1f), onClick = soon)
            GradientButton("+  Create", Modifier.weight(1f), onClick = soon)
        }
        SegmentedTabs(listOf("Browse", "Organized", "Registered"), tab) { tab = it }
        EmptyState(
            "Coming soon",
            "Brackets, match schedules, results and history will appear here."
        )
    }
}

@Composable
fun NotificationsScreen() {
    val rx = LocalRx.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Appear(0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Notifications", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Invites, matches and announcements.", color = rx.muted)
            }
        }
        EmptyState("Nothing yet", "Notifications will appear here when they are ready.")
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    val rx = LocalRx.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = rx.muted)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = if (valueColor == Color.Unspecified) rx.text else valueColor,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun ProfileScreen(dark: Boolean, onToggleTheme: () -> Unit, onLogout: () -> Unit) {
    val rx = LocalRx.current
    val ctx = LocalContext.current
    fun open(intent: Intent) {
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "No app found to open this", Toast.LENGTH_SHORT).show()
        }
    }
    val displayName = SupabaseApi.username?.takeIf { it.isNotBlank() } ?: "player"
    val idText = (SupabaseApi.userId ?: "").let { if (it.length > 18) it.take(18) + "…" else it }
    val dial = { open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801877073438"))) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Appear(0) {
            Text("Profile", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }
        Appear(100) {
            GlassPanel(Modifier.fillMaxWidth(), strong = true) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(rxBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayName.take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(SupabaseApi.email ?: "", color = rx.muted, fontSize = 14.sp)
                    }
                }
            }
        }
        Appear(180) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column {
                    Text("Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    InfoRow("Email", SupabaseApi.email ?: "")
                    InfoRow("User ID", idText)
                    InfoRow("Theme", if (dark) "Dark" else "Light")
                }
            }
        }
        GlassButton(
            text = if (dark) "Switch to light mode" else "Switch to dark mode",
            modifier = Modifier.fillMaxWidth(),
            onClick = onToggleTheme
        )
        GlassButton(
            text = "Log out",
            modifier = Modifier.fillMaxWidth(),
            onClick = onLogout
        )

        Spacer(Modifier.height(6.dp))
        Appear(260) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column {
                    Text("Developer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    InfoRow("Application Name", "ROOMX")
                    InfoRow("Tagline", "Connect. Communicate. Compete.")
                    InfoRow("Developer Name", "Enayet Islam")
                    InfoRow("Phone", "+8801877073438", RxBlue, dial)
                    InfoRow("Instagram", "@enayet_tbf")
                }
            }
        }
        GradientButton(
            text = "Call Developer",
            modifier = Modifier.fillMaxWidth(),
            onClick = dial
        )
        GlassButton(
            text = "Instagram Profile",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/enayet_tbf/")))
            }
        )
        Appear(340) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("About", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "ROOMX",
                        style = TextStyle(
                            brush = rxBrush(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text("Connect. Communicate. Compete.", color = RxPurple)
                    Text("ROOMX is a communication and gaming tournament platform built to bring people together.")
                    Text("Create rooms, connect with friends, share conversations, and organize tournaments in one place.")
                    Text("Developed by Enayet Islam.", fontWeight = FontWeight.Bold)
                    Text("For any information or further details, please contact the developer.")
                    Text("Phone: +8801877073438")
                    Text("Instagram: @enayet_tbf")
                }
            }
        }
    }
}

@Composable
fun CreateRoomDialog(onDismiss: () -> Unit, onCreated: (RoomInfo) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Create Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room name (3-40 characters)") },
                    singleLine = true,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.length < 3 || cleanName.length > 40) {
                        error = "Room name must be 3 to 40 characters."
                    } else {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val room = SupabaseApi.createRoom(cleanName, desc.trim())
                                onCreated(room)
                            } catch (e: Exception) {
                                error = e.message ?: "Could not create the room."
                                loading = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RxBlue)
            ) {
                Text(if (loading) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}

@Composable
fun JoinRoomDialog(onDismiss: () -> Unit, onJoined: (RoomInfo) -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Join Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Room code, e.g. RX-482761") },
                    singleLine = true,
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    val cleanCode = code.trim()
                    if (cleanCode.isBlank()) {
                        error = "Enter a room code."
                    } else {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val room = SupabaseApi.joinRoom(cleanCode)
                                onJoined(room)
                            } catch (e: Exception) {
                                error = e.message ?: "Could not join the room."
                                loading = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RxBlue)
            ) {
                Text(if (loading) "Joining..." else "Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}

@Composable
fun RoomCreatedDialog(room: RoomInfo, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Room created") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(room.name, fontWeight = FontWeight.Bold)
                Text("ROOM CODE", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    room.code,
                    style = TextStyle(
                        brush = rxBrush(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(room.code))
                        Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy Code") }
                    OutlinedButton(onClick = { shareRoom(ctx, room.name, room.code) }) {
                        Text("Share")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = RxBlue)) {
                Text("Done")
            }
        }
    )
}
