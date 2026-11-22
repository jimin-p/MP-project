param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = 'emulator-5554'
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$backendRoot = Join-Path $projectRoot 'backend'
function Get-LocalEmulators {
    try { Invoke-RestMethod -Uri 'http://127.0.0.1:4400/emulators' -TimeoutSec 2 }
    catch { $null }
}
Push-Location -LiteralPath $projectRoot
try {
    $deviceState = & $AdbPath -s $Serial get-state 2>$null
    if ($deviceState -ne 'device') { throw 'Start Pixel 8 in Device Manager first.' }
    $emulators = Get-LocalEmulators
    if (!$emulators) {
        $cli = Join-Path $backendRoot 'node_modules\firebase-tools\lib\bin\firebase.js'
        if (!(Test-Path -LiteralPath $cli)) { throw 'Run npm.cmd --prefix backend ci first.' }
        $bundledJava = 'C:\Program Files\Android\Android Studio\jbr'
        if (Test-Path -LiteralPath (Join-Path $bundledJava 'bin\java.exe')) {
            $env:JAVA_HOME = $bundledJava
            $env:PATH = "$bundledJava\bin;$env:PATH"
        }
        $logs = Join-Path $projectRoot 'tmp\local-firebase'
        New-Item -ItemType Directory -Path $logs -Force | Out-Null
        $node = (Get-Command node.exe -ErrorAction Stop).Source
        $server = Start-Process -FilePath $node -WorkingDirectory $backendRoot -WindowStyle Hidden `
            -ArgumentList @(('"' + $cli + '"'), 'emulators:start', '--config', '../firebase.json', '--project', 'demo-classpot', '--only', 'auth,firestore') `
            -RedirectStandardOutput (Join-Path $logs 'stdout.log') `
            -RedirectStandardError (Join-Path $logs 'stderr.log') -PassThru
        Write-Output "Starting local Firebase (PID $($server.Id)). Logs: $logs"
        $deadline = (Get-Date).AddSeconds(180)
        do {
            if ($server.HasExited) { throw "Local Firebase stopped. Check $logs" }
            Start-Sleep -Seconds 2
            $emulators = Get-LocalEmulators
        } until (($emulators.auth -and $emulators.firestore) -or (Get-Date) -gt $deadline)
    }
    if (!$emulators.auth -or !$emulators.firestore -or $emulators.auth.port -ne 9099 -or $emulators.firestore.port -ne 8080) {
        throw 'Local Auth (9099) and Firestore (8080) must be running. Check tmp/local-firebase logs.'
    }
    $previousHost = $env:FIRESTORE_EMULATOR_HOST
    try {
        $env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
        & node backend/scripts/seed-academies.mjs --project demo-classpot --write
        if ($LASTEXITCODE -ne 0) { throw 'Local sample import failed.' }
    } finally { $env:FIRESTORE_EMULATOR_HOST = $previousHost }
    & .\gradlew.bat :app:assembleLocal --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Local APK build failed.' }
    foreach ($port in @(8080, 9099)) {
        & $AdbPath -s $Serial reverse "tcp:$port" "tcp:$port"
        if ($LASTEXITCODE -ne 0) { throw 'ADB reverse failed.' }
    }
    & $AdbPath -s $Serial install -r app/build/outputs/apk/local/app-local.apk
    if ($LASTEXITCODE -ne 0) { throw 'Local APK install failed.' }
    & $AdbPath -s $Serial shell am force-stop com.example.mp2026.local
    & $AdbPath -s $Serial shell am start -W -n com.example.mp2026.local/com.example.mp2026.MainActivity
    if ($LASTEXITCODE -ne 0) { throw 'Local app launch failed.' }
    Write-Output 'Local app ready. Open Running Devices, select Pixel 8, and sign up with a test email/password.'
    Write-Output 'Firebase stays running for manual testing. Only the .local app uses this local setup.'
} finally { Pop-Location }
