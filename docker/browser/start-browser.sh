#!/bin/sh
set -eu

browser_name="${BROWSER_NAME:-Chromium}"
mkdir -p /tmp/browser-profile

Xvfb "$DISPLAY" -screen 0 1440x900x24 -ac +extension GLX +render -noreset &
fluxbox >/tmp/fluxbox.log 2>&1 &
x11vnc -display "$DISPLAY" -forever -shared -nopw -rfbport 5900 -listen 0.0.0.0 >/tmp/x11vnc.log 2>&1 &
websockify --web=/usr/share/novnc/ 0.0.0.0:6080 localhost:5900 >/tmp/novnc.log 2>&1 &

case "$browser_name" in
  Firefox) browser="firefox-esr" ;;
  *) browser="chromium" ;;
esac

$browser \
  --no-sandbox \
  --disable-dev-shm-usage \
  --start-maximized \
  --user-data-dir=/tmp/browser-profile \
  "${START_URL:-https://browserrtp.dev}" >/tmp/browser.log 2>&1 &

wait
