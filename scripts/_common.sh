#!/usr/bin/env bash
#
# _common.sh — shared bits for the other scripts. Not meant to be run directly.

RED=$'\033[0;31m'; YELLOW=$'\033[0;33m'; GREEN=$'\033[0;32m'
BLUE=$'\033[0;34m'; DIM=$'\033[2m'; BOLD=$'\033[1m'; OFF=$'\033[0m'

REPO="$(git rev-parse --show-toplevel 2>/dev/null)" || {
    echo "${RED}Not inside a git repository.${OFF}"
    exit 1
}

# The Control Hub hands out this address on its own WiFi network.
# Override with:  ROBOT_HOST=1.2.3.4 ./scripts/deploy.sh
ROBOT_HOST="${ROBOT_HOST:-192.168.43.1}"
ROBOT_PORT="${ROBOT_PORT:-5555}"

# Android Studio keeps adb inside the SDK and usually doesn't put it on PATH,
# so we go looking. sdk.dir in local.properties is per-machine and gitignored,
# which makes it the most reliable hint we have.
find_adb() {
    if command -v adb >/dev/null 2>&1; then
        command -v adb
        return 0
    fi

    local sdk=""
    if [ -f "$REPO/local.properties" ]; then
        sdk=$(grep -E '^sdk\.dir=' "$REPO/local.properties" 2>/dev/null \
              | head -1 | cut -d= -f2- | tr -d '\r' | sed 's/\\:/:/g')
    fi

    local candidate
    for candidate in "$sdk" "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" \
                     "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" \
                     "${LOCALAPPDATA:-}/Android/Sdk"; do
        if [ -n "$candidate" ] && [ -x "$candidate/platform-tools/adb" ]; then
            echo "$candidate/platform-tools/adb"
            return 0
        fi
    done

    return 1
}

# Prints guidance and exits if adb can't be found anywhere.
require_adb() {
    local adb
    if ! adb=$(find_adb); then
        echo "${RED}✗ Can't find adb.${OFF}"
        echo
        echo "adb ships with the Android SDK. Options:"
        echo "  • Install Android Studio once (it brings the SDK), or"
        echo "  • brew install --cask android-platform-tools    ${DIM}(macOS, no IDE)${OFF}"
        echo
        echo "Already have the SDK? Point us at it:"
        echo "  ${DIM}echo 'sdk.dir=/path/to/Android/sdk' >> local.properties${OFF}"
        exit 1
    fi
    echo "$adb"
}

# ─── What upstream owns ──────────────────────────────────────────
# One list, read by check-structure.sh (reports drift) and hooks/pre-commit
# (refuses to commit it). See AGENTS.md for the reasoning.
#
# Upstream is FIRST-Tech-Challenge/FtcRobotController — the SDK itself.
# TeamCode/build.gradle is deliberately NOT on this list: FIRST ships it
# nearly empty and expects teams to add dependencies there, so it's ours.
PROTECTED=(
    "README.md"
    "build.gradle"
    "build.common.gradle"
    "build.dependencies.gradle"
    "gradle.properties"
    "settings.gradle"
    "gradlew"
    "gradlew.bat"
    "gradle"
    "FtcRobotController"
    ".github"
)

# Deliberate, permanent exceptions. FIRST ships compileSdkVersion 30, which is
# too low for our dependency set — the build fails outright without this bump.
# So these two files carry a known edit. They're reported as a note, not an
# error, because a warning that never goes away is a warning everyone ignores.
# If you add to this list, write down WHY, right here.
ALLOWED_DRIFT=(
    "build.common.gradle"                 # compileSdk 34 (FIRST ships 30 — won't build)
    "FtcRobotController/build.gradle"     # compileSdk 34, same reason
)

is_allowed() {
    local f="$1" a
    for a in "${ALLOWED_DRIFT[@]}"; do
        [ "$f" = "$a" ] && return 0
    done
    return 1
}

# Gradle, the wrapper, and AGP are chosen by FIRST to fit each other. When one
# of these drifts, it was almost always Android Studio's upgrade prompt.
is_build_tooling() {
    case "$1" in
        gradle/*|build.gradle|gradle.properties|settings.gradle|gradlew|gradlew.bat) return 0 ;;
    esac
    return 1
}
