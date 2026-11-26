param(
    [string]$JdkBin = 'C:\oracleJdk-27\bin'
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$classes = Join-Path $root 'obj\package-classes'
$inputDir = Join-Path $root 'obj\package-input'
$runtime = Join-Path $root 'obj\package-runtime'
$jar = Join-Path $inputDir 'RankMatch.jar'
$output = Join-Path $root 'deliverables\RankMatch'
$archive = Join-Path $root 'deliverables\RankMatch-Windows.zip'

foreach ($name in @('javac', 'jar', 'jlink', 'jpackage')) {
    if (-not (Test-Path (Join-Path $JdkBin "$name.exe"))) {
        throw "Missing $name.exe in $JdkBin. Pass -JdkBin with a Java 17+ JDK bin directory."
    }
}

$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -File -Filter '*.java' |
    Sort-Object FullName | ForEach-Object FullName)
if ($sources.Count -eq 0) { throw 'No Java source files found.' }

if (Test-Path $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& (Join-Path $JdkBin 'javac.exe') --release 17 -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (Test-Path $inputDir) { Remove-Item -LiteralPath $inputDir -Recurse -Force }
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
& (Join-Path $JdkBin 'jar.exe') --create --file $jar --main-class rankmatch.RankMatchGui -C $classes .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (Test-Path $runtime) { Remove-Item -LiteralPath $runtime -Recurse -Force }
& (Join-Path $JdkBin 'jlink.exe') --add-modules java.desktop --strip-debug --no-man-pages --no-header-files --output $runtime
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (Test-Path $output) { Remove-Item -LiteralPath $output -Recurse -Force }
& (Join-Path $JdkBin 'jpackage.exe') --type app-image --name RankMatch --input $inputDir --main-jar RankMatch.jar --main-class rankmatch.RankMatchGui --runtime-image $runtime --dest (Join-Path $root 'deliverables')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (Test-Path $archive) { Remove-Item -LiteralPath $archive }
& tar.exe -a -c -f $archive -C (Join-Path $root 'deliverables') RankMatch
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Executable: $(Join-Path $output 'RankMatch.exe')"
Write-Host "Portable ZIP: $archive"
