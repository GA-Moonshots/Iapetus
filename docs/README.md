# Docs

**New laptop?** [setup.md](setup.md) — install Android Studio, then `./scripts/doctor.sh`.

**New here?** [architecture.md](architecture.md). One page, explains the whole repo.

**New robot?** [tuning.md](tuning.md), start to finish, before your first real drive.

**Writing a path, or a coordinate looks wrong?** [coordinates.md](coordinates.md).

**Aiming at a game piece, or setting up the camera?** [vision.md](vision.md), then
[biobuzz.md](biobuzz.md) for this game's Hives.

**Robot won't localize / coordinates frozen?** [diagnostics.md](diagnostics.md) — run Pinpoint Doctor first.

**Android Studio yelling at you?** [gradle-and-android-studio.md](gradle-and-android-studio.md).
Short version: decline the upgrade it's offering.

---

| | |
|---|---|
| [setup.md](setup.md) | Get your machine working — Android Studio, git remotes, the scripts |
| [architecture.md](architecture.md) | Subsystems, commands, the scheduler — how it fits together |
| [coordinates.md](coordinates.md) | The frame of reference — read before writing any path |
| [vision.md](vision.md) | Tracking tags on game pieces, aiming an arm, camera setup |
| [biobuzz.md](biobuzz.md) | This season only: Hive tags, tag size, the robot's subsystems |
| [diagnostics.md](diagnostics.md) | Robot won't localize? Work down this list |
| [tuning.md](tuning.md) | The 8-phase drivetrain tuning sequence |
| [gradle-and-android-studio.md](gradle-and-android-studio.md) | Build errors, and what's safe to ignore |
| [updating-from-upstream.md](updating-from-upstream.md) | Pulling the new season's SDK without losing our layer |
| [issue-log.md](issue-log.md) | Dated gotchas, so nobody rediscovers them next August |
| [roadmap.md](roadmap.md) | What's built, what's next, what still needs the robot |

## Scripts

```
./scripts/doctor.sh            is this machine set up?
./scripts/build.sh             does it compile?              (no robot needed)
./scripts/deploy.sh            put it on the robot           (--usb for cable)
./scripts/logs.sh              what is the robot saying?     (--crash for errors only)
./scripts/check-structure.sh   did we edit something upstream owns?
```

All of them report and explain; none of them edit your code.

Deeper theory — PID intuition, Pedro internals — lives in the team
[GitBook](https://gilmour.online/compsci/competitive-robotics/software-team). These docs stay short
enough to read with one hand on the robot.
