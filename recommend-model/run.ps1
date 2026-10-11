$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = Join-Path $projectDir "config.local.ps1"
if (-not (Test-Path -LiteralPath $configPath)) {
    throw "Copy config.example.ps1 to config.local.ps1 and add your API configuration."
}
# Read only the three expected assignments as data. Do not execute config.local.ps1:
# PowerShell execution policy can block dot-sourcing even when run.ps1 starts.
$configText = Get-Content -LiteralPath $configPath -Raw -Encoding utf8
foreach ($name in @('RECOMMEND_LLM_ENDPOINT', 'RECOMMEND_LLM_MODEL', 'RECOMMEND_LLM_API_KEY')) {
    $pattern = '(?m)^\s*\$env:' + [regex]::Escape($name) + '\s*=\s*"([^"]*)"\s*$'
    $match = [regex]::Match($configText, $pattern)
    if (-not $match.Success) {
        throw "Missing $name assignment in config.local.ps1. Use double quotes as in config.example.ps1."
    }
    [Environment]::SetEnvironmentVariable($name, $match.Groups[1].Value, 'Process')
}
if ([string]::IsNullOrWhiteSpace($env:RECOMMEND_LLM_ENDPOINT) -or
    [string]::IsNullOrWhiteSpace($env:RECOMMEND_LLM_MODEL) -or
    [string]::IsNullOrWhiteSpace($env:RECOMMEND_LLM_API_KEY) -or
    $env:RECOMMEND_LLM_ENDPOINT -match "your-provider.example" -or
    $env:RECOMMEND_LLM_MODEL -eq "your-model-name" -or
    $env:RECOMMEND_LLM_API_KEY -eq "paste-your-key-here") {
    throw "Set endpoint, model and API key in config.local.ps1 before running."
}
Push-Location $projectDir
try {
    python -X utf8 -m recommend_model.cli
} finally {
    Pop-Location
}
