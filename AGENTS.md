# Working in Iapetus

Iapetus is the 2026-27 BIOBUZZ robot, built on Artemis (the game-agnostic base). Game-specific
work goes here; anything that would help any game belongs in Artemis. See docs/biobuzz.md.

FTC robot code maintained by high schoolers. Some are seeing a command scheduler for the first
time; some have three seasons on it and will notice if you're sloppy. Write for both.

**Tone:** a little personality is welcome — this is a codebase named after a moon goddess for a
team called Moonshots. One good joke per file, not per line, and never in place of an explanation.
Absurd variable names are fine when they're also the clearest name (`patience` for a timeout,
`breadcrumbs` for a pose trail, `megaphone` for telemetry). If a comment takes more than 15
seconds to read, cut it.

## Rule 1: don't break the merge

Artemis tracks `upstream` = `FIRST-Tech-Challenge/FtcRobotController` — the FTC SDK itself — and
pulls it in every season. Every file upstream can touch must stay byte-for-byte theirs, so that
merge stays silent forever.

**Never hand-edit:** `README.md`, `build.gradle`, `build.common.gradle`,
`build.dependencies.gradle`, `gradle.properties`, `settings.gradle`, `gradle/`, `gradlew*`,
`FtcRobotController/`, `.github/*`.

**Always safe:** `docs/`, `scripts/`, `MOONSHOTS.md`, `AGENTS.md`, `CLAUDE.md`,
`TeamCode/build.gradle` (FIRST ships it nearly empty and expects teams to add dependencies), and
everything under `TeamCode/.../teamcode/` — `commands/`, `subsystems/`, `utils/`, `Iapetus.java`.

**Those three folders are the whole structure.** Don't add a fourth. OpModes live in `utils/`.

SolversLib, Pedro Pathing, and Panels are Gradle *dependencies*, not forks — update them by
bumping a version in `TeamCode/build.gradle`, never by merging anything.

Need something in the first list? You don't. Add a file beside it, or ask a human. This layout is
the entire point of the repo — don't undo it because a one-line edit looked easier.

Check yourself anytime: `./scripts/check-structure.sh`. It reports drift and changes nothing.
Run it before you commit, and after Android Studio offers you any kind of upgrade.

`scripts/hooks/pre-commit` refuses commits that edit those files. **Never bypass it with
`--no-verify`.** If it fires on your commit, the edit is the mistake. Roll it back.

## Rule 2: telemetry has exactly one exit

`robot.sensors.addTelemetry(key, value)`. Once the robot is running, never `telemetry.update()`
anywhere but `Sensors.periodic()`. Two flushes = half your data, no error message, one lost
afternoon. (An OpMode flushing its own init prompt, before `Iapetus` exists, is fine.)

## Rule 3: `execute()` never blocks

No while-loops, no `Thread.sleep()` in a Command. The scheduler runs every active command's
`execute()` once per loop. Block one, freeze all. Wait by checking a timer in `isFinished()`.

## Where things go

```
Iapetus.java                 subsystems, button bindings, autonomous plan

commands/Drive               default teleop drive
commands/DriveAbstract       base for anything that moves the robot (timeout + cleanup)
commands/DriveToPose         ├ go stand exactly there
commands/DriveToPoseByPoint  ├ go stand there, bending through a point on the way
commands/DriveFwdByDist      ├ go that way N inches
commands/DriveTurnBy         ├ rotate N degrees
commands/DriveTurnTo         ├ face this heading
commands/DriveFaceTarget     └ face the nearest tracked target (the TagSighting example)
commands/IntakeManual        default: operator's stick runs the intake (dead zone)
commands/LaunchManual        default: operator's trigger + feed (dead zone)
commands/LaunchAbstract      base for pre-programmed launches (timeout + cleanup)
commands/LaunchDynamic       └ the auto-shot: spin up, feed, stop (auto AND teleop)

subsystems/PedroDrive        mecanum + Pedro + dashboard drawing — tune it, don't rewrite it
subsystems/Sensors           every shared sensor, Limelight tag tracking, AND the only telemetry flush
subsystems/Intake, Launcher  this season's mechanisms. They never read a gamepad.

utils/Constants              hardware names, motor directions, follower config (final)
utils/Tunables               values you edit live from the dashboard (not final)
utils/FieldMap               tag knowledge + every coordinate conversion
utils/TagSighting            a tracked tag/target: field position, range, bearing, aim-from-pivot
utils/PersistentPoseManager  auto → teleop pose handoff
utils/DriveyMcDriverson      teleop entry point
utils/AutoMcAutty            autonomous entry point
utils/CameraCalibration      is the camera telling the truth? (all settings live on Panels)
utils/LauncherTuner          find the launch speed that scores (ours, not Pedro's)
utils/PinpointDoctor         is the odometry computer alive? No Pedro, no robot class
utils/Tuning                 Pedro AutoTune — tune from a browser, see docs/tuning.md
utils/{Mecanum,Pinpoint,Foresight}Tuner, utils/Tests   the tuners it runs (copied from Pedro — don't edit)
```

Adding a mechanism? Copy Ganymede's subsystems (`Intake`, `Launcher`, `Turret`):
https://github.com/GA-Moonshots/Ganymede/tree/master/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems
Hardware lookups in the constructor, hardware names from `Constants`, small methods that each do
one thing. Not `PedroDrive`: it wraps Pedro and is no model for a mechanism. Adding a movement? Extend
`DriveAbstract`, and give it a real timeout.

Subsystems never read a gamepad. The operator's controls go in a default command
(`IntakeManual`, `LaunchManual`), dead zone included. A pre-programmed action is its own command
that requires the subsystem: scheduling it interrupts the default command, and the default takes
over again when it ends. That's how one command (`LaunchDynamic`) serves auto and a teleop button.
In `Iapetus`, `register()` `Sensors` last: its `periodic()` is the telemetry flush.

## Rule 4: numbers have exactly one home

`Constants` for things that must not change mid-match (hardware names, directions, follower
config). `Tunables` for things worth editing live from the dashboard. Never both — a value defined
in two places is a value that will disagree with itself at the worst moment.

Coordinates convert **once, at the edge**, in `FieldMap`. A conversion buried in the middle of a
command is a bug waiting for a Saturday.

## Before you start

1. [docs/architecture.md](docs/architecture.md) — one page, explains the whole repo.
2. Writing a path, or a coordinate looks wrong? [docs/coordinates.md](docs/coordinates.md) first.
   Panels and Pedro do not agree on where (0,0) is, and getting this wrong is silent.
3. Changing drive behaviour? The numbers are in `utils/Constants.java` or `utils/Tunables.java`,
   never scattered through the code.
4. Build check: `./scripts/build.sh`. Structure check before you commit:
   `./scripts/check-structure.sh`. Java-8 deprecation warnings on a modern JDK are expected noise.
   Students deploy from Android Studio; `./scripts/deploy.sh` does the same from a terminal.
5. Lost an hour to something? Add it to [docs/issue-log.md](docs/issue-log.md) instead of fixing
   it silently. Planning something bigger? [docs/roadmap.md](docs/roadmap.md).
