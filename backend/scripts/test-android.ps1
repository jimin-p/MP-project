param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = 'emulator-5554'
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$createdPorts = @()
Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'APK build failed' }
    foreach ($apk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $AdbPath -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw 'APK install failed' }
    }
    $existing = (& $AdbPath -s $Serial reverse --list) -join "`n"
    foreach ($port in @(8080, 9099)) {
        if ($existing -notmatch "tcp:$port\s+tcp:$port") {
            if ($existing -match "tcp:$port\s+") { throw "Port $port is already forwarded elsewhere" }
            & $AdbPath -s $Serial reverse "tcp:$port" "tcp:$port"
            if ($LASTEXITCODE -ne 0) { throw 'ADB reverse failed' }
            $createdPorts += $port
        }
    }
    $result = (& $AdbPath -s $Serial shell am instrument -w -r -e backendEmulators true -e emulatorHost 127.0.0.1 -e class com.example.mp2026.BackendEmulatorTest com.example.mp2026.test/androidx.test.runner.AndroidJUnitRunner) -join "`n"
    Write-Output $result
    # am instrument can return exit code 0 even when JUnit fails; verify the test count/result.
    if ($result -notmatch 'OK \(1 test\)') { throw 'Android integration test failed or did not run' }
} finally {
    foreach ($port in $createdPorts) { & $AdbPath -s $Serial reverse --remove "tcp:$port" }
    Pop-Location
}
