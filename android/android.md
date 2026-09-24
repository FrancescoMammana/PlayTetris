# Piano di sviluppo — Tetris Android (Kotlin)

## Obiettivo
Replica fedele del gioco `index.html` (Tetris 10×20) come app Android in Kotlin, con la stessa logica, punteggio, velocità e controlli touch. UI in italiano (come il gioco originale).

## Prerequisiti e vincoli dell'ambiente
- Sistema: **Ubuntu 24.04 ARM64 (aarch64)** dentro VM QEMU **senza KVM**.
- Android Studio **ufficiale non supporta Linux ARM64**; l'**emulatore è inutilizzabile** in questa VM (niente accelerazione hardware).
- Strategia: **toolchain CLI ARM64 in locale** per iterare e compilare l'APK; il progetto sarà un progetto Gradle standard, **apribile tale e quale** su un PC con Android Studio (Windows/macOS/x86 Linux). Test funzionali su **device fisico via adb** o su PC con emulatore.
- Stesso repo GitHub (sottocartella `android/`), senza toccare `index.html`. Il deploy su Pages continua a funzionare.

## Regole di gioco da replicare (dal codice originale)
- Board `10×20`; 7 tetromino (I, O, T, S, Z, J, L) definiti come matrici `0/1` con colore dedicato.
- Caduta con `setInterval` → in Kotlin **coroutine** con `delay()`: `speed = max(600 - (gameSpeed-1)*100, 100)` ms (formula reale del codice, non quella in AGENTS.md).
- `gameSpeed` 1–6 selezionabile in game (riavvia il loop).
- Punteggio: riga completata → `score += completedLines * 100 * gameSpeed`; soft-drop → `score += gameSpeed` per cella.
- Livello: `level = lines / 10 + 1` (divisione intera, parte da 1).
- Rotazione: ruota la matrice a destra; se il pezzo non è valido, annulla il cambio (stessa logica `isValid()` usata da move).
- Game over: se un blocco del pezzo bloccato finisce fuori dalla board sopra (`y < 0`) oppure il nuovo pezzo spawna in posizione non valida.
- Preview del prossimo pezzo in una griglia 4×4.
- Touch board (stesse soglie del JS):
  - swipe orizzontale ogni 24px → 1 passo destra/sinistra
  - swipe verso il basso `ΔY > 32` → scendi di 1
  - tap (`ΔX < 12` e `ΔY < 12`) → ruota
  - swipe verso il basso `ΔY > 80` (quasi verticale) → hard drop
- Pulsanti a schermo: ◀ ⟳ ▶ ⤓ ▼ ⏯ → stessa `handleAction(action)`.

## Fase 0 — Setup toolchain (CLI ARM64)
1. `apt install openjdk-17-jdk git unzip`
2. Installare Android SDK Command-line tools nella distribuzione ARM64 (port community) in `~/Android/Sdk` con `platforms;android-34` e `build-tools;34.0.0`.
3. Override `aapt2` ARM64 in `~/.gradle/gradle.properties` → `android.aapt2FromMavenOverride=<sdk>/build-tools/34.0.0/aapt2` (Google distribuisce `aapt2` solo per x86_64).
4. Verifica: `gradle --version`, `aapt2 version`.

## Fase 1 — Struttura del progetto (in `android/` nel repo)
```
android/
├── settings.gradle.kts
├── build.gradle.kts           (AGP + kotlin plugin, repository google/mavenCentral)
├── gradle.properties
├── gradle/wrapper/…           (Gradle wrapper per build riproducibili)
├── local.properties           (sdk.dir → ~/Android/Sdk)
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts       (compileSdk 34, minSdk 24, targetSdk 34, test JUnit)
    └── src/
        ├── main/AndroidManifest.xml
        ├── main/java/com/example/tetris/
        │   ├── data/TetrominoType.kt
        │   ├── data/Piece.kt
        │   ├── game/GameEngine.kt
        │   ├── ui/BoardView.kt
        │   ├── ui/NextPieceView.kt
        │   └── ui/MainActivity.kt
        ├── main/res/layout/activity_main.xml
        ├── main/res/values/colors.xml, strings.xml (IT), themes.xml
        └── test/java/…/GameEngineTest.kt
```

## Fase 2 — Modello dati (`data/`)
**`TetrominoType.kt`** — `enum class TetrominoType(val shape: Array<IntArray>, val color: Int)`:
- I, O, T, S, Z, J, L con le **stesse** matrici di `index.html`.
- Mappa della stringa colore CSS → `Color` Android (es. `#00f0f1`, `#f0f000`, `#a000f0`, …).

**`Piece.kt`** — `class Piece(val type, var shape, var x, var y)`:
- copy della matrice (parallelo del deep-copy JSON): `Array(rows){ r -> IntArray(cols){ c -> shape[r][c] } }`
- `moveLeft/Right/Down()` e `rotate()` con validazione (riceve una callback `(Piece) -> Boolean` che è `engine::isValid`, stesso pattern `if (!isValid()) annulla`).

## Fase 3 — Logica di gioco (`game/GameEngine.kt`)
Classe **pura senza dipendenze Android** (testabile su JVM):
- Stato: `board: Array<IntArray>`, `currentPiece`, `nextPiece`, `score`, `lines`, `level`, `gameSpeed`, flag `running/paused/gameOver`.
- Metodi: `initBoard()`, `isValid()`, `lockPiece()`, `checkLines()`, `hardDrop()`, `stepMoveDown()`, `spawn()` (con pezzo casuale + next).
- Eventi: lambdas `var onGameOver: ((score, lines) -> Unit)?` per notificare la UI.
- Colori sulla board: memorizzati come **Int** (colore ARGB) invece delle stringhe CSS.

## Fase 4 — Loop di gioco
- `CoroutineScope(Dispatchers.Main)` dentro `MainActivity` (o ViewModel):
  ```kotlin
  job = scope.launch {
      while (isActive && engine.running) {
          delay(engine.fallSpeedMs())
          if (!engine.paused && !engine.gameOver) {
              engine.step()          // moveDown + lock + checkLines
              boardView.invalidate() // ridisegna
              updateHud()
          }
      }
  }
  ```
- `setSpeed(s)` → `gameSpeed = s` e (se in game) `job?.cancel(); startLoop()`.

## Fase 5 — Rendering (Custom View + Canvas)
**`BoardView.kt`** (`class BoardView(context, attrs) : View`):
- `override fun onDraw(canvas: Canvas)` con `Paint`:
  - sfondo nero + bordi + griglia (colori originali `#00d4ff`…)
  - blocchi fissi della board (`board[y][x]`)
  - pezzo corrente disegnato sopra
- Funzione `render()` identica nel concept a `render()` del JS: a ogni passo **`invalidate()`** → `onDraw` ridisegna tutto.
- Dimensione celle: `CELL_SIZE` (idealmente una unità che scala con lo schermo).

**`NextPieceView.kt`** — griglia 4×4 per la preview del prossimo pezzo (parallelo di `renderNextPiece()`), celle `30dp`.

## Fase 6 — Input
- `BoardView.setOnTouchListener`: `ACTION_DOWN` salva startX/startY; `ACTION_MOVE` applica le **stesse soglie** del JS (24px orizz, 32px giù, aggiornando l'ancora); `ACTION_UP`: tap→rotate, `ΔY>80`→hardDrop.
- Pulsanti on-screen (XML) → `handleAction('left'|'right'|'down'|'rotate'|'hardDrop'|'pause')`, stessa semantica del `dataset.action`.

## Fase 7 — Layout e HUD (`activity_main.xml`)
- Layout: titolo TETRIS, `BoardView` centrata, pannelli PUNTEGGIO / LINEE / LIVELLO (TextView con colori originali), `NextPieceView`, pulsanti INIZIA e PAUSA, selettore velocità 1–6 con stato `.active`, riga pulsanti touch. Testi in **italiano**.
- Overlay **GAME OVER** (bordo rosa `#ff006e`, pulsazione come in CSS) con punteggio e linee finali.

## Fase 8 — Test e verifica
- `GameEngineTest.kt` (JUnit 4): riga completa → punteggio corretto con `gameSpeed`; lock ai bordi; game over su spawn bloccato; soft-drop punti.
- Build: `cd android && ./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
- APK installabile su device fisico (`adb install`), oppure aprire il progetto su PC con **Android Studio** per emulatore.

## Extra (opzionali, fuori scope iniziale)
- Salvataggio del miglior punteggio (`SharedPreferences`).
- Workflow GitHub Actions che compila l'APK su runner x86_64 e lo mette tra gli artifact.

## Rischi / note
- Toolchain `aapt2` ARM64 non ufficiale: se l'override fallisse, il fallback è buildare solo via **GitHub Actions** (runner x86_64) e non in locale.
- L'emulatore **non** funzionerà su questa VM (no KVM): la verifica visiva si fa su device fisico/PC con Android Studio.
- Nessuna modifica a `index.html` né al layout del repo esistente.