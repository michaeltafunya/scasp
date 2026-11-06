$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$classOutput = Join-Path $projectRoot 'out'
$sqliteJar = Join-Path $projectRoot 'lib\sqlite-jdbc.jar'
$modules = @(
    'core-domain'
    'concurrent-processing'
    'network-gateway'
    'persistence-jdbc'
    'rmi-administration'
)

if (-not (Get-Command javac.exe -ErrorAction SilentlyContinue)) {
    throw 'A JDK is required. Install/configure the JDK, then reopen PowerShell.'
}
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
    throw 'Java was not found on PATH. Install/configure the JDK, then reopen PowerShell.'
}

$sourceFiles = foreach ($module in $modules) {
    Get-ChildItem (Join-Path $projectRoot $module) -Filter '*.java' -Recurse |
        ForEach-Object { $_.FullName }
}

New-Item -ItemType Directory -Path $classOutput -Force | Out-Null
& javac.exe -d $classOutput -cp $sqliteJar $sourceFiles
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& java.exe -cp "$classOutput;$sqliteJar" scasp.net.GatewayMain
exit $LASTEXITCODE
