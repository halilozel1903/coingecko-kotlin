#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample opens each screen from the `scene` extra and loads it live from the keyless CoinGecko API, with the coins'
# logos from CoinGecko's image CDN. Each scene logs when its data and every logo on screen have loaded; a scene that
# shows an error (including CoinGecko refusing or rate limiting the runner) or does not finish loading fails the run.
# There is no fallback data. Every capture is also checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh phone    # pixel_7: markets with the global header, Bitcoin's detail, trending
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

# The text each scene must show; the capture fails without it. None of these appear on the error screen.
expected_text() {
  case "$1" in
    markets) echo "Global market cap" ;;
    detail) echo "All-time high" ;;
    trending) echo "Trending coins" ;;
  esac
}

# The keyless tier is rate limited per IP and CI runners share IPs. Every scene makes one or two calls, and the
# client honors Retry-After, so allow for a few minutes of waiting per scene and pause between scenes.
SCENE_TIMEOUT=300

install_sample
set_night_mode light
first=1
for scene in markets detail trending; do
  if [ "$first" = 0 ]; then sleep 15; fi
  first=0
  fresh_launch --es scene "$scene"
  wait_for_scene "$scene" "$SCENE_TIMEOUT"
  capture "phone-$scene" "$(expected_text "$scene")"
done
