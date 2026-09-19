# Iapetus

**[Moonshots #21681](https://ga-moonshots.netlify.app/)**' robot for the 2026–27 FTC season,
**BIOBUZZ**. Built on [Artemis](https://github.com/GA-Moonshots/Artemis), the team's
game-agnostic base. Fold anything that would help *any* game back into Artemis.

Iapetus, Saturn's two-faced moon: one side bright, one side dark. Much like teleop and auto.

## What's here

Everything Artemis provides (Pedro Pathing drive, the command pattern, AprilTag tracking, one
telemetry funnel, one coordinate frame) plus this season's layer:

- **`Iapetus.java`**: the robot, with `Intake` and `Launcher` wired in and waiting to be built
- **Hive tracking**: `robot.ourScoringHive()` and the four Hive names in `Constants`
- **[docs/biobuzz.md](docs/biobuzz.md)**: tag ids, tag size, camera settings for this game

## Start here

- What's specific to this game → [docs/biobuzz.md](docs/biobuzz.md)
- Understanding the code → [docs/architecture.md](docs/architecture.md)
- New laptop → [docs/setup.md](docs/setup.md)
- Tuning the drivetrain → [docs/tuning.md](docs/tuning.md)
- Camera and aiming → [docs/vision.md](docs/vision.md)

## Credits

Built by **Moonshots, FTC #21681** — [ga-moonshots.netlify.app](https://ga-moonshots.netlify.app/).
Standing on [SolversLib](https://docs.seattlesolvers.com), [Pedro Pathing](https://pedropathing.com),
Panels, and FIRST's [FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController).

## Why not README.md?

`README.md` belongs to FIRST: every `git merge upstream/master` rewrites it. Team content lives in
files upstream doesn't know exist, so those merges stay silent.
