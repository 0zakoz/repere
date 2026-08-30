$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = Join-Path $projectRoot ".tooling\jdk"
$env:ANDROID_HOME = Join-Path $projectRoot ".tooling\android-sdk"

Push-Location $projectRoot
try {
    & ".\gradlew.bat" testDebugUnitTest lintDebug assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "La compilation a échoué." }
    Write-Host "APK prêt : app\build\outputs\apk\release\app-release.apk"
}
finally {
    Pop-Location
}
