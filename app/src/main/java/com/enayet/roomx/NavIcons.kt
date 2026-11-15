package com.enayet.roomx

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RxGlyph { Home, Grid, Bell, Person, Search, Bars, Trophy, Plus, Join, Chat }

@Composable
fun RxGlyphIcon(
    kind: RxGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp
) {
    Canvas(modifier.size(iconSize)) {
        val u = this.size.minDimension / 24f
        val st = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(u, Offset.Zero) {
            when (kind) {
                RxGlyph.Home -> {
                    val roof = Path().apply {
                        moveTo(3.5f, 11f)
                        lineTo(12f, 4f)
                        lineTo(20.5f, 11f)
                    }
                    val body = Path().apply {
                        moveTo(5.5f, 9.5f)
                        lineTo(5.5f, 20f)
                        lineTo(18.5f, 20f)
                        lineTo(18.5f, 9.5f)
                    }
                    val door = Path().apply {
                        moveTo(10f, 20f)
                        lineTo(10f, 14.5f)
                        lineTo(14f, 14.5f)
                        lineTo(14f, 20f)
                    }
                    drawPath(roof, tint, style = st)
                    drawPath(body, tint, style = st)
                    drawPath(door, tint, style = st)
                }
                RxGlyph.Grid -> {
                    val corner = CornerRadius(2f, 2f)
                    val box = Size(7f, 7f)
                    drawRoundRect(color = tint, topLeft = Offset(4f, 4f), size = box, cornerRadius = corner, style = st)
                    drawRoundRect(color = tint, topLeft = Offset(13f, 4f), size = box, cornerRadius = corner, style = st)
                    drawRoundRect(color = tint, topLeft = Offset(4f, 13f), size = box, cornerRadius = corner, style = st)
                    drawRoundRect(color = tint, topLeft = Offset(13f, 13f), size = box, cornerRadius = corner, style = st)
                }
                RxGlyph.Bell -> {
                    val body = Path().apply {
                        moveTo(6f, 16.5f)
                        lineTo(6f, 11f)
                        cubicTo(6f, 7.5f, 8.5f, 5f, 12f, 5f)
                        cubicTo(15.5f, 5f, 18f, 7.5f, 18f, 11f)
                        lineTo(18f, 16.5f)
                        lineTo(19.5f, 18f)
                        lineTo(4.5f, 18f)
                        close()
                    }
                    val clapper = Path().apply {
                        moveTo(10f, 20.5f)
                        cubicTo(10.6f, 21.8f, 13.4f, 21.8f, 14f, 20.5f)
                    }
                    drawPath(body, tint, style = st)
                    drawLine(tint, Offset(12f, 3f), Offset(12f, 5f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawPath(clapper, tint, style = st)
                }
                RxGlyph.Person -> {
                    val shoulders = Path().apply {
                        moveTo(4.5f, 20f)
                        cubicTo(4.5f, 15.5f, 8f, 14f, 12f, 14f)
                        cubicTo(16f, 14f, 19.5f, 15.5f, 19.5f, 20f)
                    }
                    drawCircle(tint, radius = 3.7f, center = Offset(12f, 8f), style = st)
                    drawPath(shoulders, tint, style = st)
                }
                RxGlyph.Search -> {
                    drawCircle(tint, radius = 6f, center = Offset(10.5f, 10.5f), style = st)
                    drawLine(tint, Offset(15f, 15f), Offset(20f, 20f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                }
                RxGlyph.Bars -> {
                    drawLine(tint, Offset(7f, 20f), Offset(7f, 13f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(12f, 20f), Offset(12f, 5f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(17f, 20f), Offset(17f, 10f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                }
                RxGlyph.Trophy -> {
                    val cup = Path().apply {
                        moveTo(7f, 4f)
                        lineTo(17f, 4f)
                        lineTo(17f, 9f)
                        cubicTo(17f, 12f, 14.8f, 14f, 12f, 14f)
                        cubicTo(9.2f, 14f, 7f, 12f, 7f, 9f)
                        close()
                    }
                    val left = Path().apply {
                        moveTo(7f, 6f)
                        lineTo(4f, 6f)
                        cubicTo(4f, 9f, 5.5f, 10.5f, 7.4f, 10.8f)
                    }
                    val right = Path().apply {
                        moveTo(17f, 6f)
                        lineTo(20f, 6f)
                        cubicTo(20f, 9f, 18.5f, 10.5f, 16.6f, 10.8f)
                    }
                    drawPath(cup, tint, style = st)
                    drawPath(left, tint, style = st)
                    drawPath(right, tint, style = st)
                    drawLine(tint, Offset(12f, 14f), Offset(12f, 19f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(8f, 20f), Offset(16f, 20f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                }
                RxGlyph.Plus -> {
                    drawLine(tint, Offset(12f, 5f), Offset(12f, 19f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(5f, 12f), Offset(19f, 12f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                }
                RxGlyph.Join -> {
                    val head = Path().apply {
                        moveTo(10f, 8.5f)
                        lineTo(13.5f, 12f)
                        lineTo(10f, 15.5f)
                    }
                    val frame = Path().apply {
                        moveTo(14f, 4.5f)
                        lineTo(17.5f, 4.5f)
                        cubicTo(19f, 4.5f, 20f, 5.5f, 20f, 7f)
                        lineTo(20f, 17f)
                        cubicTo(20f, 18.5f, 19f, 19.5f, 17.5f, 19.5f)
                        lineTo(14f, 19.5f)
                    }
                    drawLine(tint, Offset(4f, 12f), Offset(13.5f, 12f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawPath(head, tint, style = st)
                    drawPath(frame, tint, style = st)
                }
                RxGlyph.Chat -> {
                    val tail = Path().apply {
                        moveTo(8.5f, 16f)
                        lineTo(8.5f, 20f)
                        lineTo(12.5f, 16f)
                    }
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(4f, 4f),
                        size = Size(16f, 12f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = st
                    )
                    drawPath(tail, tint, style = st)
                }
            }
        }
    }
}

data class RxTab(val label: String, val glyph: RxGlyph)

@Composable
fun RxBottomBar(tabs: List<RxTab>, selected: Int, onSelect: (Int) -> Unit) {
    val rx = LocalRx.current
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(34.dp), strong = true)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selected
                val progress by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = tween(300),
                    label = "rxTab$i"
                )
                val tint by animateColorAsState(
                    targetValue = if (active) Color.White else rx.text.copy(alpha = 0.85f),
                    animationSpec = tween(300),
                    label = "rxTint$i"
                )
                Box(
                    Modifier
                        .weight(1f + 0.5f * progress)
                        .height(56.dp)
                        .clip(RoundedCornerShape(30.dp))
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
                    RxGlyphIcon(tab.glyph, tint, iconSize = 27.dp)
                }
            }
        }
    }
}

data class RxQuickAction(val label: String, val glyph: RxGlyph, val onClick: () -> Unit)

@Composable
fun RxQuickActionsRow(items: List<RxQuickAction>) {
    val rx = LocalRx.current
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp)).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, item ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(rx.text.copy(alpha = 0.12f))
                )
            }
            Column(
                Modifier.weight(1f).pressScale(item.onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RxGlyphIcon(item.glyph, rx.text)
                Text(item.label, fontSize = 12.sp, color = rx.text, maxLines = 1)
            }
        }
    }
}

@Composable
fun RxStatTile(
    value: String,
    label: String,
    glyph: RxGlyph,
    modifier: Modifier = Modifier
) {
    val rx = LocalRx.current
    Row(
        modifier.glass(RoundedCornerShape(26.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(RxBlue.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            RxGlyphIcon(glyph, RxBlue)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = rx.text)
            Text(label, fontSize = 13.sp, color = rx.muted, maxLines = 1)
        }
    }
}

@Composable
fun RxActivityRow(
    title: String,
    subtitle: String,
    trailing: String,
    glyph: RxGlyph,
    onClick: () -> Unit
) {
    val rx = LocalRx.current
    Row(
        Modifier.fillMaxWidth().pressScale(onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).glass(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            RxGlyphIcon(glyph, RxBlue, iconSize = 24.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = rx.text, maxLines = 1)
            Text(subtitle, fontSize = 13.sp, color = rx.muted, maxLines = 1)
        }
        Text(trailing, fontSize = 13.sp, color = rx.muted)
    }
}

@Composable
fun RxIconButton(
    glyph: RxGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false
) {
    val rx = LocalRx.current
    Box(modifier.size(50.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(46.dp).pressScale(onClick).glass(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            RxGlyphIcon(glyph, rx.text, iconSize = 24.dp)
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
