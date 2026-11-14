package com.enayet.roomx

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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

    LaunchedEffect(room.id) {
        while (true) {
            try {
                messages = SupabaseApi.getMessages(room.id)
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Could not load messages."
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
                    "Code: ${room.code}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            if (mine || room.role == "owner") {
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

        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 1000) input = it },
                placeholder = { Text("Message") },
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                enabled = !sending && input.isNotBlank(),
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
