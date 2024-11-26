package com.vhoda.luquita

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }
    private val transparentPaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onDraw(canvas: Canvas) {
        // Crear un nuevo layer para poder usar el modo CLEAR
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // Dibuja el overlay oscuro en toda la vista
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Calcula las dimensiones basadas en el scan_frame_container
        val margin = 25 * resources.displayMetrics.density
        val cornerSize = 30 * resources.displayMetrics.density

        // Calcula el alto basado en la relación de aspecto 4:3
        val availableWidth = width - (2 * margin)
        val rectHeight = (availableWidth * 3) / 4

        // Calcula la posición Y centrada
        val yOffset = (height - rectHeight) / 2

        // Define el rectángulo de escaneo
        val scanRect = RectF(
            margin,
            yOffset,
            width - margin,
            yOffset + rectHeight
        )

        // Crea el path para el área transparente
        val path = Path().apply {
            // Esquina superior izquierda
            moveTo(scanRect.left, scanRect.top + cornerSize)
            quadTo(scanRect.left, scanRect.top, scanRect.left + cornerSize, scanRect.top)

            // Esquina superior derecha
            lineTo(scanRect.right - cornerSize, scanRect.top)
            quadTo(scanRect.right, scanRect.top, scanRect.right, scanRect.top + cornerSize)

            // Esquina inferior derecha
            lineTo(scanRect.right, scanRect.bottom - cornerSize)
            quadTo(scanRect.right, scanRect.bottom, scanRect.right - cornerSize, scanRect.bottom)

            // Esquina inferior izquierda
            lineTo(scanRect.left + cornerSize, scanRect.bottom)
            quadTo(scanRect.left, scanRect.bottom, scanRect.left, scanRect.bottom - cornerSize)

            close()
        }

        // "Recorta" el área de escaneo para que sea transparente
        canvas.drawPath(path, transparentPaint)

        // Restaura el canvas
        canvas.restoreToCount(saveCount)
    }
} 