package com.enayet.roomx

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val gamePresets = listOf(
    "eFootball",
    "EA SPORTS FC",
    "PUBG Mobile",
    "Free Fire",
    "Call of Duty Mobile",
    "Mobile Legends",
    "Other"
)

private val formatOptions = listOf(
    "single_elimination" to "Single Elimination",
    "double_elimination" to "Double Elimination",
    "round_robin" to "Round Robin",
    "group_knockout" to "Groups + Knockout",
    "custom" to "Custom"
)

fun formatLabel(key: String): String {
    return formatOptions.firstOrNull { it.first == key }?.second ?: key
}

fun statusLabel(status: String): String {
    return when (status) {
        "registration" -> "Registration open"
        "in_progress" -> "In progress"
        "completed" -> "Completed"
        "cancelled" -> "Cancelled"
        else -> status
    }
}

fun statusColor(status: String): Color {
    return when (status) {
        "registration" -> Color(0xFF22C55E)
        "in_progress" -> Color(0xFFF59E0B)
        "completed" -> RxBlue
        else -> Color(0xFFEF4444)
    }
}

fun startLabel(iso: String): String {
    if (iso.isBlank()) return "Not set"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso.take(19))
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date!!)
    } catch (e: Exception) {
        "Not set"
    }
}

fun localInputToIso(input: String): String? {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        parser.isLenient = false
        val date = parser.parse(input.trim())
        val out = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        out.timeZone = TimeZone.getTimeZone("UTC")
        out.format(date!!)
    } catch (e: Exception) {
        null
    }
}

private fun shareTournament(ctx: Context, name: String, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Join my ROOMX tournament \"$name\" with code $code")
    }
    try {
        ctx.startActivity(Intent.createChooser(send, "Share tournament"))
    } catch (e: Exception) {
        Toast.makeText(ctx, "No app found to share", Toast.LENGTH_SHORT).show()
    }
}

private fun roundName(round: Int, total: Int): String {
    return when (total - round) {
        0 -> "Final"
        1 -> "Semi-finals"
        2 -> "Quarter-finals"
        else -> "Round $round"
    }
}

@Composable
fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val rx = LocalRx.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val active = key == selected
            val shape = RoundedCornerShape(14.dp)
            Box(
                (if (active) Modifier.clip(shape).background(rxBrush()) else Modifier.glass(shape))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    label,
                    color = if (active) Color.White else rx.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TournamentCard(t: TournamentInfo, onClick: () -> Unit) {
    val rx = LocalRx.current
    GlowCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(rxBrush()),
                    contentAlignment = Alignment.Center
                ) {
                    RxGlyphIcon(RxGlyph.Trophy, Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        t.game + " · " + formatLabel(t.format),
                        color = rx.muted,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("${t.participants}/${t.maxParticipants}", RxPurple)
                Chip(statusLabel(t.status), statusColor(t.status))
                Chip(t.code)
            }
            if (t.myRole == "organizer") {
                Text("You organize this tournament", color = rx.muted, fontSize = 13.sp)
            } else if (t.myRole == "participant") {
                Text("You are registered", color = rx.muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TournamentsTab() {
    val rx = LocalRx.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<TournamentInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<String?>(null) }
    var showJoin by remember { mutableStateOf(false) }
    var createdInfo by remember { mutableStateOf<CreatedTournament?>(null) }

    LaunchedEffect(tab, reload) {
        loading = true
        error = null
        val scopeName = when (tab) {
            1 -> "organized"
            2 -> "registered"
            else -> "browse"
        }
        try {
            list = SupabaseApi.listTournaments(scopeName)
        } catch (e: Exception) {
            error = e.message ?: "Could not load tournaments."
        }
        loading = false
    }

    val currentId = openId
    if (currentId != null) {
        TournamentDetailScreen(
            id = currentId,
            onBack = {
                openId = null
                reload++
            }
        )
        return
    }
    if (creating) {
        CreateTournamentScreen(
            onBack = { creating = false },
            onCreated = { created ->
                creating = false
                createdInfo = created
                tab = 1
                reload++
            }
        )
        return
    }

    val q = query.trim()
    val shown = list.filter {
        q.isBlank() ||
            it.name.contains(q, ignoreCase = true) ||
            it.game.contains(q, ignoreCase = true) ||
            it.code.contains(q, ignoreCase = true)
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
                Text("🏆 Tournaments", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Host a competition or join one with a code.", color = rx.muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassButton("Join with code", Modifier.weight(1f), onClick = { showJoin = true })
            GradientButton("+  Create", Modifier.weight(1f), onClick = { creating = true })
        }
        SegmentedTabs(listOf("Browse", "Organized", "Registered"), tab) { tab = it }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search tournaments...") },
            leadingIcon = { RxGlyphIcon(RxGlyph.Search, rx.muted, iconSize = 22.dp) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (loading && list.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }
        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { reload++ }) { Text("Try again") }
        }
        if (!loading && error == null && shown.isEmpty()) {
            EmptyState(
                "No tournaments found",
                when (tab) {
                    1 -> "Tournaments you organize will appear here."
                    2 -> "Tournaments you registered for will appear here."
                    else -> "Create one, or join with a code."
                }
            )
        }
        shown.forEachIndexed { index, t ->
            Appear(delayMs = index * 80) {
                TournamentCard(t, onClick = { openId = t.id })
            }
        }
    }

    if (showJoin) {
        JoinTournamentDialog(
            onDismiss = { showJoin = false },
            onJoined = { joined ->
                showJoin = false
                reload++
                openId = joined.id
            }
        )
    }

    createdInfo?.let { c ->
        AlertDialog(
            onDismissRequest = { createdInfo = null },
            title = { Text("Tournament created") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(c.name, fontWeight = FontWeight.Bold)
                    Text("TOURNAMENT CODE", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        c.code,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RxBlue
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(c.code))
                            Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                        }) { Text("Copy Code") }
                        OutlinedButton(onClick = { shareTournament(ctx, c.name, c.code) }) {
                            Text("Share")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        createdInfo = null
                        openId = c.id
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RxBlue)
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { createdInfo = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun JoinTournamentDialog(onDismiss: () -> Unit, onJoined: (CreatedTournament) -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Join Tournament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Tournament code, e.g. TN-482761") },
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
                        error = "Enter a tournament code."
                    } else {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val joined = SupabaseApi.registerTournament(cleanCode)
                                onJoined(joined)
                            } catch (e: Exception) {
                                error = e.message ?: "Could not join the tournament."
                                loading = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RxBlue)
            ) {
                Text(if (loading) "Joining..." else "Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateTournamentScreen(onBack: () -> Unit, onCreated: (CreatedTournament) -> Unit) {
    BackHandler(onBack = onBack)
    val rx = LocalRx.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var gameChoice by remember { mutableStateOf("eFootball") }
    var customGame by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("single_elimination") }
    var teamMode by remember { mutableIntStateOf(0) }
    var maxText by remember { mutableStateOf("16") }
    var startText by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var prize by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = rx.text)
            }
            Text("Create tournament", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tournament name (3-60 characters)") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Game", fontWeight = FontWeight.SemiBold)
        ChoiceChips(gamePresets.map { it to it }, gameChoice) { gameChoice = it }
        if (gameChoice == "Other") {
            OutlinedTextField(
                value = customGame,
                onValueChange = { customGame = it },
                label = { Text("Game name") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = glassFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)
        )

        Text("Format", fontWeight = FontWeight.SemiBold)
        ChoiceChips(formatOptions, format) { format = it }
        Text(
            "Right now only Single Elimination brackets can be started. The other formats are coming soon.",
            color = rx.muted,
            fontSize = 12.sp
        )

        Text("Participation", fontWeight = FontWeight.SemiBold)
        SegmentedTabs(listOf("Individual", "Team"), teamMode) { teamMode = it }
        if (teamMode == 1) {
            Text(
                "Team management is coming soon. For now each person registers on their own.",
                color = rx.muted,
                fontSize = 12.sp
            )
        }

        OutlinedTextField(
            value = maxText,
            onValueChange = { maxText = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text("Maximum participants (2-256)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = startText,
            onValueChange = { startText = it },
            label = { Text("Start (optional): YYYY-MM-DD HH:mm") },
            placeholder = { Text("2026-12-25 18:30") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Time is in your phone's time zone.",
            color = rx.muted,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = rules,
            onValueChange = { rules = it },
            label = { Text("Rules (optional)") },
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
        )
        OutlinedTextField(
            value = prize,
            onValueChange = { prize = it },
            label = { Text("Prize details (optional)") },
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Contact information (optional)") },
            shape = RoundedCornerShape(18.dp),
            colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Public tournament", fontWeight = FontWeight.SemiBold)
                Text(
                    if (isPublic) "Anyone can find it in Browse."
                    else "Only people with the code can join.",
                    color = rx.muted,
                    fontSize = 12.sp
                )
            }
            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
        }

        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }

        GradientButton(
            text = if (loading) "Creating..." else "Create tournament",
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {
                val cleanName = name.trim()
                val game = if (gameChoice == "Other") customGame.trim() else gameChoice
                val maxP = maxText.toIntOrNull() ?: 0
                val startIso = if (startText.isBlank()) null else localInputToIso(startText)
                if (cleanName.length < 3 || cleanName.length > 60) {
                    error = "Tournament name must be 3 to 60 characters."
                } else if (game.length < 2 || game.length > 40) {
                    error = "Enter the game name (2 to 40 characters)."
                } else if (maxP < 2 || maxP > 256) {
                    error = "Maximum participants must be between 2 and 256."
                } else if (startText.isNotBlank() && startIso == null) {
                    error = "Start time format: YYYY-MM-DD HH:mm, for example 2026-12-25 18:30"
                } else {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val created = SupabaseApi.createTournament(
                                name = cleanName,
                                game = game,
                                description = description.trim(),
                                format = format,
                                participation = if (teamMode == 1) "team" else "individual",
                                maxParticipants = maxP,
                                startAtIso = startIso,
                                rules = rules.trim(),
                                prize = prize.trim(),
                                contact = contact.trim(),
                                isPublic = isPublic
                            )
                            onCreated(created)
                        } catch (e: Exception) {
                            error = e.message ?: "Could not create the tournament."
                            loading = false
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun PlayerLine(name: String, score: Int?, isWinner: Boolean) {
    val rx = LocalRx.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            color = if (isWinner) RxBlue else rx.text,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            score?.toString() ?: "-",
            color = if (isWinner) RxBlue else rx.muted,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun MatchCard(
    m: MatchInfo,
    isOrganizer: Boolean,
    running: Boolean,
    onOrganizerEnter: () -> Unit,
    onSubmit: () -> Unit,
    onConfirm: () -> Unit,
    onDispute: () -> Unit
) {
    val rx = LocalRx.current
    val me = SupabaseApi.userId ?: ""
    val bye = m.status == "bye"
    val nameA = if (m.nameA.isNotBlank()) m.nameA else if (bye && m.playerA.isBlank()) "BYE" else "TBD"
    val nameB = if (m.nameB.isNotBlank()) m.nameB else if (bye && m.playerB.isBlank()) "BYE" else "TBD"
    val isPlayer = me.isNotBlank() && (me == m.playerA || me == m.playerB)
    val iSubmitted = m.submittedBy.isNotBlank() && m.submittedBy == me

    val chipText = when {
        m.status == "ready" && m.resultStatus == "awaiting" -> "Awaiting confirmation"
        m.status == "ready" && m.resultStatus == "disputed" -> "Disputed"
        m.status == "ready" -> "Ready"
        m.status == "completed" -> "Completed"
        m.status == "bye" -> "Bye"
        else -> "Waiting"
    }
    val chipColor = when {
        m.status == "ready" && m.resultStatus == "awaiting" -> Color(0xFFF59E0B)
        m.status == "ready" && m.resultStatus == "disputed" -> Color(0xFFEF4444)
        m.status == "ready" -> RxBlue
        m.status == "completed" -> Color(0xFF22C55E)
        m.status == "bye" -> RxPurple
        else -> Color(0xFF9AA0B4)
    }

    GlassPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Chip(chipText, chipColor)
            }
            PlayerLine(nameA, m.scoreA, m.winner.isNotBlank() && m.winner == m.playerA)
            PlayerLine(nameB, m.scoreB, m.winner.isNotBlank() && m.winner == m.playerB)

            if (running && m.status == "ready") {
                if (m.resultStatus == "awaiting") {
                    Text(
                        "${m.submitterName} reported ${m.pendingA ?: 0} - ${m.pendingB ?: 0}",
                        color = rx.muted,
                        fontSize = 13.sp
                    )
                    if (isPlayer && !iSubmitted) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton("Confirm", Modifier.weight(1f), onClick = onConfirm)
                            GlassButton("Dispute", Modifier.weight(1f), onClick = onDispute)
                        }
                    } else if (isPlayer && iSubmitted) {
                        Text(
                            "Waiting for your opponent to confirm.",
                            color = rx.muted,
                            fontSize = 13.sp
                        )
                    }
                } else if (m.resultStatus == "disputed") {
                    Text(
                        "This result is disputed. The organizer will decide.",
                        color = rx.muted,
                        fontSize = 13.sp
                    )
                } else if (isPlayer) {
                    GradientButton("Submit result", Modifier.fillMaxWidth(), onClick = onSubmit)
                }
                if (isOrganizer) {
                    GlassButton(
                        "Enter result (organizer)",
                        Modifier.fillMaxWidth(),
                        onClick = onOrganizerEnter
                    )
                }
            }
        }
    }
}

@Composable
fun ResultDialog(
    match: MatchInfo,
    title: String,
    note: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    save: suspend (Int, Int) -> Unit
) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = a,
                    onValueChange = { a = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("${match.nameA} score") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = b,
                    onValueChange = { b = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("${match.nameB} score") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    note,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    val sa = a.toIntOrNull()
                    val sb = b.toIntOrNull()
                    if (sa == null || sb == null) {
                        error = "Enter both scores."
                    } else if (sa == sb) {
                        error = "A knockout match cannot end in a draw."
                    } else {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                save(sa, sb)
                                onSaved()
                            } catch (e: Exception) {
                                error = e.message ?: "Could not save the result."
                                loading = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RxBlue)
            ) {
                Text(if (loading) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
}

@Composable
fun TournamentDetailScreen(id: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val rx = LocalRx.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TournamentDetail?>(null) }
    var matches by remember { mutableStateOf<List<MatchInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var orgResultFor by remember { mutableStateOf<MatchInfo?>(null) }
    var playerResultFor by remember { mutableStateOf<MatchInfo?>(null) }
    var disputeFor by remember { mutableStateOf<MatchInfo?>(null) }

    LaunchedEffect(id, reload) {
        try {
            val d = SupabaseApi.getTournament(id)
            detail = d
            matches = if (d.status == "in_progress" || d.status == "completed") {
                SupabaseApi.getBracket(id)
            } else {
                emptyList()
            }
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Could not load the tournament."
        }
        loading = false
    }

    LaunchedEffect(detail?.status) {
        if (detail?.status == "in_progress") {
            while (true) {
                delay(10000)
                reload++
            }
        }
    }

    val act: (suspend () -> Unit) -> Unit = { block ->
        scope.launch {
            busy = true
            error = null
            try {
                block()
                reload++
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong."
                reload++
            }
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = rx.text)
            }
            Text(
                detail?.name ?: "Tournament",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }
        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }

        val d = detail
        if (d != null) {
            val isOrganizer = d.myRole == "organizer"
            val isParticipant = d.myRole == "participant"
            val full = d.participants.size >= d.maxParticipants

            GlassPanel(Modifier.fillMaxWidth(), strong = true) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip(statusLabel(d.status), statusColor(d.status))
                        Chip(d.code)
                        if (!d.isPublic) Chip("Private", RxPurple)
                    }
                    InfoRow("Game", d.game)
                    InfoRow("Format", formatLabel(d.format))
                    InfoRow("Type", if (d.participation == "team") "Team" else "Individual")
                    InfoRow("Organizer", d.organizer)
                    InfoRow("Starts", startLabel(d.startAt))
                    InfoRow("Slots", "${d.participants.size}/${d.maxParticipants}")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(d.code))
                            Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                        }) { Text("Copy code") }
                        TextButton(onClick = { shareTournament(ctx, d.name, d.code) }) {
                            Text("Share")
                        }
                    }
                }
            }

            if (d.status == "completed" && matches.isNotEmpty()) {
                val finalMatch = matches.maxByOrNull { it.round }
                val champion = if (finalMatch == null || finalMatch.winner.isBlank()) {
                    ""
                } else if (finalMatch.winner == finalMatch.playerA) {
                    finalMatch.nameA
                } else {
                    finalMatch.nameB
                }
                if (champion.isNotBlank()) {
                    GlassPanel(Modifier.fillMaxWidth(), strong = true) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🏆 Champion", fontSize = 16.sp, color = rx.muted)
                            Text(
                                champion,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RxBlue
                            )
                        }
                    }
                }
            }

            if (d.participation == "team") {
                Text(
                    "Team management is coming soon. For now each person registers on their own.",
                    color = rx.muted,
                    fontSize = 13.sp
                )
            }

            if (d.description.isNotBlank()) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(d.description, color = rx.muted)
                    }
                }
            }
            if (d.prize.isNotBlank()) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Prize", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(d.prize, color = rx.muted)
                    }
                }
            }
            if (d.rules.isNotBlank()) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Rules", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(d.rules, color = rx.muted)
                    }
                }
            }
            if (d.contact.isNotBlank()) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Contact", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(d.contact, color = rx.muted)
                    }
                }
            }

            if (matches.isNotEmpty()) {
                val rounds = matches.groupBy { it.round }.toSortedMap()
                val totalRounds = rounds.keys.maxOrNull() ?: 1
                val running = d.status == "in_progress"
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Bracket",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { reload++ }) { Text("Refresh") }
                }
                rounds.forEach { (round, list) ->
                    Text(
                        roundName(round, totalRounds),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = RxBlue
                    )
                    list.forEach { m ->
                        MatchCard(
                            m = m,
                            isOrganizer = isOrganizer,
                            running = running,
                            onOrganizerEnter = { orgResultFor = m },
                            onSubmit = { playerResultFor = m },
                            onConfirm = { act { SupabaseApi.confirmResult(m.id) } },
                            onDispute = { disputeFor = m }
                        )
                    }
                }
            }

            GlassPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Participants (${d.participants.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (d.participants.isEmpty()) {
                        Text("Nobody has registered yet.", color = rx.muted)
                    } else {
                        d.participants.forEachIndexed { index, p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}.",
                                    color = rx.muted,
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    p.username + (if (p.userId == SupabaseApi.userId) " (you)" else ""),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (d.status == "registration" && !isOrganizer && !isParticipant) {
                if (full) {
                    Text("This tournament is full.", color = rx.muted)
                } else {
                    GradientButton(
                        text = if (busy) "Please wait..." else "Register",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        onClick = { act { SupabaseApi.registerTournament(d.code) } }
                    )
                }
            }
            if (d.status == "registration" && isParticipant) {
                GlassButton(
                    text = "Withdraw from tournament",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { act { SupabaseApi.unregisterTournament(d.id) } }
                )
            }

            if (isOrganizer) {
                Text("Organizer controls", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (d.status == "registration") {
                    GradientButton(
                        text = "Start tournament",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        onClick = { confirm = "start" }
                    )
                }
                if (d.status == "in_progress") {
                    GradientButton(
                        text = "Mark as completed",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        onClick = { confirm = "complete" }
                    )
                }
                if (d.status == "registration" || d.status == "in_progress") {
                    GlassButton(
                        text = "Cancel tournament",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { confirm = "cancel" }
                    )
                }
                GlassButton(
                    text = "Delete tournament",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirm = "delete" }
                )
            }
        }
    }

    orgResultFor?.let { m ->
        ResultDialog(
            match = m,
            title = "Enter result (organizer)",
            note = "This sets the final result right away. A knockout match cannot end in a draw.",
            onDismiss = { orgResultFor = null },
            onSaved = {
                orgResultFor = null
                reload++
            },
            save = { sa, sb -> SupabaseApi.setMatchResult(m.id, sa, sb) }
        )
    }

    playerResultFor?.let { m ->
        ResultDialog(
            match = m,
            title = "Submit result",
            note = "Your opponent must confirm this result before it becomes final. A knockout match cannot end in a draw.",
            onDismiss = { playerResultFor = null },
            onSaved = {
                playerResultFor = null
                reload++
            },
            save = { sa, sb -> SupabaseApi.submitResult(m.id, sa, sb) }
        )
    }

    disputeFor?.let { m ->
        AlertDialog(
            onDismissRequest = { disputeFor = null },
            title = { Text("Dispute this result?") },
            text = { Text("The match will be held until the organizer decides the final result.") },
            confirmButton = {
                Button(onClick = {
                    disputeFor = null
                    act { SupabaseApi.disputeResult(m.id) }
                }) { Text("Dispute") }
            },
            dismissButton = {
                TextButton(onClick = { disputeFor = null }) { Text("Cancel") }
            }
        )
    }

    confirm?.let { action ->
        val title = when (action) {
            "start" -> "Start tournament?"
            "complete" -> "Mark as completed?"
            "cancel" -> "Cancel tournament?"
            else -> "Delete tournament?"
        }
        val body = when (action) {
            "start" -> "Registration will close and the bracket will be created with random seeding. You need at least 2 participants. Only Single Elimination can be started right now."
            "complete" -> "The tournament will be marked as completed."
            "cancel" -> "The tournament will be cancelled for everyone."
            else -> "This permanently deletes the tournament for everyone. This cannot be undone."
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                Button(onClick = {
                    confirm = null
                    when (action) {
                        "start" -> act { SupabaseApi.setTournamentStatus(id, "in_progress") }
                        "complete" -> act { SupabaseApi.setTournamentStatus(id, "completed") }
                        "cancel" -> act { SupabaseApi.setTournamentStatus(id, "cancelled") }
                        else -> scope.launch {
                            try {
                                SupabaseApi.deleteTournament(id)
                                onBack()
                            } catch (e: Exception) {
                                error = e.message ?: "Could not delete the tournament."
                            }
                        }
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancel") }
            }
        )
    }
}
