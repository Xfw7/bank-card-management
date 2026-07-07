# Load variables from .env into current PowerShell session
Get-Content "$PSScriptRoot\.env" | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $parts = $_ -split '=', 2
    if ($parts.Count -eq 2) {
        Set-Item -Path "Env:$($parts[0].Trim())" -Value $parts[1].Trim()
    }
}

Write-Host "Environment loaded from .env" -ForegroundColor Green
Write-Host "DB_URL = $env:DB_URL"
