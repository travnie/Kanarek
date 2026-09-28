#!/usr/bin/env bash
# SessionStart, cloud sessions only. Never fails the session.
#  - ANDROID_HOME fallback to the SDK from the environment setup script
#  - npm ci for every committed package-lock.json (skipped when node_modules
#    is already newer than the lockfile, e.g. on resume)
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

if [ -z "${ANDROID_HOME:-}" ] && [ -d /opt/android-sdk ] && [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo 'export ANDROID_HOME=/opt/android-sdk' >> "$CLAUDE_ENV_FILE"
fi

# Android skills from the Android CLI, user-level so the repo stays clean.
# Installed in parallel (~11 s on a fresh VM, 0 s once present) and waited
# for, so the first turn already has them.
android_cli=$(command -v android || echo "${ANDROID_HOME:-/opt/android-sdk}/cmdline-tools/latest/bin/android")
if [ -x "$android_cli" ]; then
  for skill in android-cli testing-setup edge-to-edge r8-analyzer android-intent-security android-permissions-security media3-cast-integration; do
    [ -d "$HOME/.claude/skills/$skill" ] && continue
    "$android_cli" skills add --agent=claude-code "$skill" >/dev/null 2>&1 \
      || echo "android skill failed: $skill" >&2 &
  done
  wait
fi

git ls-files '*package-lock.json' | while read -r lock; do
  dir=$(dirname "$lock")
  [ "$dir/node_modules/.package-lock.json" -nt "$lock" ] && continue
  if (cd "$dir" && npm ci --no-audit --no-fund --no-update-notifier --loglevel=error >/dev/null); then
    echo "npm ci: $dir"
  else
    echo "npm ci failed: $dir" >&2
  fi
done

exit 0
