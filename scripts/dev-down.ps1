<#
.SYNOPSIS
    停止 ContentHub 的后端与前端（默认不动 MySQL / Redis 容器）。

.DESCRIPTION
    只结束占用 8084 / 5175 的进程。默认保留容器，方便你在 IDE 里
    跑后端时先释放 8084：
        .\scripts\dev-down.ps1
        # 然后在 IDEA 里 Run

.PARAMETER WithContainers
    连同 contenthub-mysql / contenthub-redis 一起停掉。

.PARAMETER Quiet
    不输出进度信息（供 dev-up.ps1 -Restart 内部调用）。

.EXAMPLE
    .\scripts\dev-down.ps1
.EXAMPLE
    .\scripts\dev-down.ps1 -WithContainers
#>
[CmdletBinding()]
param(
    [switch]$WithContainers,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')

$Root   = Split-Path -Parent $PSScriptRoot
$LogDir = Join-Path $Root 'logs'
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }

function Say {
    param([string]$Message)
    if ($Quiet) { return }
    $line = "[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    Add-Content -Path (Join-Path $LogDir 'dev-down.log') -Value $line -Encoding UTF8
}

function Stop-PortOwner {
    param([int]$Port, [string]$What)

    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) {
        Say "$What（$Port）未在运行，跳过。"
        return
    }

    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $pids) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$procId" -ErrorAction SilentlyContinue
        if (-not $proc) { continue }

        Say "停止 $What：PID $procId ($($proc.Name))"

        # npm 会以 cmd.exe -> node.exe 两层存在，父进程留着会残留窗口
        $parentId = $proc.ParentProcessId

        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue

        if ($parentId) {
            $parent = Get-CimInstance Win32_Process -Filter "ProcessId=$parentId" -ErrorAction SilentlyContinue
            if ($parent -and $parent.Name -eq 'cmd.exe' -and $parent.CommandLine -match 'vite|npm') {
                Say "  同时停止其父进程 PID $parentId (cmd.exe)"
                Stop-Process -Id $parentId -Force -ErrorAction SilentlyContinue
            }
        }
    }

    $deadline = (Get-Date).AddSeconds(20)
    while ((Get-Date) -lt $deadline) {
        if (-not (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
            Say "$What 已停止，端口 $Port 已释放。"
            return
        }
        Start-Sleep -Milliseconds 500
    }
    Say "警告：$What 的端口 $Port 仍未释放。"
}

Say '=== ContentHub dev-down 开始 ==='
Stop-PortOwner -Port 5175 -What '前端'
Stop-PortOwner -Port 8084 -What '后端'

if ($WithContainers) {
    Say '停止 MySQL / Redis 容器...'
    Push-Location $Root
    try {
        $stop = Invoke-External -FilePath 'docker' -ArgumentList @('compose', 'stop')
        if ($stop.ExitCode -ne 0) {
            Write-Host $stop.Output
            Say "警告：docker compose stop 返回 exit $($stop.ExitCode)。"
        }
    } finally {
        Pop-Location
    }
    Say '容器已停止。'
    Say '注意：compose 里是 restart: unless-stopped，手动 stop 的容器在 Docker 重启后不会自动回来；'
    Say '      下次 docker compose up -d 或运行 dev-up.ps1 即可恢复。'
} else {
    Say '（MySQL / Redis 容器保持运行；要一并停止请加 -WithContainers）'
}

Say '=== ContentHub dev-down 完成 ==='
