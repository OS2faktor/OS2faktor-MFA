#!/bin/bash
set -e

TARGET=${1:-firefox}  # chrome | edge | firefox

if [[ "$TARGET" != "chrome" && "$TARGET" != "edge" && "$TARGET" != "firefox" ]]; then
    echo "Usage: $0 [chrome|edge|firefox]"
    exit 1
fi

MANIFEST="manifests/manifest.$TARGET.json"
if [ ! -f "$MANIFEST" ]; then
    echo "Error: manifest not found: $MANIFEST"
    exit 1
fi

VERSION=$(python3 -c "import json; print(json.load(open('$MANIFEST'))['version'])")

DIST="dist/$TARGET"
rm -rf "$DIST"
mkdir -p "$DIST" releases

cp -r src/* "$DIST/"
cp "$MANIFEST" "$DIST/manifest.json"
cp "manifests/README.$TARGET.md" "$DIST/README.md"

EXT="zip"
[[ "$TARGET" == "firefox" ]] && EXT="xpi"

OUTFILE="releases/os2faktor-${TARGET}-${VERSION}.${EXT}"
(cd "$DIST" && zip -r "../../$OUTFILE" . -x "*.DS_Store") # filter out MacOS junk

echo "Built: $OUTFILE"
