# Throwaway dedicated server that installs itself from the packwiz pack (Windows PowerShell version of start.sh).
# Run from any empty folder, with `packwiz serve` running in another window from the pack directory:
#
#   powershell -ExecutionPolicy Bypass -File "C:\Users\<you>\Minecraft Redux\pack\server\start.ps1"
#
# First run: downloads the NeoForge installer and installs the server, downloads packwiz-installer-bootstrap,
# pulls the pack's mods for the server side, then starts the server, which stops at the EULA. Accept it by hand in
# eula.txt (never scripted), run again. Every later run re-syncs the mods from the pack before starting.
# The server needs Java 25; the script looks for one under Gradle's toolchain folder, then IntelliJ's JBR, then
# $env:JAVA25_HOME, before falling back to whatever `java` is on the PATH.
param(
    [string]$PackUrl = $(if ($env:PACK_URL) { $env:PACK_URL } else { "http://localhost:8080/pack.toml" }),
    [string]$NeoForgeVersion = $(if ($env:NEOFORGE_VERSION) { $env:NEOFORGE_VERSION } else { "26.2.0.76" })
)
$ErrorActionPreference = "Stop"

function Find-Java25 {
    $candidates = @()
    if ($env:JAVA25_HOME) { $candidates += (Join-Path $env:JAVA25_HOME "bin\java.exe") }
    $gradleJdks = Join-Path $env:USERPROFILE ".gradle\jdks"
    if (Test-Path $gradleJdks) {
        $candidates += Get-ChildItem $gradleJdks -Directory | Where-Object { $_.Name -like "*-25-*" } | ForEach-Object { Join-Path $_.FullName "bin\java.exe" }
    }
    $candidates += Get-ChildItem "C:\Program Files\JetBrains" -Directory -ErrorAction SilentlyContinue | ForEach-Object { Join-Path $_.FullName "jbr\bin\java.exe" }
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    return "java"
}

$java = Find-Java25
Write-Host "Using Java: $java"
$bootstrapUrl = "https://github.com/packwiz/packwiz-installer-bootstrap/releases/latest/download/packwiz-installer-bootstrap.jar"
$installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NeoForgeVersion/neoforge-$NeoForgeVersion-installer.jar"
$argsFile = "libraries\net\neoforged\neoforge\$NeoForgeVersion\win_args.txt"

if (-not (Test-Path $argsFile)) {
    Write-Host "Installing the NeoForge $NeoForgeVersion server..."
    Invoke-WebRequest -Uri $installerUrl -OutFile neoforge-installer.jar
    & $java -jar neoforge-installer.jar --install-server .
    if ($LASTEXITCODE -ne 0) { throw "NeoForge installer failed" }
    Remove-Item neoforge-installer.jar -Force
}

if (-not (Test-Path packwiz-installer-bootstrap.jar)) {
    Invoke-WebRequest -Uri $bootstrapUrl -OutFile packwiz-installer-bootstrap.jar
}

Write-Host "Syncing the pack from $PackUrl (server side)..."
& $java -jar packwiz-installer-bootstrap.jar -g -s server $PackUrl
if ($LASTEXITCODE -ne 0) { throw "packwiz-installer failed; is packwiz serve running?" }

if (-not (Test-Path eula.txt) -or -not (Select-String -Path eula.txt -Pattern "eula=true" -Quiet)) {
    Write-Host ""
    Write-Host "The server will now stop at the Minecraft EULA. Read https://aka.ms/MinecraftEULA, set eula=true in eula.txt yourself, and run this script again."
}

& $java "@user_jvm_args.txt" "@$argsFile" --nogui
