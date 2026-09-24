package com.example.tetris.game

import com.example.tetris.data.Piece
import com.example.tetris.data.TetrominoType
import kotlin.math.max

class GameEngine(
    var gameSpeed: Int = 1,
    private val pieceFactory: () -> Piece = {
        val type = TetrominoType.random()
        Piece(type, (WIDTH / 2) - (type.shape[0].size / 2))
    }
) {

    companion object {
        const val WIDTH = 10
        const val HEIGHT = 20
    }

    val board: Array<IntArray> = Array(HEIGHT) { IntArray(WIDTH) }

    var currentPiece: Piece? = null
        private set

    var nextPiece: Piece? = null
        private set

    var score: Int = 0
        private set

    var lines: Int = 0
        private set

    var level: Int = 1
        private set

    var running: Boolean = false
        private set

    var paused: Boolean = false
        private set

    var gameOver: Boolean = false
        private set

    var onGameOver: ((score: Int, lines: Int) -> Unit)? = null

    fun start() {
        initBoard()
        score = 0
        lines = 0
        level = 1
        running = true
        paused = false
        gameOver = false
        currentPiece = pieceFactory()
        nextPiece = pieceFactory()
    }

    fun initBoard() {
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                board[y][x] = 0
            }
        }
    }

    fun fallSpeedMs(): Int = max(600 - (gameSpeed - 1) * 100, 100)

    fun setSpeed(speed: Int) {
        gameSpeed = speed
    }

    fun canControlPiece(): Boolean {
        return running && !paused && !gameOver && currentPiece != null
    }

    fun isValid(piece: Piece): Boolean {
        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] != 0) {
                    val newX = piece.x + x
                    val newY = piece.y + y

                    if (newX < 0 || newX >= WIDTH || newY >= HEIGHT) {
                        return false
                    }

                    if (newY >= 0 && board[newY][newX] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun lockPiece() {
        val piece = currentPiece ?: return
        val color = piece.type.color

        for (y in piece.shape.indices) {
            for (x in piece.shape[y].indices) {
                if (piece.shape[y][x] != 0) {
                    val newX = piece.x + x
                    val newY = piece.y + y

                    if (newY >= 0 && newY < HEIGHT && newX >= 0 && newX < WIDTH) {
                        board[newY][newX] = color
                    } else if (newY < 0) {
                        endGame()
                        return
                    }
                }
            }
        }

        checkLines()

        currentPiece = nextPiece
        nextPiece = pieceFactory()

        if (!isValid(currentPiece!!)) {
            endGame()
        }
    }

    fun checkLines() {
        var completedLines = 0

        var y = HEIGHT - 1
        while (y >= 0) {
            if (board[y].all { it != 0 }) {
                for (row in y downTo 1) {
                    board[row] = board[row - 1]
                }
                board[0] = IntArray(WIDTH)
                completedLines++
            } else {
                y--
            }
        }

        if (completedLines > 0) {
            score += completedLines * 100 * gameSpeed
            lines += completedLines
            level = lines / 10 + 1
        }
    }

    fun hardDrop() {
        val piece = currentPiece ?: return
        while (piece.moveDown(::isValid)) {
            // scorre fino in fondo
        }
        lockPiece()
    }

    fun moveLeft() {
        currentPiece?.moveLeft(::isValid)
    }

    fun moveRight() {
        currentPiece?.moveRight(::isValid)
    }

    fun rotate() {
        currentPiece?.rotate(::isValid)
    }

    fun softDrop() {
        val piece = currentPiece ?: return
        if (!piece.moveDown(::isValid)) {
            lockPiece()
        }
        score += gameSpeed
    }

    fun step(): Boolean {
        if (!running || paused || gameOver) return false
        val piece = currentPiece ?: return false
        if (!piece.moveDown(::isValid)) {
            lockPiece()
        }
        return true
    }

    fun togglePause() {
        if (!running || gameOver) return
        paused = !paused
    }

    fun endGame() {
        running = false
        gameOver = true
        onGameOver?.invoke(score, lines)
    }
}