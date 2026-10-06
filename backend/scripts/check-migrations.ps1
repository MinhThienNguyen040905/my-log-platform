$ErrorActionPreference = 'Stop'

$migrationDirectory = Join-Path $PSScriptRoot '..\src\main\resources\db\migration'
$files = @(Get-ChildItem -LiteralPath $migrationDirectory -File -Filter '*.sql')
$pattern = '^V(?<version>[1-9][0-9]*)__[a-z0-9]+(?:_[a-z0-9]+)*\.sql$'
$errors = [System.Collections.Generic.List[string]]::new()
$versions = @{}

foreach ($file in $files) {
    $match = [regex]::Match($file.Name, $pattern)
    if (-not $match.Success) {
        $errors.Add("Invalid Flyway migration name: $($file.Name)")
        continue
    }

    $version = $match.Groups['version'].Value
    if ($versions.ContainsKey($version)) {
        $errors.Add("Duplicate Flyway version V${version}: $($versions[$version]) and $($file.Name)")
    } else {
        $versions[$version] = $file.Name
    }
}

if ($files.Count -eq 0) {
    $errors.Add('No Flyway migrations found.')
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Validated $($files.Count) Flyway migration file(s)."
