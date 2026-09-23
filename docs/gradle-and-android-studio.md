# Gradle & Android Studio

**When Android Studio offers to upgrade Gradle or the Android Gradle Plugin on first sync: decline.**

FTC pins those versions deliberately. `sourceCompatibility` sits at Java 1.8 because OnBotJava (the
in-browser editor other teams use) only supports Java 8. Accepting the upgrade desyncs your machine
from the repo and produces a build that works for you and nobody else. Version bumps arrive through
[an upstream merge](updating-from-upstream.md), never through the IDE prompt.

Clicked yes anyway? The commit guard (`scripts/hooks/pre-commit`) will refuse the commit and tell
you what to roll back, so it stays on your laptop and doesn't reach the team's.

## The build broke right after a pull

```
Failed to apply plugin 'com.android.internal.library'
... a Gradle internal API that was removed in Gradle 9.6.0
```

or anything else saying Gradle and the Android Gradle Plugin don't fit together. That isn't your
code. Someone's upgrade got pushed, and you pulled it. It happened here in September 2026
([issue-log.md](issue-log.md)).

```bash
./scripts/check-structure.sh                         # names the file + the checkout that fixes it
git log --oneline -5 -- gradle build.gradle          # which commit changed it
```

Run the `git checkout` it prints, then build, commit, and push so everyone else gets the fix.

## Warnings that are fine to ignore

```
Java compiler version 25 has deprecated support for compiling with source/target version 8
warning: [options] source value 8 is obsolete and will be removed in a future release
The following annotation processors are not incremental: OpModeAnnotationProcessor.jar
```

All expected. If the build ends in `BUILD SUCCESSFUL`, nothing above it mattered.

Check for yourself: `./gradlew :TeamCode:compileDebugJavaWithJavac`

## What's actually pinned

These change with every upstream merge, so check rather than trusting a number in a doc:

```bash
grep distributionUrl gradle/wrapper/gradle-wrapper.properties   # Gradle
grep "com.android.tools.build:gradle" build.gradle              # AGP
grep -E "compileSdk|minSdkVersion|targetSdkVersion" build.common.gradle
```

As of 2026-09-16: **Gradle 9.1.0, AGP 8.13.2, SDK v12.0.0** (BIOBUZZ), compileSdk 34, minSdk 24,
targetSdk 28, Java 1.8.

> ⚠️ **AGP 8.13.2 needs a recent Android Studio.** Older installs will refuse to sync this project
> with a message about the Android Gradle Plugin version. If Gradle sync fails on a machine that
> worked last season, updating Android Studio is the first thing to try — this is the one case
> where the answer really is "update the IDE."

Cross-reference AGP against Android Studio's
[compatibility table](https://developer.android.com/build/releases/gradle-plugin#agp-plugin-versions)
before assuming a given Studio version can open this project.

## Sync actually failed

1. First sync needs internet — `aapt2` and friends download on first build, no offline fallback.
2. Confirm your Android Studio version matches what FIRST recommends for this SDK (release notes
   are in `README.md`, upstream's file).
3. Run the greps above instead of trusting what worked last year.
4. Just pulled? See "The build broke right after a pull" above.
5. Found the fix? Add it to [issue-log.md](issue-log.md).
