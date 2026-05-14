package com.mustafanazeer.baselinems.battery.vision

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.mustafanazeer.baselinems.dsp.vision.SloanScoring.ContrastLevel

object SloanChart {
    fun computeLetterLuminance(contrast: ContrastLevel): Double {
        return 1.0 - (contrast.percent / 100.0)
    }

    fun letterColor(contrast: ContrastLevel): Color {
        val l = computeLetterLuminance(contrast).toFloat()
        return Color(red = l, green = l, blue = l, alpha = 1f)
    }
}

@Composable
fun SloanLetter(letter: Char, contrast: ContrastLevel) {
    val paint = remember {
        Paint().apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }
    val fontMetrics = remember { Paint.FontMetrics() }
    val letterColor = SloanChart.letterColor(contrast)
    Canvas(
        modifier = Modifier
            .size(220.dp)
            .background(Color.White)
    ) {
        paint.color = android.graphics.Color.argb(
            (letterColor.alpha * 255).toInt(),
            (letterColor.red * 255).toInt(),
            (letterColor.green * 255).toInt(),
            (letterColor.blue * 255).toInt()
        )
        paint.textSize = size.minDimension * 0.7f
        paint.getFontMetrics(fontMetrics)
        val cx = size.width / 2f
        val cy = size.height / 2f + ((fontMetrics.descent - fontMetrics.ascent) / 2f) - fontMetrics.descent
        drawContext.canvas.nativeCanvas.drawText(letter.toString(), cx, cy, paint)
    }
}
