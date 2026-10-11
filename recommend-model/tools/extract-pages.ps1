param(
    [string]$OutputDirectory = "scratch/pdf-review"
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$output = Join-Path $repo $OutputDirectory
New-Item -ItemType Directory -Path $output -Force | Out-Null
$version = '3.0.8'
$m2 = Join-Path $env:USERPROFILE '.m2/repository/org/apache/pdfbox'
$jars = @(
    (Join-Path $m2 "pdfbox/$version/pdfbox-$version.jar"),
    (Join-Path $m2 "pdfbox-io/$version/pdfbox-io-$version.jar"),
    (Join-Path $m2 "fontbox/$version/fontbox-$version.jar"),
    (Join-Path $env:USERPROFILE '.m2/repository/commons-logging/commons-logging/1.3.6/commons-logging-1.3.6.jar')
)
foreach ($jar in $jars) { if (-not (Test-Path $jar)) { throw "Missing PDFBox dependency: $jar" } }
$classPath = $jars -join ';'
$source = Join-Path $PSScriptRoot 'PdfPageExtractor.java'
$files = @('tailieu1.pdf', 'tailieuu2.pdf', 'tailieu3.pdf')
foreach ($file in $files) {
    $inputPdf = Join-Path $repo "recommend-model/docs/sources/$file"
    $outputTsv = Join-Path $output ($file -replace '\.pdf$', '.pages.tsv')
    java "-Dpdfbox.fontcache=$output" -cp $classPath $source $inputPdf $outputTsv
    if ($LASTEXITCODE -ne 0) { throw "PDF extraction failed: $file" }
}
python (Join-Path $PSScriptRoot 'build-review.py') $output
if ($LASTEXITCODE -ne 0) { throw 'Review build failed' }
