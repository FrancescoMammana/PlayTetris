package com.example.tetris.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.tetris.game.GameEngine

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var engine: GameEngine? = null

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val gridPaint = Paint().apply {
        color = Color.argb(26, 0, 212, 255)
        strokeWidth = 1f
    }
    private val blockPaint = Paint().apply { style = Paint.Style.FILL }
    private val edgeLightPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val edgeDarkPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val density: Float = resources.displayMetrics.density
    private val cellSize: Float = 30f * density
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = 0xFF00D4FF.toInt()
    }

    fun connect(gameEngine: GameEngine) {
        engine = gameEngine
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = width * GameEngine.HEIGHT / GameEngine.WIDTH
        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val gameEngine = engine ?: return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        val cell = width.toFloat() / GameEngine.WIDTH

        // Griglia
        for (x in 1 until GameEngine.WIDTH) {
            canvas.drawLine(x * cell, 0f, x * cell, height.toFloat(), gridPaint)
        }
        for (y in 1 until GameEngine.HEIGHT) {
            canvas.drawLine(0f, y * cell, width.toFloat(), y * cell, gridPaint)
        }

        // Blocchi fissi
        for (y in 0 until GameEngine.HEIGHT) {
            for (x in 0 until GameEngine.WIDTH) {
                val color = gameEngine.board[y][x]
                if (color != 0) {
                    drawBlock(canvas, x, y, cell, color)
                }
            }
        }

        // Pezzo corrente
        val piece = gameEngine.currentPiece ?: return
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] != 0) {
                    drawBlock(canvas, piece.x + x, piece.y + y, cell, piece.type.color)
                }
            }
        }
    }

    private fun drawBlock(canvas: Canvas, boardX: Int, boardY: Int, cell: Float, color: Int) {
        val left = boardX * cell
        val top = boardY * cell
        blockPaint.color = color
        canvas.drawRect(left, top, left + cell, top + cell, blockPaint)

        edgeLightPaint.color = lighten(color, 1.3f)
        edgeDarkPaint.color = darken(color, 0.6f)

        // Luce in alto a sinistra, ombra in basso a destra (effetto 3D)
        canvas.drawLine(left, top, left + cell, top, edgeLightPaint)
        canvas.drawLine(left, top, left, top + cell, edgeLightPaint)
        canvas.drawLine(left + cell, top + cell, left, top + cell, edgeDarkPaint)
        canvas.drawLine(left + cell, top + cell, left + cell, top, edgeDarkPaint)
    }

    private fun darken(color: Int, factor: Float): Int {
        return argbScaled(color, factor)
    }

    private fun lighten(color: Int, factor: Float): Int {
        return argbScaled(color, factor)
    }

    private fun argbScaled(color: Int, factor: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF) * factor
        val g = ((color ushr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        return Color.argb(
            if (a == 0xff.toInt()) 0xff else a,
            minOf(r.toInt(), 255),
            minOf(g.toInt(), 255),
            minOf(b.toInt(), 255)
        )
    }
}