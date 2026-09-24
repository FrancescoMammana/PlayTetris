package com.example.tetris.data

enum class TetrominoType(val shape: Array<IntArray>, val color: Int) {

    I(
        arrayOf(intArrayOf(1, 1, 1, 1)),
        0xFF00F0F1.toInt()
    ),
    O(
        arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)),
        0xFFF0F000.toInt()
    ),
    T(
        arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)),
        0xFFA000F0.toInt()
    ),
    S(
        arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)),
        0xFF00F000.toInt()
    ),
    Z(
        arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
        0xFFF00000.toInt()
    ),
    J(
        arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)),
        0xFF0000F0.toInt()
    ),
    L(
        arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)),
        0xFFF0A000.toInt()
    );

    companion object {
        fun random(): TetrominoType {
            return entries[(entries.indices).random()]
        }
    }
}