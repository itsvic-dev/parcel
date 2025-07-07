#!/usr/bin/env nix-shell
#!nix-shell -i bash -p libxslt.bin
set -e
SCRIPTS_DIR="$(dirname "$0")"

while IFS= read -r -d '' file; do
  echo "[*] Sorting $file"
  xsltproc --output "$file" "$SCRIPTS_DIR/sort-strings.xslt" "$file"
done < <(find "$SCRIPTS_DIR/.." -iname 'strings.xml' -print0)
