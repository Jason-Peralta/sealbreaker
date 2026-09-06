#!/usr/bin/env bash
# Throwaway dedicated server that installs itself from the packwiz pack (Linux, macOS, Git Bash; Windows PowerShell
# users run start.ps1 instead: the bash on a Windows PATH is usually WSL, which cannot read C: paths). From any
# empty folder:
#
#   PACK_URL=http://localhost:8080/pack.toml bash "/c/Users/<you>/Minecraft Redux/pack/server/start.sh"
#
# First run: downloads the NeoForge installer and installs the server, downloads packwiz-installer-bootstrap,
# pulls the pack's mods for the server side, then starts the server, which stops at the EULA. Accept it by hand in
# eula.txt (never scripted), run again. Every later run re-syncs the mods from the pack before starting.
# The server needs Java 25: JAVA25_HOME, else Gradle's toolchain folder, else IntelliJ's JBR, else java on the PATH.
set -euo pipefail

JAVA=java
if [ -n "${JAVA25_HOME:-}" ] && [ -x "$JAVA25_HOME/bin/java" ]; then
    JAVA="$JAVA25_HOME/bin/java"
else
    for candidate in "$HOME"/.gradle/jdks/*-25-*/bin/java "$HOME"/.gradle/jdks/*-25-*/bin/java.exe "/c/Program Files/JetBrains"/*/jbr/bin/java.exe; do
        if [ -x "$candidate" ]; then
            JAVA="$candidate"
            break
        fi
    done
fi
echo "Using Java: $JAVA"

NEOFORGE_VERSION="${NEOFORGE_VERSION:-26.2.0.76}"
PACK_URL="${PACK_URL:-http://localhost:8080/pack.toml}"
BOOTSTRAP_URL="https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar"
INSTALLER_URL="https://maven.neoforged.net/releases/net/neoforged/neoforge/${NEOFORGE_VERSION}/neoforge-${NEOFORGE_VERSION}-installer.jar"

if [ ! -d "libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}" ]; then
    echo "Installing the NeoForge ${NEOFORGE_VERSION} server..."
    curl -fsSL -o neoforge-installer.jar "$INSTALLER_URL"
    "$JAVA" -jar neoforge-installer.jar --install-server .
    rm -f neoforge-installer.jar
fi

if [ ! -f packwiz-installer-bootstrap.jar ]; then
    curl -fsSL -o packwiz-installer-bootstrap.jar "$BOOTSTRAP_URL"
fi

echo "Syncing the pack from ${PACK_URL} (server side)..."
"$JAVA" -jar packwiz-installer-bootstrap.jar -g -s server "$PACK_URL"

if [ ! -f eula.txt ] || ! grep -q "eula=true" eula.txt; then
    echo
    echo "The server will now stop at the Minecraft EULA. Read https://aka.ms/MinecraftEULA, set eula=true in eula.txt yourself, and run this script again."
fi

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) ARGS="libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/win_args.txt" ;;
    *) ARGS="libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/unix_args.txt" ;;
esac
exec "$JAVA" @user_jvm_args.txt "@${ARGS}" --nogui "$@"
