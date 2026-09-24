package com.example.tetris

import com.example.tetris.data.Piece
import com.example.tetris.data.TetrominoType
import com.example.tetris.game.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun completeLineScoresCorrectlyWithGameSpeed() {
        val engine = GameEngine(
            gameSpeed = 3,
            pieceFactory = { Piece(TetrominoType.I, 3) }
        )
        engine.start()

        for (x in 0 until GameEngine.WIDTH) {
            if (x !in 3..6) {
                engine.board[19][x] = TetrominoType.T.color
            }
        }

        engine.hardDrop()

        assertEquals(1 * 100 * 3, engine.score)
        assertEquals(1, engine.lines)
        assertEquals(1, engine.level)
        assertTrue(engine.board[19].all { it == 0 })
    }

    @Test
    fun repeatedLinesRaiseLevel() {
        val engine = GameEngine(
            gameSpeed = 1,
            pieceFactory = { Piece(TetrominoType.I, 3) }
        )
        engine.start()

        repeat(10) {
            for (x in 0 until GameEngine.WIDTH) {
                if (x !in 3..6) {
                    engine.board[19][x] = TetrominoType.T.color
                }
            }
            engine.hardDrop()
        }

        assertEquals(10, engine.lines)
        assertEquals(10 * 100, engine.score)
        assertEquals(2, engine.level)
    }

    @Test
    fun pieceLocksAtBoardEdges() {
        val engine = GameEngine()
        engine.start()

        val piece = engine.currentPiece ?: error("pezzo non presente")
        val width = piece.shape[0].size

        repeat(100) { engine.moveLeft() }
        assertTrue(engine.currentPiece!!.x >= 0)

        repeat(100) { engine.moveRight() }
        assertTrue(
            engine.currentPiece!!.x + engine.currentPiece!!.shape[0].size <= GameEngine.WIDTH
        )
    }

    @Test
    fun softDropAddsGameSpeedPerCell() {
        val engine = GameEngine(
            gameSpeed = 2,
            pieceFactory = { Piece(TetrominoType.O, 4, 0) }
        )
        engine.start()

        val piece = engine.currentPiece!!

        engine.softDrop()

        assertEquals(2, engine.score)
        assertEquals(1, piece.y)
    }

    @Test
    fun gameOverOnBlockedSpawn() {
        var gameOverNotified = false
        val engine = GameEngine(
            pieceFactory = { Piece(TetrominoType.I, 0) }
        )
        engine.onGameOver = { _, _ -> gameOverNotified = true }
        engine.start()

        var guard = 0
        while (!engine.gameOver && guard < 50) {
            engine.hardDrop()
            guard++
        }

        assertTrue(engine.gameOver)
        assertTrue(gameOverNotified)
        assertFalse(engine.running)
    }

    @Test
    fun fallSpeedFollowsOriginalFormula() {
        val engine = GameEngine()
        assertEquals(600, engine.fallSpeedMs())
        engine.setSpeed(2)
        assertEquals(500, engine.fallSpeedMs())
        engine.setSpeed(6)
        assertEquals(100, engine.fallSpeedMs())
    }
}