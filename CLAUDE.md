# Iapetus — agent rules (built on Artemis)

@AGENTS.md

The import above pulls in the full guidance. If your tool doesn't support `@` imports, open
`AGENTS.md` yourself before doing anything. The four rules that matter most, restated here so
they're in context no matter what:

## 1. Never edit files upstream owns

Upstream is `FIRST-Tech-Challenge/FtcRobotController` (the FTC SDK), merged every season. These
stay byte-for-byte upstream's:

`README.md` · `build.gradle` · `build.common.gradle` · `build.dependencies.gradle` ·
`gradle.properties` · `settings.gradle` · `gradlew*` · `gradle/` · `FtcRobotController/` ·
`.github/`

Our layer — safe to edit: `docs/`, `scripts/`, `MOONSHOTS.md`, `AGENTS.md`, `CLAUDE.md`,
`TeamCode/build.gradle`, and `TeamCode/.../teamcode/{commands,subsystems,utils}/` + `Iapetus.java`.

**Exactly three folders under `teamcode/`: `commands`, `subsystems`, `utils`. Don't add a fourth.**

SolversLib / Pedro / Panels are Gradle dependencies — update by version bump, never by merge.

Think you need to edit a protected file? You don't. Add a file beside it, or ask a human.
Verify anytime with `./scripts/check-structure.sh`. The pre-commit hook enforces it: never
`--no-verify` past it.

## 2. Telemetry has exactly one exit

`robot.sensors.addTelemetry(key, value)`. Never call `telemetry.update()` outside
`Sensors.periodic()`. Two flushes = half your data, no error, one lost afternoon.

## 3. `execute()` never blocks

No while-loops, no `Thread.sleep()` in a Command. The scheduler runs every active command's
`execute()` once per loop — block one, freeze all. Wait by checking a timer in `isFinished()`.

## 4. One frame, converted once

Everything in this repo is in Pedro's frame: inches, origin at a field **corner**, 0 rad = +X.

Panels' canvas is **centre**-origin and its Pedro preset applies a −72/−72 shift, a 90° rotation
*and* a Y-flip. The Limelight reports metres. Every conversion happens in `utils/FieldMap`, at the
edge — never halfway through a command.

This is the most expensive mistake available here: nothing errors, the numbers just quietly mean
something else, and it stays self-consistent while being wrong. It cost this team a season. Read
`docs/coordinates.md` before writing a path.

---

Structure, tone, and the rest: `AGENTS.md`. Architecture: `docs/architecture.md`.
