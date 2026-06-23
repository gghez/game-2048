#!/usr/bin/env bash
# Generate the launcher icon (all mipmap density buckets) and Play Store graphics
# from a source image. With no source, draws a "2^11" placeholder on the 2048 gold
# tile colour. Requires ImageMagick. No secrets. Re-runnable.
#
# Env (optional):
#   ICON_SRC - 1024x1024 source PNG (default: store-assets/icon-source.png; created
#              as the 2^11 placeholder if absent)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RES="$ROOT/app/src/main/res"
ASSETS="$ROOT/store-assets"
SRC="${ICON_SRC:-$ASSETS/icon-source.png}"
GOLD='#EDC22E'; CREAM='#FAF8EF'
FONT="$(fc-match -f '%{file}' 'DejaVu Sans:bold' 2>/dev/null || echo DejaVuSans-Bold)"
mkdir -p "$ASSETS"

if [ ! -f "$SRC" ]; then
  convert -size 1024x1024 "xc:$GOLD" -font "$FONT" -fill white \
    -gravity center -pointsize 560 -annotate -120+90 '2' \
    -gravity center -pointsize 250 -annotate +180-150 '11' "$SRC"
fi

for d in "mdpi 48" "hdpi 72" "xhdpi 96" "xxhdpi 144" "xxxhdpi 192"; do
  set -- $d; name=$1; px=$2
  mkdir -p "$RES/mipmap-$name"
  convert "$SRC" -resize ${px}x${px} "$RES/mipmap-$name/ic_launcher.png"
  convert "$SRC" -resize ${px}x${px} \
    \( +clone -alpha extract -fill black -colorize 100 \
       -fill white -draw "circle $((px/2)),$((px/2)) $((px/2)),0" \) \
    -alpha off -compose CopyOpacity -composite "$RES/mipmap-$name/ic_launcher_round.png"
done

convert "$SRC" -resize 512x512 "$ASSETS/play-icon-512.png"
convert -size 1024x500 "xc:$CREAM" \
  \( -size 380x380 "xc:$GOLD" -font "$FONT" -fill white \
     -gravity center -pointsize 210 -annotate -46+34 '2' \
     -gravity center -pointsize 95 -annotate +66-58 '11' \) \
  -gravity center -composite "$ASSETS/play-feature-1024x500.png"
echo "Generated mipmap buckets + store-assets (icon 512, feature 1024x500)."
