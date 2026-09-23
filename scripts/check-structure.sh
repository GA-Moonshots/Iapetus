#!/usr/bin/env bash
#
# check-structure.sh — did we accidentally edit something upstream owns?
#
# Artemis works because files upstream ships stay byte-for-byte upstream's.
# Edit one and next season's `git merge upstream/master` turns into a conflict
# hunt. This script tells you TODAY instead of next August.
#
# Run it whenever you like:  ./scripts/check-structure.sh
#
# It reports. It does not block anything, and it never changes a file.
# Exit 0 = clean, 1 = problems found, 2 = the main check couldn't run.

set -uo pipefail
source "$(dirname "$0")/_common.sh"   # PROTECTED, ALLOWED_DRIFT, colors
cd "$REPO"

problems=0
skipped=""   # a check we couldn't run must not end in "Clean"

# Gradle drifting on its own is how a whole team's build breaks in one pull
# (docs/issue-log.md, 2026-09-22), so it gets its own sentence.
tooling_hint() {
    local f
    while IFS= read -r f; do
        if is_build_tooling "$f"; then
            echo "${YELLOW}    Gradle/AGP files changed — almost always Android Studio's upgrade prompt.${OFF}"
            echo "${YELLOW}    Put them back: a newer Gradle can't load the AGP this SDK pins.${OFF}"
            echo "${DIM}    Why: docs/gradle-and-android-studio.md${OFF}"
            return
        fi
    done
}

echo "Checking Artemis structure..."
echo

# ─────────────────────────────────────────────────────────────────
# 1. Uncommitted edits to protected files
# ─────────────────────────────────────────────────────────────────
uncommitted=$(git status --porcelain -- "${PROTECTED[@]}" 2>/dev/null)
if [ -n "$uncommitted" ]; then
    echo "${RED}✗ Uncommitted changes to files upstream owns:${OFF}"
    echo "$uncommitted" | sed 's/^/    /'
    echo "${DIM}    Undo with: git checkout -- <file>${OFF}"
    echo "$uncommitted" | awk '{print $NF}' | tooling_hint
    echo
    problems=$((problems + 1))
fi

# ─────────────────────────────────────────────────────────────────
# 2. Committed drift, measured from where we last met upstream
#    (comparing to upstream/master directly would flag upstream's OWN
#     new commits as our problem — the merge base is the honest baseline)
# ─────────────────────────────────────────────────────────────────
if ! git remote get-url upstream >/dev/null 2>&1; then
    echo "${YELLOW}! No 'upstream' remote — skipping the committed-drift check.${OFF}"
    skipped="no upstream remote"
    echo "${DIM}    Fix: git remote add upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController.git${OFF}"
    echo
elif ! git rev-parse --verify --quiet upstream/master >/dev/null; then
    echo "${YELLOW}! Haven't fetched upstream yet — skipping the committed-drift check.${OFF}"
    skipped="upstream not fetched"
    echo "${DIM}    Fix: git fetch upstream${OFF}"
    echo
elif ! git remote get-url upstream | grep -q "FtcRobotController"; then
    # Guard against judging with the wrong yardstick. If `upstream` points at
    # some other repo (it used to be the SolversLib Quickstart), every file the
    # two repos legitimately differ on looks like our mistake — a dozen
    # confident false alarms, which is worse than saying nothing.
    echo "${YELLOW}! 'upstream' doesn't look like FIRST's SDK — skipping the committed-drift check.${OFF}"
    skipped="upstream is the wrong repo"
    echo "${DIM}    points at: $(git remote get-url upstream)${OFF}"
    echo "${DIM}    expected:  https://github.com/FIRST-Tech-Challenge/FtcRobotController.git${OFF}"
    echo "${DIM}    Fix: git remote set-url upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController.git${OFF}"
    echo
else
    base=$(git merge-base HEAD upstream/master 2>/dev/null)
    if [ -n "$base" ]; then
        drift=$(git diff --name-only "$base" HEAD -- "${PROTECTED[@]}" 2>/dev/null)
        unexpected=""; known=""
        while IFS= read -r f; do
            [ -z "$f" ] && continue
            if is_allowed "$f"; then known+="    $f"$'\n'; else unexpected+="$f"$'\n'; fi
        done <<< "$drift"

        if [ -n "$unexpected" ]; then
            short=$(git rev-parse --short "$base")
            echo "${RED}✗ Committed edits to files upstream owns:${OFF}"
            printf '    %s\n' $unexpected
            echo "${DIM}    Put upstream's version back, then commit:${OFF}"
            printf "${DIM}      git checkout $short -- %s${OFF}\n" $unexpected
            echo "${DIM}    Which commit did it: git log --oneline $short..HEAD -- <file>${OFF}"
            printf '%s' "$unexpected" | tooling_hint
            echo
            problems=$((problems + 1))
        fi
        if [ -n "$known" ]; then
            echo "${DIM}· Known deviations (deliberate, see ALLOWED_DRIFT in scripts/_common.sh):${OFF}"
            printf "${DIM}%s${OFF}" "$known"
            echo
        fi
    fi
fi

# ─────────────────────────────────────────────────────────────────
# 3. Machine-specific files that must never be committed
# ─────────────────────────────────────────────────────────────────
for leaky in "local.properties" ".idea/workspace.xml"; do
    if git ls-files --error-unmatch "$leaky" >/dev/null 2>&1; then
        echo "${RED}✗ $leaky is tracked — it points at YOUR machine and will break everyone else's build.${OFF}"
        echo "${DIM}    Fix: git rm --cached $leaky${OFF}"
        echo
        problems=$((problems + 1))
    fi
done

# ─────────────────────────────────────────────────────────────────
# Verdict
# ─────────────────────────────────────────────────────────────────
if [ "$problems" -eq 0 ] && [ -n "$skipped" ]; then
    # Saying "Clean" here would vouch for files we never compared. A fresh
    # clone has no upstream remote, so this is the case most people hit first.
    echo "${YELLOW}~ Nothing wrong in what was checked — but the main check didn't run ($skipped).${OFF}"
    echo "${DIM}  Committed edits to upstream's files are NOT ruled out. Apply the fix above and run again.${OFF}"
    exit 2   # not a failure, not a pass: doctor.sh reads this as "partial"
fi

if [ "$problems" -eq 0 ]; then
    echo "${GREEN}✓ Clean. Upstream's files are untouched — next season's merge will be boring.${OFF}"
    echo "${DIM}  (Boring is the goal.)${OFF}"
    exit 0
fi

echo "${YELLOW}Found $problems problem(s).${OFF}"
echo "Not sure what to do? Read AGENTS.md, or ask before undoing anything —"
echo "occasionally one of these is deliberate, and it should be a decision, not an accident."
exit 1
