# Setting up your machine

**Android Studio is what we use.** Everyone on the team runs the same IDE on their own laptop —
that's deliberate. When all of us are in the same tool, a problem one person solves is a problem
everyone can solve, and a teammate can sit down next to you and actually help. It's also the
toolchain FIRST supports, so the error messages you hit are the ones the docs and forums describe.

```bash
git clone https://github.com/GA-Moonshots/Iapetus.git
cd Iapetus
git remote add upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController.git
git config core.hooksPath scripts/hooks     # commit guard (Android Studio's first sync does it too)
./scripts/doctor.sh
```

`doctor.sh` checks your setup and fixes nothing, so it's always safe to run. Each ✗ comes with the
command that resolves it.

**Not on Moonshots?** You want [Artemis](https://github.com/GA-Moonshots/Artemis), the
game-agnostic base, not this season repo. Its MOONSHOTS.md explains how to adopt it.

## Android Studio

1. Install the version FIRST currently recommends for this season's SDK (the release notes in
   `README.md` say which).
2. Open the **repo root** — not `TeamCode/`, not `FtcRobotController/`.
3. Let Gradle sync fully the first time. It needs internet; dependencies download on first build.
4. **Decline any prompt offering to upgrade Gradle or the Android Gradle Plugin.** Those versions
   are pinned deliberately — [gradle-and-android-studio.md](gradle-and-android-studio.md) explains
   why. Clicked it by accident? The commit guard stops you committing it, and
   `./scripts/check-structure.sh` prints the command that puts it back.

Deploy with the **Run** button, same as always. Pick your OpMode on the Driver Station:
*Drivey McDriverson* (teleop), *Auto McAutty* (autonomous), *Camera Calibration* (see
[coordinates.md](coordinates.md)), *Pinpoint Doctor* (see [diagnostics.md](diagnostics.md)).
Drivetrain tuning isn't on the Driver Station any more: it runs in a browser
([tuning.md](tuning.md)).

## The scripts

`scripts/` supplements the IDE — it doesn't replace it. Some things are just faster or clearer from
a terminal, and they're the same Gradle and adb commands Android Studio runs behind its buttons.

```
./scripts/doctor.sh            is this machine set up right?
./scripts/build.sh             does it compile?           (no robot needed)
./scripts/deploy.sh            build + install to robot   (--usb for cable)
./scripts/logs.sh              what is the robot saying?  (--crash for errors only)
./scripts/check-structure.sh   did we edit something upstream owns?
```

`logs.sh` is worth knowing: when an OpMode crashes, the Driver Station shows a short message while
the actual stack trace goes to the robot's log. `./scripts/logs.sh --crash` shows it. Look for the
`Caused by:` line.

Run `check-structure.sh` before you commit. It catches the one mistake that costs real time later.
The commit guard in `scripts/hooks/` runs the essential part of it for you on every commit, and
refuses any edit to FIRST's files. It says what to roll back when it fires.

## Working with others

Everyone has their own laptop, so more than one person is pushing. **Pull before you start, push
when you stop.** A conflict you hit at a meeting is five minutes; one you find at a competition is
not.

## AI agents

`CLAUDE.md` and `AGENTS.md` sit in the repo root, so Claude Code and most other agents pick up the
house rules automatically when the project opens. Nothing to configure.

Claude Code has a JetBrains plugin that runs in Android Studio, with one catch: Android Studio's
bundled runtime lacks JCEF, so the chat panel won't render until you switch runtimes —
**Help → Find Action → "Choose Boot Runtime"**, pick one whose name contains **JCEF**, restart. The
plugin also needs the Claude Code CLI installed separately; it doesn't bundle it.

Get it working? Good — genuinely, that's a useful thing to have figured out, and worth writing up
in [issue-log.md](issue-log.md) so the next person doesn't have to. Can't get it working today?
No loss: open a terminal inside Android Studio (**View → Tool Windows → Terminal**) and run
`claude` there, or use the scripts above.

Either way the rules travel with the repo, so the agent starts out pointed in the right direction.
Which is the point — an agent that explains *why* `execute()` can't block teaches you something; one
that just writes the code for you doesn't. Ask it to explain what it changed, and push back when
the answer is vague. It is sometimes confidently wrong, and catching that is most of the skill.
