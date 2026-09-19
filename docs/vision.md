# Vision: tracking tags, and aiming at them

The Limelight can do two different jobs with AprilTags. Which one matters depends on the game.

| Job | Question | Needs | Default |
|---|---|---|---|
| **Tracking** | "Where is *that*?" | any tag, fixed or moving | always on |
| **Localization** | "Where am *I*?" | tags bolted to the field | off until proven |

Tracking is the flexible one: it works on field walls, game pieces, and anything else a game sticks
a tag on. Localization waits behind two switches in `Tunables` for a game whose tags stay put.

## How tracking works

1. The camera reports each tag's position *relative to the robot*.
2. `FieldMap` converts that to our robot frame (inches; forward, left, up), then uses odometry to
   put the tag on the field — from where the robot was when the frame was *taken*
   (`PedroDrive.poseAt()`), not where it is now. A frame can be up to 200 ms old, and a fast turn in
   that time would swing every tag sideways.
3. `Sensors` keeps the latest sighting of every tag, **in field coordinates**, for
   `Tunables.TAG_MEMORY_MS`.
4. When you ask, it re-measures from wherever the robot is *now*.

Step 3 is the useful trick. A field position stays true while the robot drives and turns, so a tag
that an arm blocks for half a second is still exactly where we left it. It stops being true when
someone moves the object, and that's what the memory limit is for.

Tags on the same object are merged into one **target**. The SDK says which tags belong together:
when a game puts several tags on one object, `AprilTagGameDatabase` ships them as a *cluster*, and
`FieldMap.targetName()` looks it up. A tag outside any cluster is a target on its own. Nothing in
this repo lists tag ids, so a new game's tags and clusters work without an edit.

**How exact is a target?** It's the average of the tags *in view*. Every tag of the object in view:
its centre. One tag at the edge: off by however far that tag sits from the middle. `tagCount` says
which case you're in.

## Using it

```java
TagSighting target = robot.sensors.nearestTarget();   // or target(name), or tag(id)
if (target != null) {
    target.range();      // inches from the robot's centre
    target.bearing();    // radians to turn; + is left
    target.fieldX;       // where it is on the field (Pedro frame)
}
```

Always check for `null`. No camera, nothing in view, and "saw it too long ago" all look the same,
on purpose. The robot has to cope with all three.

### Aiming a mechanism

An arm or turret doesn't pivot at the robot's centre. Give `TagSighting` the pivot's position, in
the same frame (inches from the robot's centre on the floor: forward, left, up), and it hands back
the pivot's angles:

```java
// Where the pivot is, e.g. 8" forward, centred, 10" up. Put the real numbers in Constants.
double swing = target.bearingFrom(8, 0);            // radians left(+)/right(−)
double tilt  = target.elevationFrom(8, 0, 10);      // radians up(+)/down(−)
double reach = target.distanceFrom(8, 0, 10);       // inches
```

Read these in the mechanism command's `execute()` every loop and set its target from them. Don't
wait for a "good" reading inside `execute()`: if the sighting is `null`, hold the last target and
try again next loop.

`DriveFaceTarget` is the worked example (driver **left bumper**): it locks onto the nearest target
and turns the robot to face it.

## Camera setup

Do this once per camera, and again after any reflash. The robot-space settings are the ones that
vanish.

1. **Pipeline `0` is an AprilTag pipeline** (`Constants.LIMELIGHT_APRILTAG_PIPELINE`), family 36h11.
2. **Full 3D is on** in that pipeline. Without it the camera finds tags but sends no 3D pose, and
   the Vision status reads *"tags seen, no 3D pose"*.
3. **Tag size matches the game.** The SDK knows it (`AprilTagMetadata.tagsize`; the game manual
   has it too). Distance is worked out from how big the tag looks, so a wrong size scales every
   distance by the same ratio. CameraCalibration says *"distance off by N%"* when this is the
   problem.
4. **Robot-space camera position** entered on the camera (metres), copied from
   `Constants.CAMERA_*`, which are named to match its fields.

Then run **Camera Calibration** with a tag at a measured spot. It reports the error and what the
error usually means.

## Localization, for games with fixed tags

`Tunables.TAG_LOCALIZATION` works out a field pose from fixed tags (MegaTag2) and reports it.
`VISION_CORRECTIONS_ENABLED` also lets that pose overwrite odometry, under the trust policy in
`Sensors.evaluate()`: fresh, close, on the field, and not an implausible jump. Every rejection is
on telemetry with its reason.

Leave both off until the tags are proven to stay put. A tag on a moved object reads just as
confidently as a fixed one, and quietly drags odometry toward where the object used to be.

## What belongs in a season repo instead

Artemis stays game-agnostic. The season's fork (named for that year's robot) adds the parts that
only make sense for one game:

- which targets matter, and names for them
- tag offsets within an object, if the one-tag error from merging is too big
- the arm, turret, or launcher that aims with `bearingFrom()` / `elevationFrom()`
- this game's tag size and camera settings, written down in the season's docs

If a season solves something that would help *any* game, fold it back into Artemis.
