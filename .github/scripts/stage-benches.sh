#!/usr/bin/env bash
set -euo pipefail

# Single upstream pin. Generated assets never edited in Kanarek.
readonly source_commit="c1eca0ebb18f1ccb273ef2f15c3ff1ae14ca7fa5"
readonly destination="app/src/main/assets/benches"
# Reproducible portable outputs independently verified on two CI runs.
# Updating the source pin requires reviewing all three generated hashes.
declare -Ar expected_sha256=(
  [codebench]="5296b368848c0d5887e50fe896dc0e235490fa5ec4315390e9c5318859a37879"
  [docbench]="4db8c87770dd34f32134174dab153167775da3d4ae00dc74dd919bc83ed96071"
  [streambench]="331ba8649f7a1c6d6d9ae60af2536102f416c68c408b44b813c6063f055938c9"
)
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
  actual_sha256="$(sha256sum "$asset" | cut -d ' ' -f1)"
  if [[ "$actual_sha256" != "${expected_sha256[$bench]}" ]]; then
    echo "Unexpected $bench portable.html digest: $actual_sha256" >&2
    exit 1
  fi
  mkdir -p "$destination/$bench"
  cp "$asset" "$destination/$bench/portable.html"
  cp "$scratch/source/benches/LICENSE" "$destination/$bench/LICENSE"
  cp "$scratch/source/benches/$bench/THIRD_PARTY_NOTICES.md" "$destination/$bench/THIRD_PARTY_NOTICES.md"
  printf '%s\n' "$source_commit" > "$destination/$bench/SOURCE_COMMIT"
  sha256sum "$destination/$bench/portable.html"
done
(
  cd "$destination"
  sha256sum codebench/portable.html docbench/portable.html streambench/portable.html > SHA256SUMS
)
