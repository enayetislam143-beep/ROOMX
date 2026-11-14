package com.enayet.roomx

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val ChatBlue = Color(0xFF2F80FF)
private val ChatPurple = Color(0xFF8B5CF6)

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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(room.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Code: ${room.code}" + (if (closed) "  •  Closed" else ""),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showMembers = true }) {
                Icon(Icons.Default.Person, contentDescription = "Members")
            }
            if (myRole == "owner") {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Room options")
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Box(
                                Modifier.widthIn(max = 280.dp)
                                    .background(
                                        if (mine) ChatBlue else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Column {
                                    if (!mine) {
                                        Text(
                                            msg.username,
                                            color = ChatPurple,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(msg.content)
                                    Text(
                                        timeLabel(msg.createdAt),
                                        fontSize = 10.sp,
                                        color = if (mine) Color(0xFFDDE8FF)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (mine || canModerate) {
                                Text(
                                    "Delete",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { deleting = msg }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 1000) input = it },
                placeholder = { Text(if (closed) "Room is closed" else "Message") },
                enabled = !closed,
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                enabled = !sending && !closed && input.isNotBlank(),
                onClick = {
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
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = ChatBlue)
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
