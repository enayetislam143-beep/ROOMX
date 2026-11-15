package com.enayet.roomx

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun timeLabel(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso.take(19))
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date!!)
    } catch (e: Exception) {
        ""
    }
}

private fun roleLabel(role: String): String {
    return when (role) {
        "owner" -> "Owner"
        "moderator" -> "Moderator"
        else -> "Member"
    }
}

@Composable
fun ChatScreen(room: RoomInfo, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val rx = LocalRx.current

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ChatMessage?>(null) }
    var myRole by remember { mutableStateOf(room.role) }
    var closed by remember { mutableStateOf(room.status == "closed") }
    var showMembers by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteRoom by remember { mutableStateOf(false) }
    val canModerate = myRole == "owner" || myRole == "moderator"
    val canSend = !sending && !closed && input.isNotBlank()

    LaunchedEffect(room.id) {
        while (true) {
            try {
                messages = SupabaseApi.getMessages(room.id)
                error = null
            } catch (e: Exception) {
                val m = e.message ?: ""
                if (m.contains("not a member", ignoreCase = true)) {
                    onBack()
                    return@LaunchedEffect
                }
                error = m.ifBlank { "Could not load messages." }
            }
            loading = false
            delay(3000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .glass(RoundedCornerShape(26.dp), strong = true)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = rx.text)
            }
            Column(Modifier.weight(1f)) {
                Text(room.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "Code: ${room.code}" + (if (closed) "  •  Closed" else ""),
                    fontSize = 12.sp,
                    color = rx.muted
                )
            }
            IconButton(onClick = { showMembers = true }) {
                RxGlyphIcon(RxGlyph.Person, rx.text, iconSize = 24.dp)
            }
            if (myRole == "owner") {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Room options", tint = rx.text)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (closed) "Reopen room" else "Close room") },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    try {
                                        SupabaseApi.closeRoom(room.id, !closed)
                                        closed = !closed
                                        error = null
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not change the room."
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete room") },
                            onClick = {
                                menuOpen = false
                                confirmDeleteRoom = true
                            }
                        )
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    strokeWidth = 3.dp
                )
            } else if (messages.isEmpty()) {
                Text(
                    "No messages yet. Say hello!",
                    modifier = Modifier.align(Alignment.Center),
                    color = rx.muted
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val mine = msg.senderId == SupabaseApi.userId
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
                        ) {
                            if (mine) {
                                val shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                                Box(
                                    Modifier
                                        .widthIn(max = 290.dp)
                                        .clip(shape)
                                        .background(rxBrush())
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(msg.content, color = Color.White)
                                        Text(
                                            timeLabel(msg.createdAt),
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            } else {
                                val shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                                Box(
                                    Modifier
                                        .widthIn(max = 290.dp)
                                        .glass(shape)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(
                                            msg.username,
                                            color = RxBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(msg.content, color = rx.text)
                                        Text(
                                            timeLabel(msg.createdAt),
                                            fontSize = 10.sp,
                                            color = rx.muted
                                        )
                                    }
                                }
                            }
                            if (mine || canModerate) {
                                Text(
                                    "Delete",
                                    fontSize = 11.sp,
                                    color = rx.muted,
                                    modifier = Modifier
                                        .clickable { deleting = msg }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (error != null) {
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (closed) {
            Text(
                "This room is closed. Nobody can send messages.",
                fontSize = 13.sp,
                color = rx.muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 1000) input = it },
                placeholder = { Text(if (closed) "Room is closed" else "Message") },
                enabled = !closed,
                maxLines = 4,
                shape = RoundedCornerShape(26.dp),
                colors = glassFieldColors(),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .alpha(if (canSend) 1f else 0.45f)
                    .clip(CircleShape)
                    .background(rxBrush())
                    .clickable(enabled = canSend) {
                        val text = input.trim()
                        scope.launch {
                            sending = true
                            error = null
                            try {
                                SupabaseApi.sendMessage(room.id, text)
                                input = ""
                                messages = SupabaseApi.getMessages(room.id)
                            } catch (e: Exception) {
                                error = e.message ?: "Could not send the message."
                            }
                            sending = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }

    if (showMembers) {
        MembersDialog(
            roomId = room.id,
            myRole = myRole,
            onMyRole = { myRole = it },
            onDismiss = { showMembers = false }
        )
    }

    if (confirmDeleteRoom) {
        AlertDialog(
            onDismissRequest = { confirmDeleteRoom = false },
            title = { Text("Delete room?") },
            text = { Text("\"${room.name}\" and all its messages will be deleted for everyone. This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            SupabaseApi.deleteRoom(room.id)
                            confirmDeleteRoom = false
                            onBack()
                        } catch (e: Exception) {
                            error = e.message ?: "Could not delete the room."
                            confirmDeleteRoom = false
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteRoom = false }) { Text("Cancel") }
            }
        )
    }

    deleting?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete message?") },
            text = { Text("This message will be removed for everyone in the room.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            SupabaseApi.deleteMessage(msg.id)
                            messages = SupabaseApi.getMessages(room.id)
                        } catch (e: Exception) {
                            error = e.message ?: "Could not delete the message."
                        }
                        deleting = null
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MembersDialog(
    roomId: String,
    myRole: String,
    onMyRole: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<MemberInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var removing by remember { mutableStateOf<MemberInfo?>(null) }

    LaunchedEffect(roomId, reload) {
        try {
            val list = SupabaseApi.getMembers(roomId)
            members = list
            list.firstOrNull { it.userId == SupabaseApi.userId }?.let { onMyRole(it.role) }
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Could not load members."
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Members (${members.size}/50)") },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }
                members.forEach { member ->
                    val me = member.userId == SupabaseApi.userId
                    val canRemove = !me && (
                        (myRole == "owner" && member.role != "owner") ||
                            (myRole == "moderator" && member.role == "member")
                        )
                    val canPromote = myRole == "owner" && !me && member.role != "owner"

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            member.username + (if (me) " (you)" else ""),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            roleLabel(member.role),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (canRemove || canPromote) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (canPromote) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            try {
                                                SupabaseApi.setModerator(
                                                    roomId,
                                                    member.userId,
                                                    member.role != "moderator"
                                                )
                                                reload++
                                            } catch (e: Exception) {
                                                error = e.message ?: "Could not change the role."
                                            }
                                        }
                                    }) {
                                        Text(
                                            if (member.role == "moderator") "Remove mod"
                                            else "Make mod"
                                        )
                                    }
                                }
                                if (canRemove) {
                                    TextButton(onClick = { removing = member }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    removing?.let { target ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove member?") },
            text = { Text("${target.username} will be removed from this room.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            SupabaseApi.removeMember(roomId, target.userId)
                            reload++
                        } catch (e: Exception) {
                            error = e.message ?: "Could not remove the member."
                        }
                        removing = null
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancel") }
            }
        )
    }
}
