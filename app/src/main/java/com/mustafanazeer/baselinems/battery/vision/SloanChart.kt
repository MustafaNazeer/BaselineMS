package com.mustafanazeer.baselinems.battery.vision

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    val color = SloanChart.letterColor(contrast)
    Canvas(
        modifier = Modifier
            .size(220.dp)
            .background(Color.White)
    ) {
        drawSloanLetter(letter, color)
    }
}

private fun DrawScope.drawSloanLetter(letter: Char, color: Color) {
    val canvas = drawContext.canvas.nativeCanvas
    val paint = Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        textSize = size.minDimension * 0.7f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val fm = paint.fontMetrics
    val textHeight = fm.descent - fm.ascent
    val cx = size.width / 2f
    val cy = size.height / 2f + (textHeight / 2f) - fm.descent
    canvas.drawText(letter.toString(), cx, cy, paint)
}
