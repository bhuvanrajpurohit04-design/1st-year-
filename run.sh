#!/bin/bash
cd "$(dirname "$0")"
if ! command -v javac >/dev/null 2>&1; then
  echo "Java JDK 11 or newer is required. Install it from https://adoptium.net and try again."; exit 1
fi
mkdir -p out
javac -d out $(find src -name "*.java") && java -cp out hauntedmaze.Main
