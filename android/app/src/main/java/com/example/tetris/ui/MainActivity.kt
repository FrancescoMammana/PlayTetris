package com.example.tetris.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tetris.R
import com.example.tetris.game.GameEngine
import kotlin.math.abs
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
class MainActivity : AppCompatActivity() {

    private enum class Action {
        LEFT, RIGHT, DOWN, ROTATE, HARD_DROP, PAUSE
    }

    private val engine = GameEngine()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var loopJob: Job? = null

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchMoved = false

    private lateinit var boardView: BoardView
    private lateinit var nextPieceView: NextPieceView
    private lateinit var scoreView: TextView
    private lateinit var linesView: TextView
    private lateinit var levelView: TextView
    private lateinit var playBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var gameOverView: View
    private lateinit var finalScoreView: TextView
    private lateinit var finalLinesView: TextView
    private val speedViews = mutableMapOf<Int, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        boardView = findViewById(R.id.boardView)
        nextPieceView = findViewById(R.id.nextPieceView)
        scoreView = findViewById(R.id.scoreView)
        linesView = findViewById(R.id.linesView)
        levelView = findViewById(R.id.levelView)
        playBtn = findViewById(R.id.playBtn)
        pauseBtn = findViewById(R.id.pauseBtn)
        gameOverView = findViewById(R.id.gameOverView)
        finalScoreView = findViewById(R.id.finalScore)
        finalLinesView = findViewById(R.id.finalLines)

        boardView.connect(engine)
        nextPieceView.connect(engine)

        engine.onGameOver = { score, lines -> showGameOver(score, lines) }

        boardView.setOnTouchListener { _, event -> onBoardTouch(event) }

        playBtn.setOnClickListener { startGame() }
        pauseBtn.setOnClickListener { togglePause() }

        val touchButtons = listOf(
            R.id.btnLeft to Action.LEFT,
            R.id.btnRotate to Action.ROTATE,
            R.id.btnRight to Action.RIGHT,
            R.id.btnHardDrop to Action.HARD_DROP,
            R.id.btnDown to Action.DOWN,
            R.id.btnPause to Action.PAUSE
        )
        touchButtons.forEach { (id, action) ->
            findViewById<TextView>(id).setOnClickListener { handleAction(action) }
        }

        val speedIds = listOf(
            1 to R.id.speed1,
            2 to R.id.speed2,
            3 to R.id.speed3,
            4 to R.id.speed4,
            5 to R.id.speed5,
            6 to R.id.speed6
        )
        speedIds.forEach { (speed, id) ->
            findViewById<TextView>(id).apply {
                isSelected = speed == engine.gameSpeed
                setOnClickListener { setSpeed(speed) }
            }
            speedViews[speed] = findViewById(id)
        }

        pauseBtn.isEnabled = false
        findViewById<BoardView>(R.id.boardView).invalidate()
    }

    override fun onDestroy() {
        loopJob?.cancel()
        super.onDestroy()
    }

    private fun startGame() {
        if (engine.running) return

        engine.start()
        playBtn.isEnabled = false
        pauseBtn.isEnabled = true
        pauseBtn.text = getString(R.string.pause)
        gameOverView.visibility = View.GONE

        updateHud()
        boardView.invalidate()
        nextPieceView.invalidate()
        startLoop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive && engine.running) {
                delay(engine.fallSpeedMs().toLong())
                if (!engine.paused && !engine.gameOver) {
                    engine.step()
                    boardView.invalidate()
                    nextPieceView.invalidate()
                    updateHud()
                }
            }
        }
    }

    private fun setSpeed(speed: Int) {
        engine.setSpeed(speed)
        speedViews.forEach { (n, view) -> view.isSelected = n == speed }

        if (engine.running) {
            startLoop()
        }
    }

    private fun togglePause() {
        engine.togglePause()
        pauseBtn.text =
            if (engine.paused) getString(R.string.resume) else getString(R.string.pause)
    }

    private fun showGameOver(score: Int, lines: Int) {
        finalScoreView.text = getString(R.string.final_score, score)
        finalLinesView.text = getString(R.string.final_lines, lines)
        playBtn.isEnabled = true
        pauseBtn.isEnabled = false
        pauseBtn.text = getString(R.string.pause)

        gameOverView.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        boardView.invalidate()
    }

    private fun handleAction(action: Action) {
        if (action == Action.PAUSE) {
            togglePause()
            return
        }

        if (!engine.canControlPiece()) return

        when (action) {
            Action.LEFT -> engine.moveLeft()
            Action.RIGHT -> engine.moveRight()
            Action.DOWN -> engine.softDrop()
            Action.ROTATE -> engine.rotate()
            Action.HARD_DROP -> engine.hardDrop()
            Action.PAUSE -> Unit
        }

        updateHud()
        boardView.invalidate()
        nextPieceView.invalidate()
    }

    private fun onBoardTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchMoved = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - touchStartX
                val deltaY = event.y - touchStartY

                if (abs(deltaX) > 24f) {
                    val moves = floor(abs(deltaX) / 24f).toInt()
                    repeat(moves) {
                        handleAction(if (deltaX > 0) Action.RIGHT else Action.LEFT)
                    }
                    touchStartX = event.x
                    touchMoved = true
                }

                if (deltaY > 32f) {
                    handleAction(Action.DOWN)
                    touchStartY = event.y
                    touchMoved = true
                }
            }

            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - touchStartX
                val deltaY = event.y - touchStartY

                if (!touchMoved && abs(deltaX) < 12f && abs(deltaY) < 12f) {
                    handleAction(Action.ROTATE)
                } else if (deltaY > 80f && abs(deltaX) < 30f) {
                    handleAction(Action.HARD_DROP)
                }
            }
        }
        return true
    }

    private fun updateHud() {
        scoreView.text = engine.score.toString()
        linesView.text = engine.lines.toString()
        levelView.text = engine.level.toString()
    }
}