#!/usr/bin/env bash
#
# Auto-extract ZIP archives that arrive in this repository.
#
# Supported sources
#   EVENT_NAME=push          every *.zip tracked in the repository is unpacked
#                            next to the archive:
#                              assets/app.zip  ->  assets/app/
#   EVENT_NAME=release       every *.zip attached to the release is unpacked
#                            into  releases/<tag>/<asset-name>/  and the
#                            result is attached back to the release as
#                            <asset-name>-extracted.zip
#   SOURCE_URL=<http url>   a single archive downloaded from a URL is unpacked
#                            into  downloads/<name>/
#
# Environment variables
#   REPO_DIR           repository root                (default: current dir)
#   EVENT_NAME         push | release | dispatch      (default: push)
#   RELEASE_TAG        tag of the release             (release mode only)
#   SOURCE_URL         optional archive URL           (dispatch mode)
#   OUTPUT_LIST_FILE   file that receives the list of folders created
#   GH_TOKEN           token used by the gh CLI
#
# The script never writes outside the destination folder: archives containing
# absolute paths or "../" traversal entries are rejected ("zip slip" guard).

set -euo pipefail

REPO_DIR="${REPO_DIR:-$(pwd)}"
EVENT_NAME="${EVENT_NAME:-push}"
RELEASE_TAG="${RELEASE_TAG:-}"
SOURCE_URL="${SOURCE_URL:-}"
OUTPUT_LIST_FILE="${OUTPUT_LIST_FILE:-}"
GH_TOKEN="${GH_TOKEN:-}"

log()  { printf '%s\n' "$*"; }
note() { printf '::notice::%s\n' "$*"; }
warn() { printf '::warning::%s\n' "$*"; }
die()  { printf '::error::%s\n' "$*"; exit 1; }

cd "$REPO_DIR"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

extracted_dirs=()
rejected=()

add_dir() { extracted_dirs+=("$1"); }
reject() { rejected+=("$1"); }

relative_to_repo() {
  printf '%s' "${1#"$REPO_DIR"/}"
}

# --------------------------------------------------------------------------
# Extract a single archive into a destination folder (safely).
# --------------------------------------------------------------------------
extract_archive() {
  local archive="$1" destination="$2"
  local entry bad=""

  [ -f "$archive" ] || die "Archive not found: $archive"

  # Reject dangerous entries *before* anything is written to disk.
  while IFS= read -r entry; do
    case "$entry" in
      /*)                          bad="absolute path '$entry'" ; break ;;
      ..|../*|*/..|*/../*)         bad="path traversal '$entry'" ; break ;;
    esac
  done < <(unzip -Z1 "$archive" 2>/dev/null || true)

  if [ -n "$bad" ]; then
    printf '::error::Refusing %s inside %s — archive skipped.\n' "$bad" "$(basename "$archive")"
    reject "$(basename "$archive")"
    return 1
  fi

  mkdir -p "$destination"

  if ! unzip -o -q "$archive" -d "$destination"; then
    printf '::error::Could not extract %s — archive skipped.\n' "$(basename "$archive")"
    reject "$(basename "$archive")"
    return 1
  fi

  log "  ✅ $(basename "$archive") → $(relative_to_repo "$destination")/"
  return 0
}

# --------------------------------------------------------------------------
# Mode: a ZIP was pushed / uploaded to the repository.
# --------------------------------------------------------------------------
extract_repository_zips() {
  local zip rel dir base name dest count=0

  while IFS= read -r -d '' zip; do
    rel="${zip#./}"
    dir="$(dirname "$rel")"
    base="$(basename "$rel")"
    name="${base%.*}"

    if [ "$dir" = "." ]; then
      dest="$REPO_DIR/$name"
    else
      dest="$REPO_DIR/$dir/$name"
    fi

    if extract_archive "$REPO_DIR/$rel" "$dest"; then
      add_dir "$(relative_to_repo "$dest")"
      count=$((count + 1))
    fi
  done < <(git ls-files -z -- '*.zip' '*.ZIP' '*.Zip' 2>/dev/null || true)

  if [ "$count" -eq 0 ]; then
    note "No ZIP archive found in the repository."
  else
    log "Extracted $count archive(s) from the repository."
  fi
}

# --------------------------------------------------------------------------
# Mode: ZIPs attached to a GitHub release.
# --------------------------------------------------------------------------
extract_release_zips() {
  local downloads name base zip dest count=0

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

    dest="$REPO_DIR/releases/$RELEASE_TAG/$name"
    if ! extract_archive "$zip" "$dest"; then
      continue
    fi
    add_dir "$(relative_to_repo "$dest")"
    count=$((count + 1))

    # Attach the extracted content back to the release.
    if command -v zip >/dev/null 2>&1; then
      ( cd "$dest" && zip -qr "$WORK_DIR/${name}-extracted.zip" . )
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
  local url="$SOURCE_URL" base name dest

  base="$(basename "${url%%\?*}")"
  [ -n "$base" ] || base="archive.zip"
  name="${base%.*}"

  if ! curl -fsSL "$url" -o "$WORK_DIR/$base"; then
    die "Could not download $url"
  fi

  dest="$REPO_DIR/downloads/$name"
  if extract_archive "$WORK_DIR/$base" "$dest"; then
    add_dir "$(relative_to_repo "$dest")"
  fi
}

# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
log "Extracting ZIP archives (mode: $EVENT_NAME)"

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

log "Done — ${#extracted_dirs[@]} folder(s) extracted."
