#!/usr/bin/env bash
# Throwaway dedicated server that installs itself from the packwiz pack. Run from any empty folder:
#
#   PACK_URL=http://localhost:8080/pack.toml bash /path/to/pack/server/start.sh
#
# First run: downloads the NeoForge installer and installs the server, downloads packwiz-installer-bootstrap,
# pulls the pack's mods for the server side, then starts the server, which stops at the EULA. Accept it by hand in
# eula.txt (never scripted), run again. Every later run re-syncs the mods from the pack before starting.
# Needs java (17+ to run the installer; the server itself runs on Java 25, which the installer's run script expects on PATH).
set -euo pipefail

NEOFORGE_VERSION="${NEOFORGE_VERSION:-26.2.0.76}"
PACK_URL="${PACK_URL:-http://localhost:8080/pack.toml}"
BOOTSTRAP_URL="https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar"
INSTALLER_URL="https://maven.neoforged.net/releases/net/neoforged/neoforge/${NEOFORGE_VERSION}/neoforge-${NEOFORGE_VERSION}-installer.jar"

if [ ! -f "libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/unix_args.txt" ]; then
    echo "Installing the NeoForge ${NEOFORGE_VERSION} server..."
    curl -fsSL -o neoforge-installer.jar "$INSTALLER_URL"
    java -jar neoforge-installer.jar --install-server .
    rm -f neoforge-installer.jar
fi

if [ ! -f packwiz-installer-bootstrap.jar ]; then
    curl -fsSL -o packwiz-installer-bootstrap.jar "$BOOTSTRAP_URL"
fi

echo "Syncing the pack from ${PACK_URL} (server side)..."
java -jar packwiz-installer-bootstrap.jar -g -s server "$PACK_URL"

if [ ! -f eula.txt ] || ! grep -q "eula=true" eula.txt; then
    echo
    echo "The server will now stop at the Minecraft EULA. Read https://aka.ms/MinecraftEULA, set eula=true in eula.txt yourself, and run this script again."
fi

exec java @user_jvm_args.txt "@libraries/net/neoforged/neoforge/${NEOFORGE_VERSION}/unix_args.txt" --nogui "$@"
