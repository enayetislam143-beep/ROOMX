package com.enayet.roomx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val RxBlue = Color(0xFF2F80FF)
val RxPurple = Color(0xFF8B5CF6)
val RxCyan = Color(0xFF22D3EE)
val RxCard = Color(0xFF16161D)

fun rxBrush(): Brush = Brush.horizontalGradient(listOf(RxBlue, RxPurple))

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
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(RxBlue, RxPurple, RxCyan.copy(alpha = 0.8f), RxBlue),
                    start = Offset(shift - 600f, 0f),
                    end = Offset(shift + 600f, 500f)
                )
            )
            .padding(24.dp)
    ) {
        Column {
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
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .pressScale(onClick)
            .clip(shape)
            .background(RxCard)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(RxBlue.copy(alpha = 0.7f), RxPurple.copy(alpha = 0.7f))
                ),
                shape
            )
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
    val shape = RoundedCornerShape(14.dp)
    val noop: () -> Unit = {}
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .pressScale(if (enabled) onClick else noop)
            .clip(shape)
            .background(rxBrush())
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
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
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.alpha(alphaValue).scale(scaleValue),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
