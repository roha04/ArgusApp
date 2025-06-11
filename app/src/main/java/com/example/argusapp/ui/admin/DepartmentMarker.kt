package com.example.argusapp.ui.admin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class DepartmentMarker(
    mapView: MapView,
    private val context: Context
) : Marker(mapView) {

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isFakeBoldText = true
        setShadowLayer(2f, 1f, 1f, Color.WHITE)
    }

    var labelText: String? = null

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        super.draw(canvas, mapView, shadow)

        // Draw text below marker
        if (!shadow && labelText != null) {
            val point = mPositionPixels
            canvas.drawText(
                labelText!!,
                point.x.toFloat(),
                (point.y + 70).toFloat(), // Position below the marker
                textPaint
            )
        }
    }
}