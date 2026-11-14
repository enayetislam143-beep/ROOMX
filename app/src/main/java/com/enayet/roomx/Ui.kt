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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    val glassHi: Color,
    val glassLo: Color,
    val borderHi: Color,
    val borderLo: Color,
    val text: Color,
    val muted: Color,
    val orbBlue: Color,
    val orbPurple: Color,
    val orbCyan: Color,
    val activeTint: Color,
    val activePill: Float
)

val DarkPalette = RxPalette(
    dark = true,
    bg = RxBg,
    glassHi = Color.White.copy(alpha = 0.10f),
    glassLo = Color.White.copy(alpha = 0.03f),
    borderHi = Color.White.copy(alpha = 0.35f),
    borderLo = Color.White.copy(alpha = 0.06f),
    text = Color.White,
    muted = RxMuted,
    orbBlue = RxBlue.copy(alpha = 0.40f),
    orbPurple = RxPurple.copy(alpha = 0.34f),
    orbCyan = RxCyan.copy(alpha = 0.18f),
    activeTint = Color.White,
    activePill = 0.45f
)

val LightPalette = RxPalette(
    dark = false,
    bg = Color(0xFFE9EEFA),
    glassHi = Color.White.copy(alpha = 0.80f),
    glassLo = Color.White.copy(alpha = 0.40f),
    borderHi = Color.White.copy(alpha = 0.95f),
    borderLo = Color(0xFF9FB0D6).copy(alpha = 0.45f),
    text = Color(0xFF0F1222),
    muted = Color(0xFF5B6478),
    orbBlue = RxBlue.copy(alpha = 0.38f),
    orbPurple = RxPurple.copy(alpha = 0.30f),
    orbCyan = RxCyan.copy(alpha = 0.28f),
    activeTint = Color(0xFF1F5FD6),
    activePill = 0.20f
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
            background = RxBg,
            surface = Color(0xFF16161D),
            surfaceVariant = Color(0xFF1E1E28),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = RxMuted
        )
    } else {
        lightColorScheme(
            primary = RxBlue,
            secondary = RxPurple,
            background = Color(0xFFE9EEFA),
            surface = Color.White,
            surfaceVariant = Color(0xFFE4E9F5),
            onPrimary = Color.White,
            onBackground = Color(0xFF0F1222),
            onSurface = Color(0xFF0F1222),
            onSurfaceVariant = Color(0xFF5B6478)
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
                listOf(rx.borderHi, rx.borderLo, RxPurple.copy(alpha = 0.35f))
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
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbs"
    )
    Box(modifier.fillMaxSize().background(rx.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            orb(rx.orbBlue, w * (0.10f + 0.35f * a), h * (0.10f + 0.06f * a), w * 0.85f)
            orb(rx.orbPurple, w * (0.95f - 0.35f * a), h * (0.45f + 0.10f * a), w * 0.80f)
            orb(rx.orbCyan, w * (0.30f + 0.30f * a), h * 0.95f, w * 0.70f)
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
        unfocusedBorderColor = rx.text.copy(alpha = 0.18f),
        focusedContainerColor = rx.text.copy(alpha = 0.06f),
        unfocusedContainerColor = rx.text.copy(alpha = 0.04f),
        cursorColor = RxBlue,
        focusedLabelColor = RxBlue,
        unfocusedLabelColor = rx.muted,
        focusedPlaceholderColor = rx.muted,
        unfocusedPlaceholderColor = rx.muted
    )
}

data class GlassTab(val label: String, val icon: ImageVector)

@Composable
fun GlassBottomBar(tabs: List<GlassTab>, selected: Int, onSelect: (Int) -> Unit) {
    val rx = LocalRx.current
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(28.dp), strong = true)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selected
                val progress by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = tween(250),
                    label = "tabBg$i"
                )
                val tint by animateColorAsState(
                    targetValue = if (active) rx.activeTint else rx.muted,
                    animationSpec = tween(250),
                    label = "tabTint$i"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    RxBlue.copy(alpha = rx.activePill * progress),
                                    RxPurple.copy(alpha = rx.activePill * progress)
                                )
                            )
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint)
                    Text(tab.label, fontSize = 10.sp, color = tint, maxLines = 1)
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
