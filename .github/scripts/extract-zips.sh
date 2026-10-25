#!/usr/bin/env bash
#
# Auto-extract ZIP archives that arrive in this repository.
#
# What it does
#   1. Unpacks the archive DIRECTLY into the place it lives (repo root for a
#      root level ZIP) — no extra "archive-name/" folder is created.
#   2. If the archive contains a single top level folder (e.g. "AVA/…"), that
#      folder is stripped, so the files land flat in the destination.
#   3. Files with the same name are replaced by the new version.
#   4. Files that a previous extraction brought in but the new archive no
#      longer contains are removed automatically (tracked in a manifest).
#   5. The uploaded ZIP itself is deleted after a successful extraction.
#
# Sources
#   EVENT_NAME=push     every *.zip tracked in the repository
#   EVENT_NAME=release  every *.zip attached to the release
#   SOURCE_URL=<url>    a single archive downloaded from a URL (dispatch mode)
#
# Environment variables
#   REPO_DIR          repository root                (default: current dir)
#   EVENT_NAME        push | release | dispatch      (default: push)
#   RELEASE_TAG       tag of the release              (release mode only)
#   SOURCE_URL        optional archive URL            (dispatch mode)
#   OUTPUT_LIST_FILE  file receiving the list of extracted folders
#   REPORT_FILE       file receiving a human readable report
#   KEEP_ARCHIVE      1 = keep the uploaded ZIP       (default: 0 = delete)
#   GH_TOKEN          token used by the gh CLI

set -euo pipefail

REPO_DIR="${REPO_DIR:-$(pwd)}"
EVENT_NAME="${EVENT_NAME:-push}"
RELEASE_TAG="${RELEASE_TAG:-}"
SOURCE_URL="${SOURCE_URL:-}"
OUTPUT_LIST_FILE="${OUTPUT_LIST_FILE:-}"
REPORT_FILE="${REPORT_FILE:-}"
KEEP_ARCHIVE="${KEEP_ARCHIVE:-0}"
GH_TOKEN="${GH_TOKEN:-}"

MANIFEST_FILE=".github/auto-extract-manifest.tsv"
BASELINE_FILE=".github/auto-extract-baseline.txt"

log()  { printf '%s\n' "$*"; }
note() { printf '::notice::%s\n' "$*"; }
warn() { printf '::warning::%s\n' "$*"; }
die()  { printf '::error::%s\n' "$*"; exit 1; }
report() {
  [ -n "$REPORT_FILE" ] || return 0
  printf '%s\n' "$*" >> "$REPORT_FILE"
}

cd "$REPO_DIR"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

extracted_dirs=()
rejected=()

# --------------------------------------------------------------------------
# Manifest helpers
#
# The manifest is a tab separated list:  <scope><TAB><path relative to repo>
# The scope is the destination folder of an extraction ("." = repository root).
# --------------------------------------------------------------------------
manifest_scope_paths() {
  local want="$1"
  [ -f "$MANIFEST_FILE" ] || return 0
  while IFS=$'\t' read -r scope path; do
    case "$scope" in ''|'#'*) continue ;; esac
    [ "$scope" = "$want" ] && printf '%s\n' "$path"
  done < "$MANIFEST_FILE"
}

manifest_write() {
  # $1 = scope, $2 = file with the new paths (one per line)
  local scope="$1" new_paths="$2"
  local tmp="$WORK_DIR/manifest.tsv"

  : > "$tmp"
  if [ -f "$MANIFEST_FILE" ]; then
    while IFS=$'\t' read -r s p; do
      case "$s" in ''|'#'*) continue ;; esac
      [ "$s" = "$scope" ] && continue
      printf '%s\t%s\n' "$s" "$p" >> "$tmp"
    done < "$MANIFEST_FILE"
  fi

  printf '# scope\tpath\n' >> "$tmp"
  if [ -s "$new_paths" ]; then
    sort -u "$new_paths" | while IFS= read -r p; do
      [ -n "$p" ] && printf '%s\t%s\n' "$scope" "$p" >> "$tmp"
    done
  fi

  mkdir -p "$(dirname "$MANIFEST_FILE")"
  mv "$tmp" "$MANIFEST_FILE"
}

baseline_contains() {
  [ -f "$BASELINE_FILE" ] || return 1
  grep -Fxq -- "$1" "$BASELINE_FILE"
}

# The automation must never delete itself, whatever the archive contains.
is_protected() {
  case "$1" in
    .github/workflows/auto-extract-zip.yml) return 0 ;;
    .github/scripts/extract-zips.sh) return 0 ;;
    .github/auto-extract-manifest.tsv) return 0 ;;
    .github/auto-extract-baseline.txt) return 0 ;;
  esac
  return 1
}

create_baseline_if_missing() {
  local managed="${1:-}"
  [ -f "$BASELINE_FILE" ] && return 0
  log "  📋 recording the repository baseline (files owned by the repository)"
  mkdir -p "$(dirname "$BASELINE_FILE")"

  git ls-files \
    | grep -viE '\.zip$' \
    | grep -vxF "$MANIFEST_FILE" \
    | grep -vxF "$BASELINE_FILE" \
    | sort -u > "$WORK_DIR/baseline.tmp"

  if [ -n "$managed" ] && [ -s "$managed" ]; then
    # Files that came out of the archive belong to the extraction, not to the
    # repository, so they are allowed to disappear with a later archive.
    grep -Fxv -f "$managed" "$WORK_DIR/baseline.tmp" > "$BASELINE_FILE" || true
  else
    cp "$WORK_DIR/baseline.tmp" "$BASELINE_FILE"
  fi
}

# --------------------------------------------------------------------------
# Extraction helpers
# --------------------------------------------------------------------------

# Print the single top level directory of the archive, or nothing when the
# archive must not be flattened.
detect_single_top_dir() {
  local archive="$1"
  unzip -Z1 "$archive" 2>/dev/null | awk '
    NF {
      if (index($0, "/") == 0) { file_at_top = 1 }
      split($0, p, "/")
      tops[p[1]] = 1
    }
    END {
      n = 0
      for (t in tops) { n++; single = t }
      if (n == 1 && !file_at_top) print single
    }'
}

# Extract one archive into $2 (absolute destination) and print the repo
# relative paths of the files it produced.  Returns 1 when the archive was
# rejected.
extract_archive() {
  local archive="$1" destination="$2"
  local entry bad="" prefix="" tmp src
  local scope_rel="${3:-.}"
  local new_paths="${4:-$WORK_DIR/new-paths.txt}"

  [ -f "$archive" ] || die "Archive not found: $archive"

  # --- safety: reject absolute paths and "../" traversal ("zip slip") ------
  while IFS= read -r entry; do
    case "$entry" in
      /*)                bad="absolute path '$entry'" ; break ;;
      ..|../*|*/..|*/../*) bad="path traversal '$entry'" ; break ;;
    esac
  done < <(unzip -Z1 "$archive" 2>/dev/null || true)

  if [ -n "$bad" ]; then
    printf '::error::Refusing %s inside %s — archive skipped.\n' "$bad" "$(basename "$archive")"
    rejected+=("$(basename "$archive")")
    return 1
  fi

  prefix="$(detect_single_top_dir "$archive")"

  # --- unpack into a temporary folder -------------------------------------
  tmp="$WORK_DIR/unpack-$$"
  rm -rf "$tmp"; mkdir -p "$tmp"
  if ! unzip -o -q "$archive" -d "$tmp"; then
    printf '::error::Could not extract %s — archive skipped.\n' "$(basename "$archive")"
    rejected+=("$(basename "$archive")")
    rm -rf "$tmp"
    return 1
  fi

  src="$tmp"
  [ -n "$prefix" ] && src="$tmp/$prefix"

  # --- copy into the destination, overwriting existing files ---------------
  mkdir -p "$destination"
  if [ -d "$src" ]; then
    cp -a "$src/." "$destination/" 2>/dev/null || true
  fi

  # --- list the files that belong to the archive ---------------------------
  : > "$new_paths"
  while IFS= read -r entry; do
    [ -z "$entry" ] && continue
    case "$entry" in */) continue ;; esac
    if [ -n "$prefix" ]; then
      case "$entry" in
        "$prefix"/*) entry="${entry#"$prefix"/}" ;;
        *) continue ;;
      esac
    fi
    [ -z "$entry" ] && continue
    [ -e "$destination/$entry" ] || continue
    if [ "$scope_rel" = "." ]; then
      printf '%s\n' "$entry" >> "$new_paths"
    else
      printf '%s/%s\n' "$scope_rel" "$entry" >> "$new_paths"
    fi
  done < <(unzip -Z1 "$archive" 2>/dev/null || true)

  rm -rf "$tmp"
  return 0
}

# Remove the files a previous extraction brought in but that are not part of
# the new archive any more.
remove_stale_files() {
  local scope="$1" new_paths="$2"
  local scope_dir="$3"
  local stale path dir

  while IFS= read -r path; do
    [ -z "$path" ] && continue
    # Never touch the scope root itself or anything outside the repository.
    [ "$path" = "." ] && continue
    case "$path" in
      /*|../*|*/../*) continue ;;
    esac
    [ -f "$new_paths" ] && grep -Fxq -- "$path" "$new_paths" && continue

    if is_protected "$path"; then
      warn "Keeping $path — it belongs to the auto-extract automation."
      continue
    fi

    if baseline_contains "$path"; then
      warn "Keeping $path — it belongs to the repository, not to the archive."
      continue
    fi

    if [ -e "$REPO_DIR/$path" ]; then
      rm -rf "$REPO_DIR/$path"
      log "  🗑️ removed $path"
      report "removed: $path"
    fi

    # Remove directories that became empty.
    dir="$(dirname "$REPO_DIR/$path")"
    while [ "$dir" != "$scope_dir" ] && [ "$dir" != "$REPO_DIR" ] && [ "$dir" != "/" ]; do
      rmdir "$dir" 2>/dev/null || break
      dir="$(dirname "$dir")"
    done
  done < <(manifest_scope_paths "$scope")
}

# --------------------------------------------------------------------------
# Mode: a ZIP was pushed / uploaded to the repository.
# --------------------------------------------------------------------------
extract_repository_zips() {
  local zip rel dir base scope dest_abs dest_rel new_paths total=0
  local combined="$WORK_DIR/scope-root-paths.txt"

  : > "$combined"

  while IFS= read -r -d '' zip; do
    rel="${zip#./}"
    dir="$(dirname "$rel")"
    base="$(basename "$rel")"

    if [ "$dir" = "." ]; then
      scope="."
      dest_abs="$REPO_DIR"
      dest_rel="."
    else
      scope="$dir"
      dest_abs="$REPO_DIR/$dir"
      dest_rel="$dir"
    fi

    log "  📦 $(basename "$rel") → $( [ "$dest_rel" = "." ] && echo "repository root" || echo "$dest_rel/" )"
    report "archive: $rel → $( [ "$dest_rel" = "." ] && echo "repository root" || echo "$dest_rel/" )"

    new_paths="$WORK_DIR/new-paths.txt"
    if ! extract_archive "$REPO_DIR/$rel" "$dest_abs" "$dest_rel" "$new_paths"; then
      continue
    fi

    cat "$new_paths" >> "$combined"
    total=$((total + 1))

    # The archive is no longer needed — the extracted files are the source.
    if [ "$KEEP_ARCHIVE" != "1" ]; then
      rm -f "$REPO_DIR/$rel"
      log "  🧹 removed the archive $rel"
      report "archive removed: $rel"
    fi
  done < <(git ls-files -z -- '*.zip' '*.ZIP' '*.Zip' 2>/dev/null || true)

  if [ "$total" -eq 0 ]; then
    note "No ZIP archive found in the repository."
    return 0
  fi

  create_baseline_if_missing "$combined"
  remove_stale_files "." "$combined" "$REPO_DIR"
  manifest_write "." "$combined"

  extracted_dirs+=(".")
  log "Extracted $total archive(s) into the repository."

  # List everything the extraction actually changed.
  if [ -n "$REPORT_FILE" ]; then
    git status --porcelain 2>/dev/null | awk '
      {
        code = substr($0, 1, 2)
        path = substr($0, 4)
        gsub(/"/, "", path)
        if (path ~ /^\.github\/auto-extract-(manifest|baseline)/) next
        if (path == "") next
        status = "modified"
        if (code ~ /\?\?/ || code ~ /A/) status = "added"
        else if (code ~ /D/) status = "deleted"
        else if (code ~ /R/) status = "renamed"
        print status ": " path
      }
    ' >> "$REPORT_FILE"
  fi
}

# --------------------------------------------------------------------------
# Mode: ZIPs attached to a GitHub release.
# --------------------------------------------------------------------------
extract_release_zips() {
  local downloads name base zip dest_rel dest_abs new_paths count=0

  [ -n "$RELEASE_TAG" ] || die "RELEASE_TAG is required for release events"
  command -v gh >/dev/null 2>&1 || die "The gh CLI is not available on this runner"

  downloads="$WORK_DIR/release-assets"
  mkdir -p "$downloads"

  if ! gh release download "$RELEASE_TAG" --pattern '*.zip' --dir "$downloads" --clobber >/dev/null 2>&1; then
    note "No ZIP asset found on release $RELEASE_TAG."
    return 0
  fi

  for zip in "$downloads"/*.zip; do
    [ -e "$zip" ] || continue
    base="$(basename "$zip")"
    name="${base%.*}"

    # Never re-process the archives we attach back to the release.
    case "$name" in
      *-extracted) continue ;;
    esac

    dest_rel="releases/$RELEASE_TAG/$name"
    dest_abs="$REPO_DIR/$dest_rel"

    log "  📦 $base → $dest_rel/"

    new_paths="$WORK_DIR/new-paths.txt"
    if ! extract_archive "$zip" "$dest_abs" "$dest_rel" "$new_paths"; then
      continue
    fi
    count=$((count + 1))

    create_baseline_if_missing "$new_paths"
    remove_stale_files "$dest_rel" "$new_paths" "$dest_abs"
    manifest_write "$dest_rel" "$new_paths"

    extracted_dirs+=("$dest_rel")

    # Attach the extracted content back to the release.
    if command -v zip >/dev/null 2>&1; then
      ( cd "$dest_abs" && zip -qr "$WORK_DIR/${name}-extracted.zip" . )
      if gh release upload "$RELEASE_TAG" "$WORK_DIR/${name}-extracted.zip" --clobber >/dev/null 2>&1; then
        log "  📦 attached ${name}-extracted.zip to release $RELEASE_TAG"
      else
        warn "Could not attach ${name}-extracted.zip to release $RELEASE_TAG"
      fi
    fi
  done

  if [ "$count" -eq 0 ]; then
    note "No ZIP asset found on release $RELEASE_TAG."
  else
    log "Extracted $count archive(s) from release $RELEASE_TAG."
  fi
}

# --------------------------------------------------------------------------
# Mode: manual run with a direct download URL.
# --------------------------------------------------------------------------
extract_download() {
  local url="$SOURCE_URL" base name dest_rel dest_abs new_paths

  base="$(basename "${url%%\?*}")"
  [ -n "$base" ] || base="archive.zip"
  name="${base%.*}"

  if ! curl -fsSL "$url" -o "$WORK_DIR/$base"; then
    die "Could not download $url"
  fi

  dest_rel="downloads/$name"
  dest_abs="$REPO_DIR/$dest_rel"

  log "  📦 $base → $dest_rel/"

  new_paths="$WORK_DIR/new-paths.txt"
  if extract_archive "$WORK_DIR/$base" "$dest_abs" "$dest_rel" "$new_paths"; then
    create_baseline_if_missing "$new_paths"
    remove_stale_files "$dest_rel" "$new_paths" "$dest_abs"
    manifest_write "$dest_rel" "$new_paths"
    extracted_dirs+=("$dest_rel")
  fi
}

# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
log "Extracting ZIP archives (mode: $EVENT_NAME)"

if [ -n "$REPORT_FILE" ]; then
  mkdir -p "$(dirname "$REPORT_FILE")"
  : > "$REPORT_FILE"
  report "AVA auto-extract report — mode: $EVENT_NAME"
fi

case "$EVENT_NAME" in
  release)
    extract_release_zips
    ;;
  dispatch)
    if [ -n "$SOURCE_URL" ]; then
      extract_download
    else
      extract_repository_zips
    fi
    ;;
  *)
    extract_repository_zips
    ;;
esac

if [ -n "$OUTPUT_LIST_FILE" ]; then
  mkdir -p "$(dirname "$OUTPUT_LIST_FILE")"
  printf '%s\n' ${extracted_dirs[@]+"${extracted_dirs[@]}"} > "$OUTPUT_LIST_FILE"
fi

if [ "${#rejected[@]}" -gt 0 ]; then
  printf '::error::%s archive(s) skipped: %s\n' \
    "${#rejected[@]}" "${rejected[*]}"
fi

log "Done — ${#extracted_dirs[@]} location(s) extracted."
