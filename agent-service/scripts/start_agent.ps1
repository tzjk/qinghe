[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8100
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$python = Join-Path $projectRoot ".venv-clean\Scripts\python.exe"
if (-not (Test-Path -LiteralPath $python)) {
    throw "缺少 .venv-clean。请先按 README 创建 Agent 专用隔离环境。"
}

foreach ($name in @("CONDA_PREFIX", "CONDA_DEFAULT_ENV", "CONDA_PROMPT_MODIFIER", "LLM_API_KEY", "QINGHE_TEST_USER_TOKEN")) {
    Remove-Item "Env:$name" -ErrorAction SilentlyContinue
}
$pathSeparator = [System.IO.Path]::PathSeparator
$env:PATH = (($env:PATH -split $pathSeparator | Where-Object {
    $entry = $_.ToLowerInvariant()
    $_ -and -not ($entry.Contains("anaconda") -or $entry.Contains("miniconda") -or $entry.Contains("\\conda") -or $entry.Contains("\\library\\bin") -or $entry.EndsWith("\\dlls"))
}) -join $pathSeparator)
$env:AGENT_MOCK_MODE = "false"
$env:AGENT_TOOL_MODE = "spring"
$env:QINGHE_BACKEND_ENABLED = "true"
$env:QINGHE_BACKEND_BASE_URL = "http://127.0.0.1:8090"
$env:LLM_PROVIDER = "deterministic"
$env:LLM_API_KEY = ""

Push-Location $projectRoot
try {
    & $python -m uvicorn app.main:app --host 127.0.0.1 --port $Port
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
