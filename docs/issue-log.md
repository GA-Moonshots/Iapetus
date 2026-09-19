# Issue log

Dated gotchas. Add one when you lose an hour to something — that's the whole point.

Format: `## YYYY-MM-DD — title`, then what broke, why, and the fix.

---

## 2026-09-19 — The structure check said "Clean" when it hadn't checked

**`check-structure.sh` printed "✓ Clean" after skipping its main check.** With no `upstream`
remote, an unfetched one, or the wrong repo, it warned, skipped the committed-drift comparison, and
then said upstream's files were untouched. A fresh clone has no `upstream` remote, so that was
everyone's first run. `doctor.sh` repeated it as "Repo structure clean". Now a skipped check says
so and exits 2, and `doctor.sh` reports "only partly checked". Every branch was driven in a scratch
clone: clean, not fetched, wrong repo, no remote, uncommitted edit, committed edit.
The flip side of *"a checker that cries wolf is worse than no checker"*: a checker that says
"clean" about something it never looked at is worse too.

**`diagnostics.md` said the pod offsets match Ganymede exactly.** They did until 2026-09-17, when
the Pinpoint Tuner re-measured them and both signs flipped. The retracted-lead entry below was
true when it was written; the doc now says which settings still match and which don't.

## 2026-09-19 — Vision: a heading in the wrong frame, and tags placed from the wrong moment

**MegaTag2 was told our heading in Pedro's frame.** `limelightToPedro()` reads the camera's yaw as
FTC-frame and subtracts 90°, so the heading we hand back with `updateRobotOrientation()` has to go
the other way. It didn't: it was Pedro's, 90° off. Only matters with `TAG_LOCALIZATION` on.
`FieldMap.pedroHeadingToFtc()` now does it, at the edge, where every other conversion lives.

**Tags were placed using where the robot is now, not where it was when the frame was taken.** A
frame can be up to `VISION_MAX_STALENESS_MS` (200 ms) old. Turning at 2 rad/s over 40 ms of
latency is about 5°, which moves a tag 48" away by about 4". Breadcrumbs couldn't fix it (they're
spaced by distance, and a turn in place drops none), so `PedroDrive` keeps half a second of
timestamped poses and `poseAt()` looks one up. Checked off-robot against the compiled class.

**The uploaded Limelight map said `"frc"`.** Now `"ftc"`, matching the FTC centre-origin
coordinates it carries. Limelight's docs list both types without saying what the type changes, so
this is **unverified on hardware**, like the rest of that upload.

## 2026-09-19 — A dashboard dial wired to nothing

**`Tunables.DRIVE_SPEED` did nothing.** Nothing read it. The speed `PedroDrive` actually used was a
private field seeded from `Constants.DEFAULT_DRIVE_SPEED`, and its setter had no callers. So one
number had two homes, and the one on the dashboard was the dead one: drop it for a nervous driver
and the robot, and the "Speed" telemetry line, stayed at 100%. No error anywhere. Fixed:
`PedroDrive.getDriveSpeed()` reads the Tunable every call, clamped to `Constants.MIN/MAX_DRIVE_SPEED`.
**A Tunable only works if it's read where it's used, every loop.** Before trusting a new one,
grep for a reader.

Also corrected while checking the rails: `architecture.md` quoted SolversLib's turn commands as
ending on `!follower.isBusy()`. That was 0.3.5; 0.3.6 checks heading tolerance. Still no timeout
and no `end()`, and its suggested `withTimeout(...)` ends the command but leaves the follower
holding the turn. And "the ONLY `telemetry.update()` in the project" was only true once the robot
is running: the OpModes flush their own init prompts, which is fine.

## 2026-09-19 — Path end constraints lost in a paste; Pedro 3.0.1

**Foresight's end constraints were gone.** Pasting AutoTune's `foresightConfig` output replaced the
whole block, and AutoTune doesn't generate the end constraints, so paths fell back to Pedro's
defaults for "done". It happened in Artemis and Iapetus both, despite the warning in tuning.md.
Restored, with a louder comment right where the paste lands.

**Pedro 3.0.1** includes the fix for 3.0.0 dropping the start pose (the Pinpoint resets in the
localizer's constructor, and a pose set straight afterwards could be ignored, so autonomous
started at (0,0)). Iapetus had worked around it with a 500 ms wait; bumping is the real fix.

## 2026-09-19 — Field tab gone, frame still Pedro 2, repo re-forked

**Panels showed no Field tab**, so no robot overlay. `fullpanels` 1.0.13's only change is `field`
1.0.7, and that `.aar` has no web files (1.0.6 has six). Nothing errors; the tab just isn't there.
Pinned `fullpanels:1.0.12`. Iapetus found this first.

**`FieldMap.ftcToPedro()` was still the Pedro 2 version**, a +72 shift. Pedro 3 publishes a 90°
rotation as well. It went unnoticed because vision corrections are off this season, so the only
symptom was tags drawn in the wrong place. Ported the Iapetus fix.

**Dropped two unused dependencies.** `solverslib:pedroPathing` (never imported; tying SolversLib
to a Pedro major is the thing we'd rather avoid) and FTC Dashboard (never imported, and a second
web server next to Panels).

**A game can have no fixed tags at all.** Correcting the 09-16 entry: BIOBUZZ's tags don't ship
with field positions. SDK 12.0 introduced *clusters* (several tags on one moving object, no field
position), so `FieldMap.localizationTags()` came back empty and vision was quietly computing and
drawing nothing useful. Vision now *tracks* tags, which works whether they move or not, and keeps
localization behind a switch for games that have fixed tags: [vision.md](vision.md). The game's
specifics live in its season repo. The Limelight's robot-space Y points **right** while ours
points left; the flip lives in `FieldMap.limelightTargetToRobot()`.

**GitHub fork re-parented.** `GA-Moonshots/Artemis` was a fork of the SolversLib Quickstart. It is
now a fork of `FIRST-Tech-Challenge/FtcRobotController`, created fresh and fast-forwarded to our
history. The old repo is `GA-Moonshots/Artemis-quickstart-archive`, archived. GitHub has no
"change parent" button; this is the only way. `doctor.sh` and `check-structure.sh` were still
suggesting the Quickstart as `upstream` and are fixed.

## 2026-09-16 — BIOBUZZ ready: SDK 12.0, SolversLib 0.3.6, Pedro 3, Panels 1.0.13

Our PR (SolversLib #40) was folded into SolversLib's own "Migrate to Pedro 3.0.0" commit and
shipped as **0.3.6**. Moved everything at once, because 0.3.6 only works with Pedro 3 and Pedro 3
couldn't be a version bump anyway:

- **SDK 12.0 merged from FIRST.** Clean merge; only `build.dependencies.gradle`, `README.md`, and
  AprilTag samples changed.
- **Pedro moved to Maven Central** as `com.pedropathing:revhub:3.0.0`. The old
  `maven.pedropathing.com` repo is gone from our build.
- **Pedro 3 renamed nearly everything.** `geometry.Pose` → `math.Pose` with `x()` instead of
  `getX()`. `FollowerBuilder` → `new Follower(localizer, drivetrain, new Foresight(config))`.
  `PathBuilder` → `Paths.line(a, b).constant(heading)`. `followPath` → `follow`.
  `setTeleOpDrive` → `manual(...)`. `breakFollowing` → `stop`. `turn()` / `turnTo()` are gone:
  a turn is now `hold(pose.withHeading(h))`. Headings are stored 0..2π, so anything shown to a human
  goes through `Angle.normalizeSigned` first.
- **Don't use `isBusy()` to end a turn.** While holding, it goes false after Foresight's
  `timeoutConstraint` whether or not the robot arrived. `PedroDrive.isFacing()` checks the actual
  heading instead.
- **Pedro 3 owns the drive motors.** `Mecanum` caches the last power per wheel and skips writes
  that match it, so `PedroDrive` no longer grabs the motors to zero them. That would leave the cache
  stale, and the next matching command would be silently dropped. `stop()` is `follower.stop()`.
- **Tuning is AutoTune now:** a web page at `192.168.43.1:10158`, backed by
  `com.pedropathing:tuning`. The old 17-class `Tuning.java` is replaced; see
  [tuning.md](tuning.md). The Foresight controller values in `Constants` are placeholders until the
  Foresight Tuner runs on this robot.
- **Pedro 3 dropped `PoseHistory`,** so `PedroDrive` keeps its own breadcrumb trail.

**BIOBUZZ AprilTags move.** SDK 12.0's release notes: the tags "are not suitable for absolute Field
Localization." They still ship *with* field positions, so `FieldMap`'s "has a position = landmark"
filter no longer tells them apart. A moved tag reads as confident as a fixed one and pulls odometry
toward where it started, inside the 24" jump limit, with no error. So
`Tunables.VISION_CORRECTIONS_ENABLED` now defaults to **false**, and the tag map is only uploaded
to the Limelight when it's on. Vision still runs and reports what it *would* have done.

**Not yet verified on hardware:** AutoTune's web UI, the new turn logic, and field-centric feel.
Run the tuning sequence before trusting any path.

## 2026-09-08 — Localization dead: Panels frozen, tuner never slows down

Two symptoms, one cause. The Forward Zero Power tuner builds its OWN follower from
`Constants.createFollower()` and touches none of our subsystem code — so the fact that it failed
*too* ruled out `PedroDrive`, `Drive`, and the drawing code immediately, and pointed at localization.

Ruled out by reading the libraries, not guessing:
- **Bulk caching is NOT the culprit.** `MANUAL` mode freezes sensors if nobody clears the cache,
  but SolversLib's `CommandScheduler.run()` calls `clearBulkCache()` at the end of every loop when
  the mode is MANUAL. Verified in its source.
- **`PINPOINT_NAME` was an invisible default.** Pedro defaults `hardwareMapName` to `"pinpoint"`
  internally and we never set it, so a config named anything else would fail silently. Now set
  explicitly in `Constants`.

Added `Pinpoint Doctor` (Diagnostics group): a plain LinearOpMode that reads the device directly —
no Pedro, no SolversLib, no MyRobot — reporting device id, `DeviceStatus`, update rate, and raw
encoder ticks. Push the robot; if ticks move, the sensor is fine and the fault is above it.
Playbook: [diagnostics.md](diagnostics.md).

**Retracted a bad lead.** I flagged `encoderResolution` (`goBILDA_SWINGARM_POD`) as suspect.
Checked Ganymede on GitHub: its Pinpoint block is byte-for-byte identical to ours — same pods,
offsets, and directions — and it worked all last season. So the pod type is right, and config is
not what changed. Ganymede is a useful control whenever localization misbehaves: same team, same
hardware, known-good numbers.

**What did change:** Ganymede ran SDK 11.1; Artemis is on 11.2.1 with Pedro 2.0.6. If the Doctor
reports READY with ticks moving — sensor and config both fine — that version seam is the next
place to look, not our subsystem code.

**Also fixed today, unrelated but noisy:** `check-structure.sh` reported 13 files as drift. All
false. The `upstream` remote had reverted to the SolversLib Quickstart, so the script was comparing
our FIRST-based tree against the wrong repo. Remote repointed at FIRST, and the script now refuses
to run that check at all when `upstream` isn't FIRST's SDK — a checker that cries wolf is worse
than no checker.

## 2026-08-29 — Migrated upstream: Quickstart → FIRST's SDK

The SolversLib Quickstart hadn't been touched since February and held us at SDK v11.1. Repointed
`upstream` to `FIRST-Tech-Challenge/FtcRobotController` (shared git ancestry, so it merged
normally) and jumped to **v11.2.1 / Gradle 9.1 / AGP 8.13.2**. FIRST's `TeamCode` contains only a
readme, which is why `pedroPathing/` and `samples/` are gone — those were Quickstart injections.
`Tuning.java` was rescued into `utils/` before deleting the rest. SolversLib is a *dependency*, not
an upstream; see [updating-from-upstream.md](updating-from-upstream.md).

## 2026-08-29 — SolversLib 0.3.4 hard-pins the SDK version

After moving to SDK 11.2.1 the build died with:

```
Cannot find a version of 'org.firstinspires.ftc:FtcCommon' that satisfies the version constraints:
  org.solverslib:core:0.3.4 --> org.firstinspires.ftc:FtcCommon:{strictly 11.1.0}
```

`{strictly}` can't be overridden by normal resolution. **SolversLib 0.3.5 dropped the pin
entirely** (its POM lists only kotlin-stdlib, ejml, androidx.core), so the fix was bumping to
0.3.5. Lesson: when an SDK bump fails on a version constraint, check whether the *library* pins it
before touching anything in the SDK.

## 2026-08-29 — FIRST's stock compileSdk doesn't build

FIRST v11.2.1 ships `compileSdkVersion 30` in `build.common.gradle` and
`FtcRobotController/build.gradle`. With AGP 8.13.2 and our dependency set that fails outright
("Recommended action: Update this project to use a newer compileSdk"). We keep `compileSdk 34` as
a deliberate deviation — it's in `ALLOWED_DRIFT` in `scripts/check-structure.sh`, so the structure
check reports it as expected rather than as an error. If a future merge reverts it to 30, put 34
back.

## 2026-08-29 — Android Studio silently edited settings.gradle

Found `settings.gradle` modified with a `foojay-resolver-convention` plugin block nobody added on
purpose — Android Studio wrote it during a Gradle sync. Exactly the scenario
`check-structure.sh` exists for, and it caught it. Discarded; FIRST's v11.2.1 `settings.gradle`
supplies its own `pluginManagement` block that covers the same ground.

## 2026-08-29 — Upstream's `pedroPathing/Constants.java` can't actually drive

The stub upstream ships builds a follower with no drivetrain and no localizer:

```java
return new FollowerBuilder(followerConstants, hardwareMap)
        .pathConstraints(pathConstraints)
        .build();          // no .mecanumDrivetrain(), no .pinpointLocalizer()
```

Our real config lives in [`utils/Constants.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Constants.java)
instead — which also keeps it in our layer, so upstream merges never touch it. Use
`utils.Constants.createFollower()`. The upstream file stays untouched; the `Tuning` OpMode uses it.

## 2026-08-29 — GitBook says we wrote `Robot.java` / `CommandOpMode`; we didn't

The "Under the Hood" page presents both as living in "our utils folder." Diffed against SolversLib
0.3.4 sources: byte-for-byte `com.seattlesolvers.solverslib.command.Robot` and `.CommandOpMode`.
We extend the library classes directly — no shadow copies of code we don't own. Worth correcting
on the GitBook.

## 2026-08-29 — Build verified clean

`./gradlew :TeamCode:compileDebugJavaWithJavac --rerun-tasks` → `BUILD SUCCESSFUL`, warnings only
(Java 8 deprecation on JDK 25, non-incremental `OpModeAnnotationProcessor`). See
[gradle-and-android-studio.md](gradle-and-android-studio.md). Pinned: Gradle 8.9, AGP 8.7.0,
compileSdk 34.

## Template

```
## YYYY-MM-DD — title

What broke, what you were doing, the actual root cause, and the fix. "Reinstalled Android Studio"
is not a root cause.
```
