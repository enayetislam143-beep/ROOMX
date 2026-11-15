package com.enayet.roomx

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val RxBg = Color(0xFF07070C)
val RxBlue = Color(0xFF2F80FF)
val RxPurple = Color(0xFF8B5CF6)
val RxCyan = Color(0xFF22D3EE)
val RxCard = Color(0xFF16161D)
val RxMuted = Color(0xFF9AA0B4)

fun rxBrush(): Brush = Brush.horizontalGradient(listOf(RxBlue, RxPurple))

data class RxPalette(
    val dark: Boolean,
    val bg: Color,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val wave1: Color,
    val wave2: Color,
    val wave3: Color,
    val glassHi: Color,
    val glassLo: Color,
    val borderHi: Color,
    val borderLo: Color,
    val text: Color,
    val muted: Color
)

val DarkPalette = RxPalette(
    dark = true,
    bg = Color(0xFF08103A),
    bgTop = Color(0xFF08103A),
    bgMid = Color(0xFF0B1B66),
    bgBottom = Color(0xFF050A28),
    wave1 = Color(0xFF4F7BFF).copy(alpha = 0.55f),
    wave2 = Color(0xFF7C5CFF).copy(alpha = 0.40f),
    wave3 = Color(0xFF22D3EE).copy(alpha = 0.25f),
    glassHi = Color(0xFFBFD0FF).copy(alpha = 0.16f),
    glassLo = Color(0xFF8AA4FF).copy(alpha = 0.05f),
    borderHi = Color.White.copy(alpha = 0.42f),
    borderLo = Color(0xFF6F8CFF).copy(alpha = 0.22f),
    text = Color.White,
    muted = Color(0xFFA9B6DD)
)

val LightPalette = RxPalette(
    dark = false,
    bg = Color(0xFFDCEBFF),
    bgTop = Color(0xFFDCEBFF),
    bgMid = Color(0xFFA9C8FF),
    bgBottom = Color(0xFFC7DBFF),
    wave1 = Color.White.copy(alpha = 0.65f),
    wave2 = Color(0xFF5B8CFF).copy(alpha = 0.35f),
    wave3 = Color(0xFF22D3EE).copy(alpha = 0.22f),
    glassHi = Color.White.copy(alpha = 0.70f),
    glassLo = Color.White.copy(alpha = 0.32f),
    borderHi = Color.White.copy(alpha = 0.95f),
    borderLo = Color.White.copy(alpha = 0.40f),
    text = Color(0xFF0B1B4D),
    muted = Color(0xFF4A5D91)
)

val LocalRx = staticCompositionLocalOf { DarkPalette }

object ThemeStore {
    private const val FILE = "roomx_prefs"

    fun isDark(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("dark", true)

    fun setDark(ctx: Context, dark: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean("dark", dark).apply()
    }
}

@Composable
fun RxTheme(dark: Boolean, content: @Composable () -> Unit) {
    val rx = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = RxBlue,
            secondary = RxPurple,
            background = Color(0xFF08103A),
            surface = Color(0xFF111B52),
            surfaceVariant = Color(0xFF1A2766),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFA9B6DD)
        )
    } else {
        lightColorScheme(
            primary = RxBlue,
            secondary = RxPurple,
            background = Color(0xFFDCEBFF),
            surface = Color.White,
            surfaceVariant = Color(0xFFE4ECFB),
            onPrimary = Color.White,
            onBackground = Color(0xFF0B1B4D),
            onSurface = Color(0xFF0B1B4D),
            onSurfaceVariant = Color(0xFF4A5D91)
        )
    }
    CompositionLocalProvider(LocalRx provides rx) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(22.dp),
    strong: Boolean = false
): Modifier {
    val rx = LocalRx.current
    val k = if (strong) 1.3f else 1f
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    rx.glassHi.copy(alpha = minOf(1f, rx.glassHi.alpha * k)),
                    rx.glassLo
                )
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(rx.borderHi, rx.borderLo, rx.borderHi.copy(alpha = rx.borderHi.alpha * 0.5f))
            ),
            shape
        )
}

@Composable
fun Modifier.pressScale(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(120),
        label = "press"
    )
    return this
        .scale(scale)
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

private fun DrawScope.orb(color: Color, cx: Float, cy: Float, r: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy)
    )
}

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val rx = LocalRx.current
    val transition = rememberInfiniteTransition(label = "ambient")
    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waves"
    )
    Box(modifier.fillMaxSize().background(rx.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(rx.bgTop, rx.bgMid, rx.bgBottom)))
            orb(rx.wave3, w * (0.12f + 0.20f * a), h * 0.08f, w * 0.75f)

            val top = Path().apply {
                moveTo(w * 0.50f, 0f)
                cubicTo(
                    w * (0.80f + 0.06f * a), h * 0.10f,
                    w * (0.40f - 0.06f * a), h * 0.28f,
                    w * 0.52f, h * 0.46f
                )
                cubicTo(w * 0.62f, h * 0.60f, w * 0.90f, h * 0.52f, w, h * 0.60f)
                lineTo(w, 0f)
                close()
            }
            drawPath(
                top,
                Brush.linearGradient(
                    listOf(rx.wave1, Color.Transparent),
                    start = Offset(w, 0f),
                    end = Offset(w * 0.25f, h * 0.55f)
                )
            )

            val mid = Path().apply {
                moveTo(0f, h * 0.55f)
                cubicTo(
                    w * 0.28f, h * (0.46f + 0.05f * a),
                    w * 0.58f, h * (0.78f - 0.05f * a),
                    w, h * 0.66f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                mid,
                Brush.verticalGradient(
                    listOf(rx.wave2, Color.Transparent),
                    startY = h * 0.45f,
                    endY = h
                )
            )

            val low = Path().apply {
                moveTo(0f, h * 0.80f)
                cubicTo(
                    w * 0.35f, h * (0.74f + 0.04f * a),
                    w * 0.65f, h * (0.92f - 0.04f * a),
                    w, h * 0.84f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                low,
                Brush.verticalGradient(
                    listOf(rx.wave1.copy(alpha = rx.wave1.alpha * 0.6f), Color.Transparent),
                    startY = h * 0.7f,
                    endY = h
                )
            )
        }
        CompositionLocalProvider(LocalContentColor provides rx.text) {
            content()
        }
    }
}

@Composable
fun GradientBanner(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "banner")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Reverse),
        label = "shift"
    )
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(RxBlue, RxPurple, RxCyan.copy(alpha = 0.8f), RxBlue),
                    start = Offset(shift - 600f, 0f),
                    end = Offset(shift + 600f, 500f)
                )
            )
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )
        Column(Modifier.padding(24.dp)) {
            Text(title, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(subtitle, fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .pressScale(onClick)
            .glass()
            .padding(18.dp)
    ) {
        content()
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .glass(strong = strong)
            .padding(18.dp)
    ) {
        content()
    }
}

@Composable
fun Appear(
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 5 }
    ) {
        content()
    }
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val noop: () -> Unit = {}
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .pressScale(if (enabled) onClick else noop)
            .clip(shape)
            .background(rxBrush())
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )
        Text(text, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rx = LocalRx.current
    Box(
        modifier
            .pressScale(onClick)
            .glass(RoundedCornerShape(16.dp))
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, color = rx.text)
    }
}

@Composable
fun glassFieldColors(): androidx.compose.material3.TextFieldColors {
    val rx = LocalRx.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = rx.text,
        unfocusedTextColor = rx.text,
        focusedBorderColor = RxBlue,
        unfocusedBorderColor = rx.text.copy(alpha = 0.20f),
        focusedContainerColor = rx.text.copy(alpha = 0.06f),
        unfocusedContainerColor = rx.text.copy(alpha = 0.04f),
        cursorColor = RxBlue,
        focusedLabelColor = RxBlue,
        unfocusedLabelColor = rx.muted,
        focusedPlaceholderColor = rx.muted,
        unfocusedPlaceholderColor = rx.muted
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false
) {
    val rx = LocalRx.current
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(46.dp).pressScale(onClick).glass(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = rx.text)
        }
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(RxPurple)
            )
        }
    }
}

@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val rx = LocalRx.current
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animated by animateFloatAsState(
        targetValue = if (started) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(1200),
        label = "ring"
    )
    Box(modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = rx.text.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(RxPurple, RxBlue, RxCyan, RxPurple)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.width(7.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(RxBlue))
            Box(Modifier.width(7.dp).height(30.dp).clip(RoundedCornerShape(4.dp)).background(RxCyan))
            Box(Modifier.width(7.dp).height(22.dp).clip(RoundedCornerShape(4.dp)).background(RxPurple))
        }
    }
}

data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun QuickActionsRow(items: List<QuickAction>) {
    val rx = LocalRx.current
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, item ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(rx.text.copy(alpha = 0.12f))
                )
            }
            Column(
                Modifier.weight(1f).pressScale(item.onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(item.icon, contentDescription = item.label, tint = rx.text)
                Text(item.label, fontSize = 12.sp, color = rx.text, maxLines = 1)
            }
        }
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val rx = LocalRx.current
    Row(
        modifier.glass(RoundedCornerShape(24.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(RxBlue.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RxBlue)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = rx.text)
            Text(label, fontSize = 13.sp, color = rx.muted, maxLines = 1)
        }
    }
}

@Composable
fun ActivityRow(
    title: String,
    subtitle: String,
    trailing: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val rx = LocalRx.current
    Row(
        Modifier.fillMaxWidth().pressScale(onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).glass(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RxBlue)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = rx.text, maxLines = 1)
            Text(subtitle, fontSize = 13.sp, color = rx.muted, maxLines = 1)
        }
        Text(trailing, fontSize = 13.sp, color = rx.muted)
    }
}

data class GlassTab(val label: String, val icon: ImageVector)

@Composable
fun GlassBottomBar(tabs: List<GlassTab>, selected: Int, onSelect: (Int) -> Unit) {
    val rx = LocalRx.current
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(32.dp), strong = true)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selected
                val progress by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = tween(300),
                    label = "tabBg$i"
                )
                val tint by animateColorAsState(
                    targetValue = if (active) Color.White else rx.text.copy(alpha = 0.85f),
                    animationSpec = tween(300),
                    label = "tabTint$i"
                )
                Box(
                    Modifier
                        .weight(1f + 0.7f * progress)
                        .height(54.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    RxBlue.copy(alpha = progress),
                                    Color(0xFF1D4ED8).copy(alpha = progress)
                                )
                            )
                        )
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint)
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val rx = LocalRx.current
    var started by remember { mutableStateOf(false) }
    val alphaValue by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900),
        label = "splashAlpha"
    )
    val scaleValue by animateFloatAsState(
        targetValue = if (started) 1f else 0.6f,
        animationSpec = tween(900),
        label = "splashScale"
    )
    LaunchedEffect(Unit) {
        started = true
        delay(1600)
        onFinished()
    }
    AmbientBackground {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(alphaValue)
                .scale(scaleValue),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ROOMX",
                style = TextStyle(
                    brush = rxBrush(),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                "Connect. Communicate. Compete.",
                fontSize = 14.sp,
                color = rx.muted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
