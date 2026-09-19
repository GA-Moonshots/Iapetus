# BIOBUZZ (2026–27): what's specific to this game

Everything here is true for this season only. The general machinery lives in Artemis and is
explained in [vision.md](vision.md); this page is how it applies to BIOBUZZ.

## The tags are on the Hives

BIOBUZZ has **no fixed AprilTags**. Every tag rides on a Hive, so the camera can tell us where a
Hive is, but never where *we* are. Leave `Tunables.TAG_LOCALIZATION` and
`VISION_CORRECTIONS_ENABLED` off all season.

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
TagSighting hive = robot.ourScoringHive();   // our alliance's scoring Hive, or null
if (hive != null) {
    hive.range();                 // inches away
    hive.bearingFrom(8, 0);       // radians for a mechanism pivoting 8" forward of centre
}
```

Driver **left bumper** turns the robot to face the nearest Hive (`DriveFaceTarget`).

## Camera settings for this game

On top of the steps in [vision.md](vision.md#camera-setup):

- **Tag size: 82.55 mm** (3.25"). Get this wrong and every Hive distance is scaled by the same
  ratio.
- Full 3D on, robot-space position from `Constants.CAMERA_*`.

Check with **Camera Calibration**: tape a Hive tag upright at `TAG_FORWARD` / `TAG_LEFT` and read
the error.

## Subsystems

`Intake` and `Launcher` are wired into `Iapetus` and registered, but empty. Build them the way
[Ganymede's subsystems](https://github.com/GA-Moonshots/Ganymede/tree/master/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems) are built: hardware names in `Constants`, lookups in the constructor, small methods
that each do one thing. Anything that aims at a Hive reads a `TagSighting` in its command's
`execute()` every loop.
