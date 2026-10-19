#!/usr/bin/env bash
set -euo pipefail

# Single upstream pin. Generated assets never edited in Kanarek.
readonly source_commit="c1eca0ebb18f1ccb273ef2f15c3ff1ae14ca7fa5"
readonly destination="app/src/main/assets/benches"
readonly scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT

git init -q "$scratch/source"
git -C "$scratch/source" remote add origin https://github.com/travnie/twojstar.git
git -C "$scratch/source" fetch -q --depth 1 origin "$source_commit"
test "$(git -C "$scratch/source" rev-parse FETCH_HEAD)" = "$source_commit"
git -C "$scratch/source" checkout -q --detach FETCH_HEAD
(
  cd "$scratch/source/benches"
  npm ci --no-audit --no-fund
  for bench in codebench docbench streambench; do
    npm run "build:$bench"
  done
)
for bench in codebench docbench streambench; do
  asset="$scratch/source/benches/$bench/public/portable.html"
  test -s "$asset"
  mkdir -p "$destination/$bench"
  cp "$asset" "$destination/$bench/portable.html"
  cp "$scratch/source/benches/LICENSE" "$destination/$bench/LICENSE"
  cp "$scratch/source/benches/$bench/THIRD_PARTY_NOTICES.md" "$destination/$bench/THIRD_PARTY_NOTICES.md"
  printf '%s\n' "$source_commit" > "$destination/$bench/SOURCE_COMMIT"
  sha256sum "$destination/$bench/portable.html"
done
