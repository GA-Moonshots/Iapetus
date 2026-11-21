# BIOBUZZ (2026–27): what's specific to this game

Everything here is true for this season only. The general machinery lives in Artemis and is
explained in [vision.md](vision.md); this page is how it applies to BIOBUZZ.

## The tags are on the Hives

BIOBUZZ has **no fixed AprilTags**. Every tag rides on a Hive, and Hives move, so the camera can
tell us where a Hive is, but never where *we* are. Odometry is the only answer to "where am I?"
this season.

`Sensors` enforces that: with no fixed tags in the SDK, `Tunables.TAG_LOCALIZATION` and
`VISION_CORRECTIONS_ENABLED` do nothing, and flipping either on the dashboard just shows
*"Localization: off — no fixed tags this season"*.

Each Hive carries a strip of four 3.25" tags. The SDK groups each strip into a cluster, and
`Sensors` merges each cluster into one target:

| Hive | Tag ids | Constant |
|---|---|---|
| RED SCORING | 30–33 | `Constants.RED_SCORING_HIVE` |
| RED AUDIENCE | 34–37 | `Constants.RED_AUDIENCE_HIVE` |
| BLUE AUDIENCE | 38–41 | `Constants.BLUE_AUDIENCE_HIVE` |
| BLUE SCORING | 42–45 | `Constants.BLUE_SCORING_HIVE` |

Within a strip the tags sit 6.5" and 2.75" either side of the Hive's centre. See all four and the
target is the centre of the strip; see one end tag alone and it can be up to 6.5" off toward that
end. `tagCount` tells you which.

**The strip's centre is not the Hive's own origin.** The SDK (`AprilTagGameDatabase`, 12.0) puts
every Hive tag at 7.19" and −5.62" from the cluster's origin on its other two axes, so even four
tags in view land about 9" from that point. Which physical point on the Hive the origin is, the SDK
doesn't say. If the launcher aims at something other than the tag face, measure that offset on a
real Hive and add it in `FieldMap`.

## Using it

```java
TagSighting hive = robot.ourScoringHive();   // our Hive on this half of the field, or null
if (hive != null) {
    hive.range();                 // inches away
    hive.bearingFrom(8, 0);       // radians for a mechanism pivoting 8" forward of centre
}
```

Despite the name, `ourScoringHive()` means *our Hive on the half we're in*: the audience Hive below
y = 72, the scoring Hive above (`Sensors.getOurTarget()`). `LaunchDynamic` ranges off it.

Driver **left bumper** turns the robot to face the nearest Hive, **X** to face ours
(`DriveFaceTarget`).

## Camera settings for this game

On top of the steps in [vision.md](vision.md#camera-setup):

- **Tag size: 82.55 mm** (3.25"). Get this wrong and every Hive distance is scaled by the same
  ratio.
- Full 3D on, robot-space position from `Constants.CAMERA_*`.

Check with **Camera Calibration**, editing its settings live on Panels. A Hive strip puts four tags
in view at once, so either:

- set `TAG_ID` to the one tag you measured to (otherwise the check hops between the four), or
- turn on `WHOLE_TARGET` and measure to the middle of the strip. That's the merged point
  `LaunchDynamic` ranges off, so it's the one worth getting right.

Set `TAG_UP` to the tag's height to check the camera's pitch too. The field check skips itself
this season: there are no fixed tags to check against.

## Subsystems

**Intake**: one motor. **Launcher**: two flywheels and four feed servos, speeds in m/s of wheel
surface speed. Neither reads a gamepad; the operator's controls are their default commands.

| Player 2 | Does | Where |
|---|---|---|
| Left stick Y | intake in (up) / out (down) | `IntakeManual` |
| Right trigger | flywheel speed | `LaunchManual` |
| X | feed on/off | `LaunchManual` |
| A | auto-shot: spin up, feed, stop | `LaunchDynamic` |
| Y | stop the launcher, auto-shot included | `Iapetus` |

Find the auto-shot speed with the **Launcher Tuner** OpMode ([tuning.md](tuning.md)). Anything that
aims at a Hive reads a `TagSighting` in its command's `execute()` every loop.

**Auto:** every routine is a shoot leg then a park leg (`Iapetus.initAuto()`). During INIT on
gamepad 1, Y = shoot + park, A = park only.

**Still for the team to confirm:** the back flywheel's radius (`Constants.BACK_WHEEL_RADIUS`, 0.07
until 2026-11-14, now 0.05).
