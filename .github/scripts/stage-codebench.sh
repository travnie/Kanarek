#!/usr/bin/env bash
set -euo pipefail

# Immutable Bench source; updates need a deliberate review.
readonly bench_commit="c1eca0ebb18f1ccb273ef2f15c3ff1ae14ca7fa5"
readonly target="app/src/main/assets/benches/codebench"
readonly tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

git init -q "$tmp/source"
git -C "$tmp/source" remote add origin https://github.com/travnie/twojstar.git
git -C "$tmp/source" fetch --quiet --depth 1 origin "$bench_commit"
if [[ "$(git -C "$tmp/source" rev-parse FETCH_HEAD)" != "$bench_commit" ]]; then
  echo "Unexpected twojstar revision" >&2
  exit 1
fi
git -C "$tmp/source" checkout --quiet --detach FETCH_HEAD

(
  cd "$tmp/source/benches"
  npm ci --no-audit --no-fund
  npm run build:codebench
)
readonly generated="$tmp/source/benches/codebench/public/portable.html"
test -s "$generated" || { echo "Missing Codebench portable output" >&2; exit 1; }
mkdir -p "$target"
cp "$generated" "$target/portable.html"
cp "$tmp/source/benches/LICENSE" "$target/LICENSE"
cp "$tmp/source/benches/codebench/THIRD_PARTY_NOTICES.md" "$target/THIRD_PARTY_NOTICES.md"
printf '%s\n' "$bench_commit" > "$target/SOURCE_COMMIT"
sha256sum "$target/portable.html"
