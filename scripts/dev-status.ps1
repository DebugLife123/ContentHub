<#
.SYNOPSIS
    查看 ContentHub 本地开发环境状态。

.EXAMPLE
    .\scripts\dev-status.ps1
#>
[CmdletBinding()]
param(
    [int]$TailLines = 8
)

$ErrorActionPreference = 'Continue'

. (Join-Path $PSScriptRoot '_common.ps1')

$Root   = Split-Path -Parent $PSScriptRoot
$LogDir = Join-Path $Root 'logs'

function Write-Head {
    param([string]$Text)
    Write-Host ''
    Write-Host "=== $Text ===" -ForegroundColor Cyan
}

Write-Head '基础设施容器（ContentHub 专属）'
Push-Location $Root
try {
    $ps = Invoke-External -FilePath 'docker' -ArgumentList @('compose', 'ps', '--format', '{{.Name}}|{{.Status}}')
} finally {
    Pop-Location
}

$lines = @($ps.Output -split "`r?`n" | Where-Object { $_.Trim() -ne '' })
if ($ps.ExitCode -ne 0 -or $lines.Count -eq 0) {
    Write-Host '  没有正在运行的 compose 服务（执行 .\scripts\dev-up.ps1 启动）' -ForegroundColor Yellow
} else {
    foreach ($line in $lines) {
        $parts = $line -split '\|', 2
        $name  = $parts[0]
        $state = if ($parts.Count -gt 1) { $parts[1] } else { '' }
        if ($state -match 'healthy') {
            Write-Host ("  {0,-22} {1}" -f $name, $state) -ForegroundColor Green
        } elseif ($state -match 'Up') {
            Write-Host ("  {0,-22} {1}" -f $name, $state) -ForegroundColor Yellow
        } else {
            Write-Host ("  {0,-22} {1}" -f $name, $state) -ForegroundColor Red
        }
    }
}

Write-Head '服务端口'
foreach ($item in @(
    @{ Port = 3308; Name = 'MySQL' },
    @{ Port = 6380; Name = 'Redis' },
    @{ Port = 8084; Name = '后端 API' },
    @{ Port = 5175; Name = '前端 Dev' }
)) {
    if (Test-TcpPort -Port $item.Port) {
        $conns = Get-NetTCPConnection -LocalPort $item.Port -State Listen -ErrorAction SilentlyContinue
        $owner = ($conns | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
        Write-Host ("  {0,-10} {1,-6} 监听中 (PID {2})" -f $item.Name, $item.Port, $owner) -ForegroundColor Green
    } else {
        Write-Host ("  {0,-10} {1,-6} 未监听" -f $item.Name, $item.Port) -ForegroundColor Yellow
    }
}

Write-Head '接口自检'
try {
    $c = Invoke-RestMethod 'http://127.0.0.1:8084/contents' -TimeoutSec 5
    Write-Host ("  GET /contents        success={0} 条数={1}" -f $c.success, $c.data.Count) -ForegroundColor Green
} catch {
    Write-Host '  GET /contents        不可用' -ForegroundColor Yellow
}
try {
    $h = Invoke-WebRequest 'http://127.0.0.1:5175/' -Headers @{ Accept = 'text/html' } -TimeoutSec 5 -UseBasicParsing
    Write-Host ("  前端首页             HTTP {0}" -f $h.StatusCode) -ForegroundColor Green
} catch {
    Write-Host '  前端首页             不可用' -ForegroundColor Yellow
}

Write-Head '自启任务'
$task = Get-ScheduledTask -TaskName 'ContentHub Dev Up' -ErrorAction SilentlyContinue
if ($task) {
    $info = Get-ScheduledTaskInfo -TaskName 'ContentHub Dev Up' -ErrorAction SilentlyContinue
    Write-Host ("  已注册   状态={0}   上次运行={1}   上次结果=0x{2:X}" -f $task.State, $info.LastRunTime, $info.LastTaskResult)
} else {
    Write-Host '  未注册（运行 scripts\register-autostart.ps1 安装）' -ForegroundColor Yellow
}

Write-Head '最近日志'
foreach ($f in 'dev-up.log', 'backend.err.log', 'frontend.err.log') {
    $p = Join-Path $LogDir $f
    Write-Host "--- $f ---" -ForegroundColor DarkGray
    if (Test-Path $p) {
        Get-Content $p -Tail $TailLines | ForEach-Object { "  $_" }
    } else {
        Write-Host '  (无)'
    }
}
