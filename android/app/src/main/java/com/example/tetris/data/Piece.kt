package com.example.tetris.data

class Piece(
    val type: TetrominoType,
    var x: Int,
    var y: Int = 0
) {

    var shape: Array<IntArray> = Array(type.shape.size) { r ->
        IntArray(type.shape[r].size) { c -> type.shape[r][c] }
    }
        private set

    fun rotate(isValid: (Piece) -> Boolean) {
        val rows = shape.size
        val cols = shape[0].size
        val rotated = Array(cols) { i ->
            IntArray(rows) { j -> shape[j][cols - 1 - i] }
        }

        val oldShape = shape
        shape = rotated

        if (!isValid(this)) {
            shape = oldShape
        }
    }

    fun moveLeft(isValid: (Piece) -> Boolean) {
        x--
        if (!isValid(this)) {
            x++
        }
    }

    fun moveRight(isValid: (Piece) -> Boolean) {
        x++
        if (!isValid(this)) {
            x--
        }
    }

    fun moveDown(isValid: (Piece) -> Boolean): Boolean {
        y++
        if (!isValid(this)) {
            y--
            return false
        }
        return true
    }
}