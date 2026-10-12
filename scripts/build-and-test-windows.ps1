# Validacao local COMPROMISSO. Executar pelo PowerShell no Windows com Android SDK e Gradle instalados.
# O script nao coleta arquivos pessoais, nao gera bugreport e nao altera configuracoes de seguranca do aparelho.
[CmdletBinding()]
param(
    [switch]$AllowEmulator,
    [switch]$SkipInstrumentedTests
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $project

function Stop-WithMessage([string]$Message) {
    Write-Host "ERRO: $Message" -ForegroundColor Red
    exit 1
}
function Run-Check([string]$Name, [string]$Command, [string[]]$Arguments) {
    Write-Host "`n== $Name ==" -ForegroundColor Cyan
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { Stop-WithMessage "$Name falhou (codigo $LASTEXITCODE)." }
}

$gradleWrapper = Join-Path $project 'gradlew.bat'
if (Test-Path $gradleWrapper) { $gradle = $gradleWrapper }
else {
    $gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
    if (-not $gradleCmd) {
        Stop-WithMessage 'Gradle nao encontrado. Instale Gradle 9.6.0 ou gere o wrapper 9.6.0 no Android Studio.'
    }
    $gradle = $gradleCmd.Source
}

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path (Join-Path $sdk 'platforms\android-37\android.jar'))) {
    Stop-WithMessage 'SDK Android API 37 nao encontrado. Instale o Android SDK Platform 37 pelo SDK Manager.'
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = $sdk }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) { Stop-WithMessage 'ADB indisponivel. Instale Android SDK Platform-Tools.' }

Run-Check 'Versao do Gradle' $gradle @('--version')
Run-Check 'Auditoria de codigo-fonte' 'python' @('scripts/security_audit.py')
Run-Check 'Testes unitarios JVM Android' $gradle @('testDebugUnitTest', '--no-daemon')
Run-Check 'Android Lint' $gradle @('lintDebug', '--no-daemon')
Run-Check 'Compilacao APK debug' $gradle @('assembleDebug', '--no-daemon')
$apk = Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { Stop-WithMessage 'O APK esperado nao foi gerado.' }
$aapt = Join-Path $sdk 'build-tools\36.0.0\aapt.exe'
if (-not (Test-Path $aapt)) { Stop-WithMessage 'aapt Build Tools 36.0.0 nao encontrado.' }
Run-Check 'Auditoria de permissoes APK' 'python' @('scripts/apk_permission_audit.py', $aapt, $apk)

if ($SkipInstrumentedTests) {
    Write-Host 'Testes no aparelho foram IGNORADOS por solicitacao explicita.' -ForegroundColor Yellow
    exit 0
}

Write-Host "`n== Detectando aparelho Android autorizado ==" -ForegroundColor Cyan
$attached = & $adb devices
if ($LASTEXITCODE -ne 0) { Stop-WithMessage 'Falha na comunicacao com o ADB.' }
$devices = @($attached | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
if (-not $AllowEmulator) { $devices = @($devices | Where-Object { $_ -notmatch '^emulator-' }) }
if ($devices.Count -ne 1) {
    Stop-WithMessage 'Conecte exatamente 1 celular Android por USB, ative a depuracao USB e autorize este PC. Verifique: adb devices. Use -AllowEmulator se desejar emulador.'
}
$serial = $devices[0]
$apiText = & $adb -s $serial shell getprop ro.build.version.sdk
if ($LASTEXITCODE -ne 0 -or -not ($apiText -match '^\s*\d+\s*$')) { Stop-WithMessage 'Nao foi possivel determinar a versao Android.' }
$api = [int]$apiText.Trim()
if ($api -lt 26) { Stop-WithMessage 'O dispositivo precisa ser Android 8.0/API 26 ou posterior.' }
Write-Host "Dispositivo autorizado: $serial (API $api)" -ForegroundColor Green
$env:ANDROID_SERIAL = $serial
Run-Check 'Testes instrumentados no celular' $gradle @('connectedDebugAndroidTest', '--no-daemon')
Write-Host "`nSUCESSO: build, testes JVM, Lint, auditoria APK e testes instrumentados concluídos." -ForegroundColor Green
Write-Host 'ATENCAO: testes manuais de bypass, DNS privado e reinicio ainda sao necessarios.' -ForegroundColor Yellow
Write-Host "APK debug: $apk"
