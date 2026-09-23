# Updates: three kinds, three mechanisms

Iapetus pulls from three directions, and they work completely differently. Knowing which is which
saves you from merging something that was never meant to be merged.

## 1. The FTC SDK — a merge, once a season

`upstream` is **`FIRST-Tech-Challenge/FtcRobotController`**: the SDK itself, straight from FIRST.

```bash
git fetch upstream
git merge upstream/master
```

FIRST's `TeamCode` holds only a `readme.md`, so their releases barely touch our code. Expect
changes in `FtcRobotController/`, the root `build*.gradle` files, and `gradle/`. This is the only
way a new Gradle or AGP should arrive: FIRST ships them as a pair that works together. The commit
guard lets merges through, conflicts and all.

## 2. Libraries — a version bump, any time

SolversLib, Pedro Pathing, and Panels are **dependencies, not forks.** They live as version numbers
in `TeamCode/build.gradle`:

```gradle
implementation "org.solverslib:core:0.3.6"
implementation 'com.pedropathing:revhub:3.0.1'
implementation 'com.pedropathing:tuning:1.0.0'
implementation "com.bylazar:fullpanels:1.0.12"
```

Change the number, rebuild, done. No merge, no conflict, nothing to resolve. Latest versions:
[SolversLib releases](https://github.com/FTC-23511/SolversLib/releases) ·
[Pedro Pathing on Maven Central](https://central.sonatype.com/namespace/com.pedropathing) ·
[Panels](https://mymaven.bylazar.com/#/releases/com/bylazar/fullpanels).

**The exception is a major version.** A version bump is only "done" when the API didn't change.
Pedro 2 → 3 renamed nearly everything and replaced the tuners (see
[issue-log.md](issue-log.md), 2026-09-16), so it took a real port. Check the release notes before
bumping a first digit.

We use only SolversLib's `core` (scheduler, gamepad). Its `pedroPathing` module is compiled
against one Pedro major (0.3.5 ↔ Pedro 2, 0.3.6 ↔ Pedro 3), which is exactly why we don't depend on
it: our drive commands talk to Pedro themselves, so Pedro and SolversLib can move independently.

**Panels: check the Field tab after every bump.** `fullpanels` 1.0.13 bundles `field` 1.0.7, which
was published without its web files, and the robot overlay silently disappears. Build, deploy, open
the dashboard; no Field tab means roll back. (Quick check without a robot: the `field` `.aar` should
contain web assets; 1.0.6 has them, 1.0.7 has none.)

Bump one at a time and build in between. When something breaks you want to know which one did it.

## 3. Artemis — a merge, whenever the base improves

Iapetus is Artemis plus this season. When Artemis gets a fix that helps any game, pull it in:

```bash
git remote add artemis https://github.com/GA-Moonshots/Artemis.git   # once
git fetch artemis
git merge artemis/master
```

Conflicts land only where we changed something Artemis also changed — usually `Constants` or the
robot class. Going the other way, a general improvement made here gets copied into Artemis by hand,
without the BIOBUZZ parts.

## Known deviations from upstream

Two files carry a deliberate edit, and `check-structure.sh` reports them as expected rather than
as errors:

| File | Why |
|---|---|
| `build.common.gradle` | `compileSdk 34` — FIRST ships 30, which **fails to build** with our dependencies |
| `FtcRobotController/build.gradle` | same |

If a merge reverts those to 30, the build breaks immediately with "Recommended action: Update this
project to use a newer compileSdk." Put 34 back. Best done inside the merge commit, where the
commit guard allows it. In a commit of its own, it's the one deliberate case for
`git commit --no-verify`.

## After any update

1. `./scripts/build.sh` — catch API changes immediately.
2. `./scripts/check-structure.sh` — confirm nothing drifted beyond the two known deviations.
3. Surprises go in [issue-log.md](issue-log.md).

## Why not fork SolversLib?

It's a library — `core`, `pedroPathing`, `photon` modules published to Maven. It contains no
Android app, so nothing in it installs on a Control Hub. Forking it to get a robot project is like
forking React to get a website. Fork it only if you intend to modify the library itself; consume it
as a dependency otherwise.
