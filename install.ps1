$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb = Join-Path $projectRoot ".tooling\android-sdk\platform-tools\adb.exe"
$apk = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"
$androidHome = Join-Path $projectRoot ".tooling\android-user"
New-Item -ItemType Directory -Force -Path $androidHome | Out-Null
$env:ANDROID_USER_HOME = $androidHome

if (-not (Test-Path $apk)) { throw "APK introuvable. Lance d'abord .\build.ps1." }
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw "Installation ADB échouée." }
