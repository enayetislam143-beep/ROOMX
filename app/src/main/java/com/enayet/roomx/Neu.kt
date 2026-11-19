package com.enayet.roomx

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class NeuPalette(
    val base: Color,
    val light: Color,
    val dark: Color,
    val text: Color,
    val muted: Color,
    val blue: Color
)

@Composable
fun neuPalette(): NeuPalette {
    val rx = LocalRx.current
    return if (rx.dark) {
        NeuPalette(
            base = Color(0xFF1C2230),
            light = Color(0xFF2B3550).copy(alpha = 0.9f),
            dark = Color(0xFF0B0F18).copy(alpha = 0.95f),
            text = Color(0xFFE8ECF6),
            muted = Color(0xFF8E98B0),
            blue = Color(0xFF3B8CFF)
        )
    } else {
        NeuPalette(
            base = Color(0xFFE7EBF0),
            light = Color.White.copy(alpha = 0.95f),
            dark = Color(0xFFA3B1C6).copy(alpha = 0.65f),
            text = Color(0xFF303943),
            muted = Color(0xFF7C858E),
            blue = Color(0xFF087CFF)
        )
    }
}

object RememberStore {
    private const val FILE = "roomx_prefs"

    fun get(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("remember", true)

    fun set(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean("remember", value).apply()
    }
}

fun Modifier.neuRaised(
    corner: Dp,
    base: Color,
    lightShadow: Color,
    darkShadow: Color,
    depth: Dp = 10.dp
): Modifier = this.drawBehind {
    val c = minOf(corner.toPx(), size.minDimension / 2f)
    val path = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, c, c))
    }
    val d = depth.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.color = base
        val fp = paint.asFrameworkPaint()
        fp.setShadowLayer(d * 1.6f, d, d, darkShadow.toArgb())
        canvas.drawPath(path, paint)
        fp.setShadowLayer(d * 1.6f, -d, -d, lightShadow.toArgb())
        canvas.drawPath(path, paint)
    }
}

fun Modifier.neuInset(
    corner: Dp,
    base: Color,
    lightShadow: Color,
    darkShadow: Color
): Modifier = this.drawBehind {
    val c = minOf(corner.toPx(), size.minDimension / 2f)
    drawRoundRect(color = base, cornerRadius = CornerRadius(c, c))
    val path = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, c, c))
    }
    clipPath(path) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    darkShadow.copy(alpha = darkShadow.alpha * 0.75f),
                    Color.Transparent,
                    lightShadow.copy(alpha = lightShadow.alpha * 0.9f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
    }
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(darkShadow, lightShadow),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(c, c),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.electricRing(angle: Float, corner: Float, thickness: Float, alpha: Float) {
    val outer = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner, corner))
    }
    val innerCorner = maxOf(corner - thickness, 0f)
    val inner = Path().apply {
        addRoundRect(
            RoundRect(
                thickness,
                thickness,
                size.width - thickness,
                size.height - thickness,
                innerCorner,
                innerCorner
            )
        )
    }
    val ring = Path.combine(PathOperation.Difference, outer, inner)
    clipPath(ring) {
        rotate(angle, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    0.00f to Color.Transparent,
                    0.08f to Color.Transparent,
                    0.125f to Color(0xFF00EAFF).copy(alpha = alpha),
                    0.18f to Color(0xFF00AAFF).copy(alpha = alpha),
                    0.22f to Color(0xFF006CFF).copy(alpha = alpha),
                    0.29f to Color.Transparent,
                    1.00f to Color.Transparent,
                    center = center
                ),
                radius = maxOf(size.width, size.height),
                center = center
            )
        }
    }
}

enum class NeuGlyph { Mail, Lock, User, EyeOpen, EyeOff, Plus, ArrowRight, Check }

@Composable
fun NeuGlyphIcon(kind: NeuGlyph, tint: Color, iconSize: Dp = 22.dp) {
    Canvas(Modifier.size(iconSize)) {
        val u = this.size.minDimension / 24f
        val st = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(u, Offset.Zero) {
            when (kind) {
                NeuGlyph.Mail -> {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(3.5f, 5.5f),
                        size = Size(17f, 13f),
                        cornerRadius = CornerRadius(2.5f, 2.5f),
                        style = st
                    )
                    val flap = Path().apply {
                        moveTo(4.5f, 7.5f)
                        lineTo(12f, 13f)
                        lineTo(19.5f, 7.5f)
                    }
                    drawPath(flap, tint, style = st)
                }
                NeuGlyph.Lock -> {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(5.5f, 10.5f),
                        size = Size(13f, 9.5f),
                        cornerRadius = CornerRadius(2.5f, 2.5f),
                        style = st
                    )
                    val shackle = Path().apply {
                        moveTo(8.5f, 10.5f)
                        lineTo(8.5f, 8f)
                        cubicTo(8.5f, 4.5f, 15.5f, 4.5f, 15.5f, 8f)
                        lineTo(15.5f, 10.5f)
                    }
                    drawPath(shackle, tint, style = st)
                    drawCircle(tint, radius = 1.2f, center = Offset(12f, 15f))
                }
                NeuGlyph.User -> {
                    val shoulders = Path().apply {
                        moveTo(4.5f, 20f)
                        cubicTo(4.5f, 15.5f, 8f, 14f, 12f, 14f)
                        cubicTo(16f, 14f, 19.5f, 15.5f, 19.5f, 20f)
                    }
                    drawCircle(tint, radius = 3.6f, center = Offset(12f, 8f), style = st)
                    drawPath(shoulders, tint, style = st)
                }
                NeuGlyph.EyeOpen, NeuGlyph.EyeOff -> {
                    val eye = Path().apply {
                        moveTo(2.5f, 12f)
                        cubicTo(5f, 7.5f, 8.5f, 6f, 12f, 6f)
                        cubicTo(15.5f, 6f, 19f, 7.5f, 21.5f, 12f)
                        cubicTo(19f, 16.5f, 15.5f, 18f, 12f, 18f)
                        cubicTo(8.5f, 18f, 5f, 16.5f, 2.5f, 12f)
                        close()
                    }
                    drawPath(eye, tint, style = st)
                    drawCircle(tint, radius = 3f, center = Offset(12f, 12f), style = st)
                    if (kind == NeuGlyph.EyeOff) {
                        drawLine(
                            tint,
                            Offset(4f, 20f),
                            Offset(20f, 4f),
                            strokeWidth = 1.9f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                NeuGlyph.Plus -> {
                    drawLine(tint, Offset(12f, 5f), Offset(12f, 19f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(5f, 12f), Offset(19f, 12f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                }
                NeuGlyph.ArrowRight -> {
                    val head = Path().apply {
                        moveTo(13f, 6f)
                        lineTo(19f, 12f)
                        lineTo(13f, 18f)
                    }
                    drawLine(tint, Offset(4f, 12f), Offset(19f, 12f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawPath(head, tint, style = st)
                }
                NeuGlyph.Check -> {
                    val tick = Path().apply {
                        moveTo(5.5f, 12.5f)
                        lineTo(10f, 17f)
                        lineTo(18.5f, 7.5f)
                    }
                    drawPath(tick, tint, style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
fun NeuField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leading: NeuGlyph,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val p = neuPalette()
    var focused by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val floated = focused || value.isNotEmpty()
    val f by animateFloatAsState(
        targetValue = if (floated) 1f else 0f,
        animationSpec = tween(250),
        label = "neuLabel"
    )
    val transition = rememberInfiniteTransition(label = "electric")
    val angleState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "angle"
    )

    Box(modifier.fillMaxWidth().height(58.dp)) {
        Box(
            Modifier.matchParentSize().drawBehind {
                if (focused) {
                    val c = 19.dp.toPx()
                    electricRing(angleState.value, c, 5.dp.toPx(), 0.35f)
                    electricRing(angleState.value, c, 2.dp.toPx(), 1f)
                }
            }
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(2.dp)
                .neuInset(17.dp, p.base, p.light, p.dark)
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuGlyphIcon(leading, p.muted, 22.dp)
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = p.text, fontSize = 15.sp),
                    cursorBrush = SolidColor(p.blue),
                    visualTransformation = if (isPassword && !visible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused }
                )
                if (isPassword) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).clickable { visible = !visible },
                        contentAlignment = Alignment.Center
                    ) {
                        NeuGlyphIcon(
                            if (visible) NeuGlyph.EyeOff else NeuGlyph.EyeOpen,
                            p.muted,
                            22.dp
                        )
                    }
                }
            }
        }
        val labelX = 48f + (13f - 48f) * f
        val labelY = 19f + (-9f - 19f) * f
        val labelSize = 15f + (10f - 15f) * f
        Text(
            label,
            color = lerpColor(p.muted, p.blue, f),
            fontSize = labelSize.sp,
            maxLines = 1,
            modifier = Modifier
                .offset(x = labelX.dp, y = labelY.dp)
                .background(p.base.copy(alpha = f), RoundedCornerShape(7.dp))
                .padding(horizontal = (7f * f).dp, vertical = (1f * f).dp)
        )
    }
}

@Composable
fun NeuMainButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val p = neuPalette()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(300),
        label = "btnFill"
    )
    val shape = RoundedCornerShape(17.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(55.dp)
            .neuRaised(17.dp, p.base, p.light, p.dark, 7.dp)
            .clip(shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = fill }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF006CFF), Color(0xFF00CFFF), Color(0xFF006CFF))
                    )
                )
        )
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = lerpColor(p.blue, Color.White, fill)
        )
    }
}

@Composable
fun NeuCircleButton(glyph: NeuGlyph, onClick: () -> Unit) {
    val p = neuPalette()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(200),
        label = "circleFill"
    )
    Box(
        Modifier
            .size(49.dp)
            .neuRaised(100.dp, p.base, p.light, p.dark, 6.dp)
            .clip(CircleShape)
            .clickable(interactionSource = source, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = fill }
                .background(p.blue)
        )
        NeuGlyphIcon(glyph, lerpColor(p.blue, Color.White, fill), 22.dp)
    }
}

@Composable
fun NeuCheckBox(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = neuPalette()
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(22.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(p.blue)
                else Modifier.neuInset(6.dp, p.base, p.light, p.dark)
            )
            .clickable { onChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) NeuGlyphIcon(NeuGlyph.Check, Color.White, 16.dp)
    }
}

@Composable
fun NeuThemeToggle(dark: Boolean, onToggle: () -> Unit) {
    val p = neuPalette()
    Box(
        Modifier
            .size(48.dp)
            .neuRaised(100.dp, p.base, p.light, p.dark, 5.dp)
            .clip(CircleShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(if (dark) "☀️" else "🌙", fontSize = 18.sp)
    }
}

@Composable
private fun FloatingCircles(w: Dp, h: Dp, p: NeuPalette) {
    val t = rememberInfiniteTransition(label = "neuFloat")
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatA"
    )
    Box(
        Modifier
            .offset(x = w * 0.07f, y = h * 0.07f - 18.dp * a)
            .size(190.dp)
            .neuRaised(200.dp, p.base, p.light, p.dark, 15.dp)
    )
    Box(
        Modifier
            .offset(x = w - w * 0.08f - 130.dp, y = h - h * 0.08f - 130.dp - 18.dp * (1f - a))
            .size(130.dp)
            .neuRaised(200.dp, p.base, p.light, p.dark, 12.dp)
    )
    Box(
        Modifier
            .offset(x = w - w * 0.20f - 55.dp, y = h * 0.22f - 18.dp * a)
            .size(55.dp)
            .neuRaised(100.dp, p.base, p.light, p.dark, 7.dp)
    )
}

@Composable
fun NeuBackground(content: @Composable BoxScope.() -> Unit) {
    val p = neuPalette()
    BoxWithConstraints(Modifier.fillMaxSize().background(p.base)) {
        FloatingCircles(maxWidth, maxHeight, p)
        content()
    }
}

@Composable
private fun NeuFaceShell(p: NeuPalette, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .neuRaised(35.dp, p.base, p.light, p.dark, 16.dp)
            .padding(horizontal = 30.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun NeuLogo(glyph: NeuGlyph, p: NeuPalette) {
    Box(
        Modifier.size(86.dp).neuRaised(100.dp, p.base, p.light, p.dark, 8.dp),
        contentAlignment = Alignment.Center
    ) {
        NeuGlyphIcon(glyph, p.blue, 34.dp)
    }
}

@Composable
private fun NeuLoginFace(
    p: NeuPalette,
    email: String,
    onEmail: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    keep: Boolean,
    onKeep: (Boolean) -> Unit,
    error: String?,
    loading: Boolean,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    onSwitch: () -> Unit
) {
    NeuFaceShell(p) {
        NeuLogo(NeuGlyph.Lock, p)
        Spacer(Modifier.height(16.dp))
        Text("Welcome", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = p.text)
        Spacer(Modifier.height(4.dp))
        Text("Login to continue your journey", fontSize = 13.sp, color = p.muted)
        Spacer(Modifier.height(26.dp))
        NeuField(email, onEmail, "Email Address", NeuGlyph.Mail, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(19.dp))
        NeuField(
            password,
            onPassword,
            "Password",
            NeuGlyph.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuCheckBox(keep, onKeep)
            Spacer(Modifier.width(8.dp))
            Text("Remember me", fontSize = 13.sp, color = p.muted, modifier = Modifier.weight(1f))
            Text(
                "Forgot Password?",
                fontSize = 13.sp,
                color = p.blue,
                modifier = Modifier.clickable { onForgot() }
            )
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        NeuMainButton(if (loading) "PLEASE WAIT..." else "LOGIN", !loading, onSubmit)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", fontSize = 12.sp, color = p.muted)
            Spacer(Modifier.width(12.dp))
            NeuCircleButton(NeuGlyph.Plus, onSwitch)
        }
    }
}

@Composable
private fun NeuSignupFace(
    p: NeuPalette,
    username: String,
    onUsername: (String) -> Unit,
    email: String,
    onEmail: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    confirm: String,
    onConfirm: (String) -> Unit,
    agree: Boolean,
    onAgree: (Boolean) -> Unit,
    error: String?,
    loading: Boolean,
    onSubmit: () -> Unit,
    onTerms: () -> Unit,
    onSwitch: () -> Unit
) {
    NeuFaceShell(p) {
        NeuLogo(NeuGlyph.User, p)
        Spacer(Modifier.height(14.dp))
        Text("Create Account", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = p.text)
        Spacer(Modifier.height(4.dp))
        Text("Sign up to get started", fontSize = 13.sp, color = p.muted)
        Spacer(Modifier.height(22.dp))
        NeuField(username, onUsername, "Username", NeuGlyph.User)
        Spacer(Modifier.height(17.dp))
        NeuField(email, onEmail, "Email Address", NeuGlyph.Mail, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(17.dp))
        NeuField(
            password,
            onPassword,
            "Password",
            NeuGlyph.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )
        Spacer(Modifier.height(17.dp))
        NeuField(
            confirm,
            onConfirm,
            "Confirm Password",
            NeuGlyph.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuCheckBox(agree, onAgree)
            Spacer(Modifier.width(8.dp))
            Text("I agree to the ", fontSize = 12.sp, color = p.muted)
            Text(
                "Terms & Conditions",
                fontSize = 12.sp,
                color = p.blue,
                modifier = Modifier.clickable { onTerms() }
            )
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        NeuMainButton(if (loading) "PLEASE WAIT..." else "SIGN UP", !loading, onSubmit)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", fontSize = 12.sp, color = p.muted)
            Spacer(Modifier.width(12.dp))
            NeuCircleButton(NeuGlyph.ArrowRight, onSwitch)
        }
    }
}

@Composable
fun NeuAuthScreen(dark: Boolean, onToggleTheme: () -> Unit, onDone: () -> Unit) {
    val p = neuPalette()
    val ctx = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    var flipped by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var keepLoggedIn by remember { mutableStateOf(RememberStore.get(ctx)) }
    var agree by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(900, easing = CubicBezierEasing(0.68f, -0.55f, 0.27f, 1.55f)),
        label = "flip"
    )

    val submit: (Boolean) -> Unit = { signUp ->
        val cleanEmail = email.trim()
        val cleanName = username.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            error = "Enter a valid email."
        } else if (password.length < 6) {
            error = "Password must be at least 6 characters."
        } else if (signUp && (cleanName.length < 3 || cleanName.length > 20)) {
            error = "Username must be 3 to 20 characters."
        } else if (signUp && confirm != password) {
            error = "Passwords do not match."
        } else if (signUp && !agree) {
            error = "Please agree to the Terms & Conditions."
        } else {
            scope.launch {
                loading = true
                error = null
                try {
                    if (signUp) {
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

    NeuBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                NeuThemeToggle(dark, onToggleTheme)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .height((650f + 120f * (rot / 180f).coerceIn(0f, 1f)).dp)
                    .graphicsLayer {
                        rotationY = rot
                        cameraDistance = 12f * density
                    }
            ) {
                if (rot <= 90f) {
                    NeuLoginFace(
                        p = p,
                        email = email,
                        onEmail = { email = it },
                        password = password,
                        onPassword = { password = it },
                        keep = keepLoggedIn,
                        onKeep = {
                            keepLoggedIn = it
                            RememberStore.set(ctx, it)
                        },
                        error = error,
                        loading = loading,
                        onSubmit = { submit(false) },
                        onForgot = {
                            Toast.makeText(ctx, "Password reset is coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onSwitch = {
                            error = null
                            flipped = true
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                        NeuSignupFace(
                            p = p,
                            username = username,
                            onUsername = { username = it },
                            email = email,
                            onEmail = { email = it },
                            password = password,
                            onPassword = { password = it },
                            confirm = confirm,
                            onConfirm = { confirm = it },
                            agree = agree,
                            onAgree = { agree = it },
                            error = error,
                            loading = loading,
                            onSubmit = { submit(true) },
                            onTerms = { showTerms = true },
                            onSwitch = {
                                error = null
                                flipped = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            title = { Text("Terms & Conditions") },
            text = {
                Text(
                    "The full terms will be published before ROOMX launches. By signing up you agree to use ROOMX respectfully and to follow room and tournament rules."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) { Text("OK") }
            }
        )
    }
}
