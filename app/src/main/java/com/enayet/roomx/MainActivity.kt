package com.enayet.roomx

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Blue = Color(0xFF2F80FF)
private val Purple = Color(0xFF8B5CF6)

private val RoomXColors = darkColorScheme(
    primary = Blue,
    secondary = Purple,
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF16161D),
    surfaceVariant = Color(0xFF1E1E28),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B3C0)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = RoomXColors) { RoomXRoot() }
        }
    }
}

@Composable
fun RoomXRoot() {
    var loggedIn by remember { mutableStateOf(SupabaseApi.isLoggedIn) }
    if (loggedIn) {
        RoomXApp(onLogout = {
            SupabaseApi.signOut()
            loggedIn = false
        })
    } else {
        AuthScreen(onDone = { loggedIn = true })
    }
}

@Composable
fun AuthScreen(onDone: () -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Text("ROOMX", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Blue)
            Text("Connect. Communicate. Compete.", color = Purple)
            Spacer(Modifier.height(16.dp))
            Text(
                if (isSignUp) "Create your account" else "Log in",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            if (isSignUp) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (3-20 characters)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (6+ characters)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }

            Button(
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
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(if (isSignUp) "Sign up" else "Log in")
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
                    if (isSignUp) "Already have an account? Log in"
                    else "New here? Create an account"
                )
            }
        }
    }
}

data class NavTab(val label: String, val icon: ImageVector)

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
fun RoomXApp(onLogout: () -> Unit) {
    val tabs = listOf(
        NavTab("Home", Icons.Default.Home),
        NavTab("Rooms", Icons.Default.Email),
        NavTab("Tournaments", Icons.Default.Star),
        NavTab("Alerts", Icons.Default.Notifications),
        NavTab("Profile", Icons.Default.AccountCircle)
    )
    var selected by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var createdRoom by remember { mutableStateOf<RoomInfo?>(null) }
    var openRoom by remember { mutableStateOf<RoomInfo?>(null) }

    val currentRoom = openRoom
    if (currentRoom != null) {
        ChatScreen(
            room = currentRoom,
            onBack = {
                openRoom = null
                refreshKey++
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { i, tab ->
                        NavigationBarItem(
                            selected = selected == i,
                            onClick = { selected = i },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Blue,
                                indicatorColor = Blue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (selected) {
                    0 -> HomeScreen(
                        onCreateRoom = { dialog = "create" },
                        onJoinRoom = { dialog = "join" }
                    )
                    1 -> RoomsScreen(
                        refreshKey = refreshKey,
                        onCreate = { dialog = "create" },
                        onJoin = { dialog = "join" },
                        onOpen = { room -> openRoom = room }
                    )
                    2 -> ComingSoon("Tournaments", "Brackets, schedules and results. Coming in Stage 6.")
                    3 -> ComingSoon("Notifications", "Invites, matches and announcements. Coming in Stage 8.")
                    else -> ProfileScreen(onLogout)
                }
            }
        }

        if (dialog == "create") {
            CreateRoomDialog(
                onDismiss = { dialog = null },
                onCreated = { room ->
                    dialog = null
                    createdRoom = room
                    refreshKey++
                    selected = 1
                }
            )
        } else if (dialog == "join") {
            JoinRoomDialog(
                onDismiss = { dialog = null },
                onJoined = {
                    dialog = null
                    refreshKey++
                    selected = 1
                }
            )
        }

        createdRoom?.let { room ->
            RoomCreatedDialog(room = room, onDone = { createdRoom = null })
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
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
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
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
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
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
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
                Text(room.code, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Blue)
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
            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                Text("Done")
            }
        }
    )
}

@Composable
fun RoomsScreen(
    refreshKey: Int,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onOpen: (RoomInfo) -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var rooms by remember { mutableStateOf<List<RoomInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var leaving by remember { mutableStateOf<RoomInfo?>(null) }

    LaunchedEffect(refreshKey, reload) {
        loading = true
        error = null
        try {
            rooms = SupabaseApi.myRooms()
        } catch (e: Exception) {
            error = e.message ?: "Could not load rooms."
        }
        loading = false
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("My Rooms", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCreate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Blue)
            ) { Text("Create Room") }
            OutlinedButton(onClick = onJoin, modifier = Modifier.weight(1f)) {
                Text("Join Room")
            }
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }
        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { reload++ }) { Text("Try again") }
        }
        if (!loading && error == null && rooms.isEmpty()) {
            Text(
                "You are not in any room yet. Create one, or join a friend's room with a code.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        rooms.forEach { room ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(room.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (room.description.isNotBlank()) {
                        Text(room.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Code: ${room.code}", fontWeight = FontWeight.SemiBold, color = Blue)
                    Text(
                        "${room.members}/${room.capacity} members  •  " +
                            (if (room.role == "owner") "Owner" else "Member"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onOpen(room) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) { Text("Open chat") }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(room.code))
                            Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                        }) { Text("Copy code") }
                        TextButton(onClick = { shareRoom(ctx, room.name, room.code) }) {
                            Text("Share")
                        }
                        if (room.role != "owner") {
                            TextButton(onClick = { leaving = room }) { Text("Leave") }
                        }
                    }
                }
            }
        }
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
                            leaving = null
                            reload++
                        } catch (e: Exception) {
                            error = e.message ?: "Could not leave the room."
                            leaving = null
                        }
                    }
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { leaving = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HomeScreen(onCreateRoom: () -> Unit, onJoinRoom: () -> Unit) {
    val ctx = LocalContext.current
    val soon = { Toast.makeText(ctx, "Coming in an upcoming stage", Toast.LENGTH_SHORT).show() }
    val name = SupabaseApi.username?.takeIf { it.isNotBlank() } ?: "player"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Blue, Purple)), RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Column {
                Text("ROOMX", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("Connect. Communicate. Compete.", fontSize = 15.sp)
            }
        }
        Text("Welcome, $name", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Create Room", Icons.Default.Add, Modifier.weight(1f), onCreateRoom)
            ActionCard("Join Room", Icons.Default.Search, Modifier.weight(1f), onJoinRoom)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Create Tournament", Icons.Default.Star, Modifier.weight(1f), soon)
            ActionCard("Join Tournament", Icons.Default.Check, Modifier.weight(1f), soon)
        }
    }
}

@Composable
fun ActionCard(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp).fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = Blue)
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ComingSoon(title: String, note: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    fun open(intent: Intent) {
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "No app found to open this", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    SupabaseApi.username?.takeIf { it.isNotBlank() } ?: "player",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(SupabaseApi.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }

        Spacer(Modifier.height(8.dp))
        Text("About ROOMX", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ROOMX", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Blue)
                Text("Connect. Communicate. Compete.", color = Purple)
                Text("ROOMX is a communication and gaming tournament platform built to bring people together.")
                Text("Create rooms, connect with friends, share conversations, and organize tournaments in one place.")
                Text("Developed by Enayet Islam.", fontWeight = FontWeight.Bold)
                Text("For any information or further details, please contact the developer.")
                Text(
                    "Phone: +8801877073438",
                    color = Blue,
                    modifier = Modifier.clickable {
                        open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801877073438")))
                    }
                )
                Text("Instagram: @enayet_tbf")
            }
        }
        Button(
            onClick = { open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801877073438"))) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            Icon(Icons.Default.Call, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Call Developer")
        }
        Button(
            onClick = {
                open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/enayet_tbf/")))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("Instagram Profile")
        }
    }
}
