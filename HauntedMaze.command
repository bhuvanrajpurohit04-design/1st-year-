#!/bin/bash
# Double-click launcher: game keeps running even after Terminal is closed.
cd "$(dirname "$0")" || exit 1
if [ ! -d out ]; then
  mkdir -p out && javac -d out $(find src -name "*.java") || { echo "Compile failed"; read -p "Press Enter"; exit 1; }
fi
nohup java -cp out hauntedmaze.Main >/dev/null 2>&1 &
disown
sleep 1
# close this Terminal window (game stays alive)
osascript -e 'tell application "Terminal" to close (every window whose name contains "HauntedMaze")' >/dev/null 2>&1 &
exit 0
