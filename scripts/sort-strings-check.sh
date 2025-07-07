#!/usr/bin/env nix-shell
#!nix-shell -i bash -p libxslt.bin
set -e
SCRIPTS_DIR="$(dirname "$0")"

file_fail() {
  echo "[!] File $1 failed check."
  exit 1
}

while IFS= read -r -d '' file; do
  xsltproc --output "/tmp/strings-check" "$SCRIPTS_DIR/sort-strings.xslt" "$file"
  diff "$file" "/tmp/strings-check" > /dev/null || file_fail "$file"
  rm "/tmp/strings-check"
done < <(find "$SCRIPTS_DIR/.." -iname 'strings.xml' -print0)
