#!/bin/bash
# Run ONCE:  bash make_app.sh   -> creates HauntedMaze.app (double-click, no terminal needed)
cd "$(dirname "$0")" || exit 1
HERE="$(pwd)"
mkdir -p out && javac -d out $(find src -name "*.java") || exit 1
APP="$HERE/HauntedMaze.app"
rm -rf "$APP"; mkdir -p "$APP/Contents/MacOS"
cat > "$APP/Contents/Info.plist" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleName</key><string>HauntedMaze</string>
<key>CFBundleExecutable</key><string>HauntedMaze</string>
<key>CFBundleIdentifier</key><string>local.hauntedmaze</string>
<key>CFBundlePackageType</key><string>APPL</string>
</dict></plist>
PL
cat > "$APP/Contents/MacOS/HauntedMaze" <<L
#!/bin/bash
cd "$HERE" || exit 1
JAVA="\$(/usr/libexec/java_home 2>/dev/null)/bin/java"
[ -x "\$JAVA" ] || JAVA=java
exec "\$JAVA" -Xdock:name="Haunted Maze" -cp out hauntedmaze.Main
L
chmod +x "$APP/Contents/MacOS/HauntedMaze"
xattr -cr "$APP" 2>/dev/null
echo "Done! Double-click HauntedMaze.app (drag it to Dock/Applications if you like)."
