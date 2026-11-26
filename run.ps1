param(
    [switch]$Test,
    [switch]$BuildOnly,
    [switch]$Console
)

$ErrorActionPreference = 'Stop'

if (($Test -and $BuildOnly) -or ($Test -and $Console) -or ($BuildOnly -and $Console)) {
    throw 'Choose only one of -Test, -BuildOnly, or -Console.'
}

$projectRoot = $PSScriptRoot
$buildDir = Join-Path $projectRoot 'obj\classes'

function Get-JdkBin {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin'
        if ((Test-Path (Join-Path $candidate 'javac.exe')) -and (Test-Path (Join-Path $candidate 'java.exe'))) {
            return $candidate
        }
    }

    $javacOnPath = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacOnPath) {
        $candidate = Split-Path $javacOnPath.Source -Parent
        if (Test-Path (Join-Path $candidate 'java.exe')) {
            return $candidate
        }
    }

    $searchRoots = @(
        (Join-Path $env:ProgramFiles 'JetBrains'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Microsoft')
    )
    foreach ($searchRoot in $searchRoots) {
        if (-not (Test-Path $searchRoot)) { continue }
        foreach ($installation in (Get-ChildItem -LiteralPath $searchRoot -Directory | Sort-Object Name -Descending)) {
            foreach ($candidate in @(
                (Join-Path $installation.FullName 'jbr\bin'),
                (Join-Path $installation.FullName 'bin')
            )) {
                if ((Test-Path (Join-Path $candidate 'javac.exe')) -and (Test-Path (Join-Path $candidate 'java.exe'))) {
                    return $candidate
                }
            }
        }
    }

    throw 'Java 17+ JDK not found. Install a JDK or set JAVA_HOME to its installation directory.'
}

$jdkBin = Get-JdkBin
$javac = Join-Path $jdkBin 'javac.exe'
$java = Join-Path $jdkBin 'java.exe'
$sourceRoots = @(Join-Path $projectRoot 'src')
if ($Test) { $sourceRoots += (Join-Path $projectRoot 'tests') }
$sources = @(Get-ChildItem -LiteralPath $sourceRoots -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object FullName)

if ($sources.Count -eq 0) { throw 'No Java source files found.' }
New-Item -ItemType Directory -Path $buildDir -Force | Out-Null

Write-Host "Compiling with $javac"
& $javac --release 17 -d $buildDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($BuildOnly) {
    Write-Host "Build complete: $buildDir"
    exit 0
}

Push-Location $projectRoot
try {
    if ($Test) {
        & $java -ea -cp $buildDir rankmatch.RankMatchTests
    } elseif ($Console) {
        & $java -cp $buildDir rankmatch.RankMatch
    } else {
        & $java -cp $buildDir rankmatch.RankMatchGui
    }
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
