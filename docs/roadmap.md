# Roadmap

What's planned, what's done. Pick something up — each item is small enough to finish in a session.

Camera decision: **Limelight 3A**. Webcams crash and slow startup; the Limelight has held up over
whole seasons. Non-negotiable consequence: if the camera doesn't load, or dies mid-match, the robot
keeps driving on odometry alone.

## Tier 1 — cheap wins

- [x] `setBulkReading()` in `Iapetus` — one line, real loop-time gain. Pedro's accuracy depends on loop time.
- [x] `DriveTurnBy` / `DriveTurnTo` extending `DriveAbstract` — SolversLib's turn commands have no timeout and no cleanup; ours will.
- [x] Loop-time + match-time telemetry in `Sensors` — a slow loop causes a lot of "it drifted" reports.
- [x] `PersistentPoseManager` — hand the robot's pose from autonomous to teleop. Ganymede had this; Artemis lost it.

## Tier 2 — Panels

- [x] Live-editable `Tunables` (drive feel, tolerances, drawing toggles). Drivetrain tuning deliberately left to Pedro's AutoTune — two places to edit one number is how you lose an afternoon.
- [x] Draw target pose + a line from current position — intent vs. reality.
- [x] Alliance-coloured robot; tag-sighting draw ready for Tier 3.
- [x] **Frame-of-reference markers** — origin, both axes, centre, far corner. Catches a Panels/Pedro frame mismatch in two seconds without moving the robot. See docs/coordinates.md.

## Tier 3 — Vision

- [x] `FieldMap.java` — reads tags and clusters from the SDK's own library (no hand-typed ids or coordinates; updates itself at kickoff).
- [x] Coordinate bridge — metres/degrees → Pedro inches/radians, isolated in `FieldMap`, Pedro 3's 90° rotation included.
- [x] **Tag tracking** — every tag in view placed on the field via odometry, remembered briefly, merged into targets by SDK cluster. `TagSighting` answers range, bearing, and aim-from-a-pivot for mechanisms. `DriveFaceTarget` is the example.
- [x] **Localization** (games with fixed tags) — MegaTag2 + trust policy, kept behind `TAG_LOCALIZATION` / `VISION_CORRECTIONS_ENABLED`, both off until proven.
- [x] `CameraCalibration` — tag check (any game) and field check (fixed tags only).

**Trust policy: snap, but only when confident.** Overwrite Pedro's pose only when the detection is
fresh, close, on the field, and the implied jump is small. Log every rejection with its reason —
you learn more from why it declined than from when it worked.

Game-specific uses (which targets, an arm that aims) belong in the season repo. See
[vision.md](vision.md#what-belongs-in-a-season-repo-instead).

## Needs the robot

- [ ] Tag check in `CameraCalibration`: confirm "left" is positive to the robot's left (the Limelight's robot-space Y points right; we flip it)
- [ ] Limelight pipeline: full 3D on, the game's tag size, robot-space position from `Constants.CAMERA_*`
- [ ] Confirm the Panels Field tab and overlay on 1.0.12
- [ ] Confirm `Tunables` live-edit round-trips from the dashboard
- [ ] Re-check tag ids/sizes at kickoff — the game changes, the code shouldn't need to

## After the build

- [ ] Full review of docs + agent guidance against what actually got built.
