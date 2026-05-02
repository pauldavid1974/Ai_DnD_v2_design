package com.pauldavid74.ai_dnd.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import kotlin.random.Random

fun Modifier.parchmentBackground(
    color: Color = Color(0xFFF5E6D3),
    seed: Int = 0
): Modifier = this.drawBehind {
    val random = Random(seed)
    val path = Path()
    val width = size.width
    val height = size.height
    val step = 10f
    
    // Top Edge
    path.moveTo(0f, 0f)
    for (x in 0..(width / step).toInt()) {
        path.lineTo(x * step, random.nextFloat() * 8f)
    }
    
    // Right Edge
    for (y in 0..(height / step).toInt()) {
        path.lineTo(width - random.nextFloat() * 8f, y * step)
    }
    
    // Bottom Edge
    for (x in (width / step).toInt() downTo 0) {
        path.lineTo(x * step, height - random.nextFloat() * 8f)
    }
    
    // Left Edge
    for (y in (height / step).toInt() downTo 0) {
        path.lineTo(random.nextFloat() * 8f, y * step)
    }
    
    path.close()
    
    drawPath(
        path = path,
        color = color,
        style = Fill
    )
    
    // Subtle noise / aging
    repeat(20) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.05f),
            radius = random.nextFloat() * 50f,
            center = Offset(random.nextFloat() * width, random.nextFloat() * height)
        )
    }
}

fun Modifier.termHighlight(
    color: Color = Color(0xFFD4AF37).copy(alpha = 0.3f)
): Modifier = this.drawBehind {
    // This is a simplified version; in reality, we'd need to coordinate with TextLayoutResult
    // to find the exact coordinates of the terms. 
    // For the PRD, we'll demonstrate the concept of "bounding boxes over critical terms".
    drawRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = size,
        style = Fill
    )
}
