#!/usr/bin/env bash
#
# doctor.sh — is this machine set up to work on Artemis?
#
# Run this first on a new laptop. It checks things and fixes nothing, so it's
# always safe. Every ✗ comes with the command that resolves it.

set -uo pipefail
source "$(dirname "$0")/_common.sh"
cd "$REPO"

problems=0
note() { echo "${DIM}    $1${OFF}"; }
fail() { echo "${RED}✗ $1${OFF}"; problems=$((problems + 1)); }
ok()   { echo "${GREEN}✓ $1${OFF}"; }
warn() { echo "${YELLOW}! $1${OFF}"; }

echo "${BOLD}Artemis setup check${OFF}"
echo

# ─── Java ────────────────────────────────────────────────────────
if command -v java >/dev/null 2>&1; then
    ok "Java: $(java -version 2>&1 | head -1 | tr -d '"')"
else
    fail "No Java found."
    note "Android Studio bundles one. Otherwise: brew install --cask temurin"
fi

# ─── Gradle wrapper ──────────────────────────────────────────────
if [ -x "./gradlew" ]; then
    ok "Gradle wrapper present"
else
    fail "gradlew missing or not executable."
    note "chmod +x gradlew"
fi

# ─── Android SDK / adb ───────────────────────────────────────────
if adb_path=$(find_adb); then
    ok "adb: $adb_path"
else
    fail "adb not found — you can write code, but not deploy to the robot."
    note "Install Android Studio once, or: brew install --cask android-platform-tools"
    note "Then: echo 'sdk.dir=/path/to/Android/sdk' >> local.properties"
fi

# ─── Git remotes ─────────────────────────────────────────────────
if git remote get-url origin >/dev/null 2>&1; then
    ok "origin: $(git remote get-url origin)"
else
    warn "No 'origin' remote — you won't be able to push."
fi

if git remote get-url upstream >/dev/null 2>&1; then
    if ! git remote get-url upstream | grep -q "FtcRobotController"; then
        warn "upstream points at $(git remote get-url upstream), not FIRST's SDK."
        note "git remote set-url upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController.git"
    elif git rev-parse --verify --quiet upstream/master >/dev/null; then
        ok "upstream configured and fetched"
    else
        warn "upstream configured but never fetched."
        note "git fetch upstream"
    fi
else
    warn "No 'upstream' remote — season SDK updates and the structure check need it."
    note "git remote add upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController.git"
fi

# ─── Machine-specific config ─────────────────────────────────────
if [ -f "local.properties" ]; then
    ok "local.properties exists (points at your SDK; correctly gitignored)"
else
    warn "No local.properties — Gradle may not find your Android SDK."
    note "Opening the project in Android Studio once generates it, or write it yourself:"
    note "echo 'sdk.dir=$HOME/Library/Android/sdk' > local.properties"
fi

# ─── Structure ───────────────────────────────────────────────────
echo
./scripts/check-structure.sh >/dev/null 2>&1
case $? in
    0) ok "Repo structure clean (upstream's files untouched)" ;;
    2) warn "Structure only partly checked — needs the upstream remote above."
       note "Details: ./scripts/check-structure.sh" ;;
    *) fail "Structure drift detected."
       note "Details: ./scripts/check-structure.sh" ;;
esac

# ─── Verdict ─────────────────────────────────────────────────────
echo
if [ "$problems" -eq 0 ]; then
    echo "${GREEN}${BOLD}Ready to go.${OFF}"
    echo
    echo "  ./scripts/build.sh     does it compile?"
    echo "  ./scripts/deploy.sh    put it on the robot"
    echo "  ./scripts/logs.sh      what is the robot saying?"
    echo
    echo "${DIM}New to the codebase? docs/architecture.md — one page, whole repo.${OFF}"
    exit 0
fi

echo "${YELLOW}$problems thing(s) to fix above.${OFF}"
echo "${DIM}Warnings (!) are usually fine to ignore for a while. ✗ will stop you.${OFF}"
exit 1
