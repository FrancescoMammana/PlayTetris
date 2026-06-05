# PlayTetris — Agent guide

## Project

Pure HTML/CSS/JS Tetris game in a single `index.html`. No build tools, no npm, no package.json.

## Commands

| Action | Command |
|---|---|
| Run locally | Open `index.html` in browser, or `npx serve` / `live-server` |
| Debug in VS Code | Launch the "Launch Chrome against localhost" config (needs a server on `:8080`) |
| Deploy | Push to `main` → GitHub Actions deploys to Pages (uploads entire repo root) |

## Architecture

- **Single file**: `index.html` — CSS in `<style>`, JS in `<script>` at bottom.
- **No frameworks**, no modules, no imports. All state is global variables in the script.
- **Board**: 10×20 grid, `BOARD_WIDTH`/`BOARD_HEIGHT` constants. Rendered via DOM `<div>` elements.
- **Pieces**: 7 tetrominoes (I, O, T, S, Z, J, L) defined in `PIECES` object with shape matrices and CSS color classes.
- **Scoring**: `score += completedLines * 100 * gameSpeed`. Level = `floor(lines / 10) + 1`.
- **Game loop**: `setInterval` fall timer, speed = `max(100 - (speed-1)*15, 50)` ms.

## Controls

- Keyboard: arrows (move/rotate), space (hard drop), P (pause).
- Touch: swipe left/right/down, tap to rotate, swipe far-down for hard drop.
- Touch buttons: on-screen grid for mobile (auto-shown on touch devices via `@media (hover: none)`).

## CI

GitHub Pages deploy on push to `main`. No test suite, no linting, no typechecking.

## Conventions

- UI labels and README are in **Italian**.
- Block colors: CSS classes `.color-i`, `.color-o`, `.color-t`, `.color-s`, `.color-z`, `.color-j`, `.color-l`.
- Speed levels 1–6, buttons with `.speed-btn.active` class.
