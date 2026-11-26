#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "usage: IOS18=<required|optional|skip> ios-simulator-screenshots.sh <path to QuietGrid.app> <output dir>" >&2
  exit 2
fi

app=$1
out=$2
ios18_mode=${IOS18:-optional}
bundle_id=com.quietgrid.app
mkdir -p "$out"

json=$(xcrun simctl list devices available --json)
runtimes=$(echo "$json" | jq -r '.devices | keys[] | select(contains(".iOS-"))' | sort -V)
newest=$(echo "$runtimes" | tail -1)
ios18=$(echo "$runtimes" | grep '\.iOS-18-' | tail -1 || true)

pick() {
  echo "$json" | jq -r --arg rt "$1" --arg prefix "$2" '[.devices[$rt][]? | select(.name | startswith($prefix)) | .udid] | first // empty'
}

shoot() {
  local label=$1 runtime=$2 prefix=$3 required=$4
  shift 4
  local udid=""
  if [ -n "$runtime" ]; then
    udid=$(pick "$runtime" "$prefix")
  fi
  if [ -z "$udid" ]; then
    if [ "$required" = "yes" ]; then
      echo "::error::$label: no $prefix simulator for runtime '${runtime:-none}'"
      exit 1
    fi
    echo "::warning::$label: no $prefix simulator for runtime '${runtime:-none}', skipped"
    return 0
  fi

  echo "$label: $udid ($runtime)"
  xcrun simctl boot "$udid"
  xcrun simctl bootstatus "$udid" -b
  xcrun simctl install "$udid" "$app"
  local launched pid
  launched=$(xcrun simctl launch "$udid" "$bundle_id" ${@+"$@"})
  pid=${launched##* }
  echo "$label: launched as pid $pid"
  sleep 15
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "::error::$label: app is not running (pid $pid is gone)"
    xcrun simctl io "$udid" screenshot "$out/$label-not-running.png" || true
    exit 1
  fi
  xcrun simctl io "$udid" screenshot "$out/$label.png"
  xcrun simctl shutdown "$udid"
}

shoot iphone-newest "$newest" iPhone yes
shoot iphone-newest-fr "$newest" iPhone yes -AppleLanguages "(fr)" -AppleLocale fr_FR
shoot ipad-newest "$newest" iPad yes
case "$ios18_mode" in
  required) shoot iphone-ios18 "$ios18" iPhone yes ;;
  optional) shoot iphone-ios18 "$ios18" iPhone no ;;
  skip) echo "iphone-ios18: skipped (IOS18=skip)" ;;
  *) echo "::error::unknown IOS18 mode '$ios18_mode'"; exit 2 ;;
esac
