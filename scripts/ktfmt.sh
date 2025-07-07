#!/usr/bin/env bash
SCRIPTS_DIR="$(realpath "$(dirname "$0")")"

KTFMT_VERSION="0.54"
KTFMT_FILE="ktfmt-$KTFMT_VERSION-jar-with-dependencies.jar"
KTFMT_DIST="$SCRIPTS_DIR/$KTFMT_FILE"
KTFMT_DOWNLOAD_URL="https://github.com/facebook/ktfmt/releases/download/v$KTFMT_VERSION/$KTFMT_FILE"

if ! (command -v java >/dev/null 2>&1); then
  echo "[!] You do not have Java in your PATH."
  exit 1
fi

if [ ! -e "$KTFMT_DIST" ]; then
  echo "[+] Downloading ktfmt to $KTFMT_DIST..."
  wget -q -O "$KTFMT_DIST" "$KTFMT_DOWNLOAD_URL"
fi

java -jar "$KTFMT_DIST" "$@"
