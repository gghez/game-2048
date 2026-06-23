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

# Feature graphic (1024x500): 2048-palette number tiles scattered with varied
# rotations, Boggle-dice style, on a cream background.
FEAT="$ASSETS/play-feature-1024x500.png"
TMPD="$(mktemp -d)"; trap 'rm -rf "$TMPD"' EXIT
convert -size 1024x500 "xc:$CREAM" "$FEAT"
declare -A COL=( [2]='#eee4da' [4]='#ede0c8' [8]='#f2b179' [16]='#f59563' [32]='#f67c5f' [64]='#f65e3b' [128]='#edcf72' [256]='#edcc61' [512]='#edc850' [1024]='#edc53f' [2048]='#edc22e' )
# val:cx:cy:angle:size
tiles=( "2048:512:250:-8:152" "512:165:150:13:112" "128:335:108:-19:100" "64:705:118:21:106" "1024:868:182:-12:122" "8:118:348:-23:94" "32:300:392:16:100" "256:505:430:-9:106" "4:705:408:23:96" "16:892:372:-16:102" "2:66:212:19:86" "8:948:300:9:86" )
n=0
for t in "${tiles[@]}"; do
  IFS=: read -r val cx cy ang sz <<< "$t"
  if [ "$val" = 2 ] || [ "$val" = 4 ]; then txt='#776e65'; else txt='#f9f6f2'; fi
  case ${#val} in 1) ps=$((sz*55/100));; 2) ps=$((sz*48/100));; 3) ps=$((sz*38/100));; *) ps=$((sz*30/100));; esac
  tl="$TMPD/t$n.png"
  convert -size ${sz}x${sz} xc:none -fill "${COL[$val]}" -draw "roundrectangle 0,0,$((sz-1)),$((sz-1)),$((sz/7)),$((sz/7))" \
    -font "$FONT" -fill "$txt" -gravity center -pointsize "$ps" -annotate 0 "$val" \
    -background none -rotate "$ang" +repage "$tl"
  read -r Wr Hr <<< "$(identify -format '%w %h' "$tl")"
  x=$((cx - Wr/2)); y=$((cy - Hr/2))
  [ $x -ge 0 ] && xs="+$x" || xs="$x"; [ $y -ge 0 ] && ys="+$y" || ys="$y"
  convert "$FEAT" "$tl" -geometry "${xs}${ys}" -composite "$FEAT"
  n=$((n+1))
done
echo "Generated mipmap buckets + store-assets (icon 512, feature 1024x500)."
