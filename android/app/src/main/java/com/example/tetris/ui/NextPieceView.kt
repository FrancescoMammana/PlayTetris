package com.example.tetris.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.tetris.data.Piece
import com.example.tetris.game.GameEngine

class NextPieceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var engine: GameEngine? = null

    private val bgPaint = Paint().apply { color = Color.BLACK }
    private val blockPaint = Paint().apply { style = Paint.Style.FILL }

    private val density: Float = resources.displayMetrics.density
    private val cellSize: Float = 30f * density

    private fun gridSize(): Int = 4

    fun connect(gameEngine: GameEngine) {
        engine = gameEngine
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (cellSize * gridSize()).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val gameEngine = engine ?: return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val piece: Piece = gameEngine.nextPiece ?: return

        val cols = piece.shape[0].size
        val rows = piece.shape.size

        val offsetX = ((gridSize() - cols) * cellSize) / 2f
        val offsetY = ((gridSize() - rows) * cellSize) / 2f

        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] != 0) {
                    blockPaint.color = piece.type.color
                    canvas.drawRect(
                        offsetX + x * cellSize,
                        offsetY + y * cellSize,
                        offsetX + (x + 1) * cellSize,
                        offsetY + (y + 1) * cellSize,
                        blockPaint
                    )
                }
            }
        }
    }
}