# Load variables from .env into current PowerShell session
Get-Content "$PSScriptRoot\.env" | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $parts = $_ -split '=', 2
    if ($parts.Count -eq 2) {
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ([string]::IsNullOrWhiteSpace($value)) { return }
        Set-Item -Path "Env:$name" -Value $value
    }
}

Write-Host "Environment loaded from .env" -ForegroundColor Green
Write-Host "DB_URL = $env:DB_URL"
