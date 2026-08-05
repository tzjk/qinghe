[CmdletBinding()]
param([switch]$RealBackend)

$projectRoot = Split-Path -Parent $PSScriptRoot
$python = Join-Path $projectRoot ".venv-clean\Scripts\python.exe"
if (-not (Test-Path -LiteralPath $python)) {
    throw "缺少 .venv-clean。请先按 README 创建 Agent 专用隔离环境。"
}

foreach ($name in @("CONDA_PREFIX", "CONDA_DEFAULT_ENV", "CONDA_PROMPT_MODIFIER", "LLM_API_KEY")) {
    Remove-Item "Env:$name" -ErrorAction SilentlyContinue
}
$pathSeparator = [System.IO.Path]::PathSeparator
$env:PATH = (($env:PATH -split $pathSeparator | Where-Object {
    $entry = $_.ToLowerInvariant()
    $_ -and -not ($entry.Contains("anaconda") -or $entry.Contains("miniconda") -or $entry.Contains("\\conda") -or $entry.Contains("\\library\\bin") -or $entry.EndsWith("\\dlls"))
}) -join $pathSeparator)
$env:LLM_API_KEY = ""

Push-Location $projectRoot
try {
    if ($RealBackend) {
        $env:QINGHE_REAL_BACKEND_TESTS = "true"
        & $python -m pytest -m real_backend
    }
    else {
        Remove-Item Env:QINGHE_REAL_BACKEND_TESTS -ErrorAction SilentlyContinue
        & $python -m pytest -m "not real_backend"
    }
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
