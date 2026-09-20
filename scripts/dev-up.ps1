<#
.SYNOPSIS
    拉起 ContentHub 本地开发环境（幂等，可重复执行）。

.DESCRIPTION
    按依赖顺序启动：
      1. docker compose up -d  ->  contenthub-mysql(3308) + contenthub-redis(6380)
      2. 后端 8084（源码比 jar 新时自动重新打包；已在监听则跳过）
      3. 前端 5175（Vite dev server；已在监听则跳过）

    手动执行与「登录时自启」的计划任务调用的是同一个脚本，
    因此排查问题时手动跑一遍就能复现自启时的完整行为。

.PARAMETER Rebuild
    无条件重新打包后端（默认只在源码比 jar 新时打包）。

.PARAMETER Restart
    先停掉后端 / 前端再启动（默认已在运行就跳过）。

.PARAMETER NoBackend
    不启动后端。要在 IDE 里跑后端时用它，避免 8084 端口冲突。

.PARAMETER NoFrontend
    不启动前端。

.EXAMPLE
    .\scripts\dev-up.ps1
.EXAMPLE
    .\scripts\dev-up.ps1 -Restart
.EXAMPLE
    .\scripts\dev-up.ps1 -NoBackend          # 后端留给 IDEA
.NOTES
    兼容 Windows PowerShell 5.1（计划任务默认用它），不要使用 PS7 专有语法。
    本文件必须保存为「UTF-8 带 BOM」，否则 5.1 会按 GBK 解码导致中文乱码甚至语法错误。
#>
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$Restart,
    [switch]$NoBackend,
    [switch]$NoFrontend
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')

$Root        = Split-Path -Parent $PSScriptRoot
$LogDir      = Join-Path $Root 'logs'
$BackendDir  = Join-Path $Root 'backend'
$FrontendDir = Join-Path $Root 'frontend'
$JarPath     = Join-Path $BackendDir 'contenthub-web\target\contenthub-web-0.0.1-SNAPSHOT.jar'
$BuildLog    = Join-Path $LogDir 'backend-build.log'

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }

function Say {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    Add-Content -Path (Join-Path $LogDir 'dev-up.log') -Value $line -Encoding UTF8
}

Say '=== ContentHub dev-up 开始 ==='

# --------------------------------------------------- 1. 基础设施容器（3308/6380）
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw '找不到 docker 命令，请确认 Docker Desktop 已安装。'
}

Say '等待 Docker 引擎就绪...'
$engineReady = Wait-Until -What 'Docker 引擎' -TimeoutSec 180 -Condition {
    (Invoke-External -FilePath 'docker' -ArgumentList @('info')).ExitCode -eq 0
}
if (-not $engineReady) { throw 'Docker 引擎在 180 秒内未就绪。请先手动启动 Docker Desktop。' }
Say 'Docker 引擎已就绪。'

Say '启动 MySQL / Redis 容器（docker compose up -d）...'
Push-Location $Root
try {
    # compose 里已设 restart: unless-stopped，Docker 引擎重启后容器会自己回来；
    # 这里再执行一次是为了覆盖「容器被手动 stop 过」与首次创建的情况，命令本身幂等。
    $up = Invoke-External -FilePath 'docker' -ArgumentList @('compose', 'up', '-d')
    if ($up.ExitCode -ne 0) {
        Write-Host $up.Output
        throw "docker compose up -d 失败（exit $($up.ExitCode)）。"
    }
} finally {
    Pop-Location
}

Say '等待 MySQL 接受连接（3308）...'
$mysqlOk = Wait-Until -What 'MySQL' -TimeoutSec 180 -Condition {
    if (-not (Test-TcpPort -Port 3308)) { return $false }
    (Invoke-External -FilePath 'docker' -ArgumentList @(
        'exec', 'contenthub-mysql', 'mysqladmin', 'ping', '-h', '127.0.0.1', '-uroot', '-p123456', '--silent'
    )).ExitCode -eq 0
} -OnTimeoutMessage { docker logs contenthub-mysql --tail 20 }
if (-not $mysqlOk) { throw 'MySQL 在 180 秒内未就绪（详见 docker logs contenthub-mysql）。' }
Say 'MySQL 已就绪（contenthub-mysql:3308）。'

Say '等待 Redis 接受连接（6380）...'
$redisOk = Wait-Until -What 'Redis' -TimeoutSec 60 -Condition {
    if (-not (Test-TcpPort -Port 6380)) { return $false }
    (Invoke-External -FilePath 'docker' -ArgumentList @(
        'exec', 'contenthub-redis', 'redis-cli', 'ping'
    )).ExitCode -eq 0
}
if (-not $redisOk) { throw 'Redis 在 60 秒内未就绪（详见 docker logs contenthub-redis）。' }
Say 'Redis 已就绪（contenthub-redis:6380）。'

# --------------------------------------------------------------- 2. 后端 8084
$backendCheck = { Test-HttpOk -Url 'http://127.0.0.1:8084/contents' }

if ($Restart) {
    & (Join-Path $PSScriptRoot 'dev-down.ps1') -Quiet
}

if ($NoBackend) {
    Say '已指定 -NoBackend，跳过后端（留给 IDE 启动）。'
} else {
    $needBuild = [bool]$Rebuild
    if (-not $needBuild) {
        if (-not (Test-Path $JarPath)) {
            Say '后端 jar 不存在，需要打包。'
            $needBuild = $true
        } else {
            $jarTime = (Get-Item $JarPath).LastWriteTime
            $newest = Get-ChildItem -Path $BackendDir -Recurse -File -ErrorAction SilentlyContinue |
                      Where-Object {
                          $_.FullName -notmatch '\\target\\' -and
                          $_.Extension -in '.java', '.xml', '.yml', '.yaml', '.properties'
                      } |
                      Sort-Object LastWriteTime -Descending |
                      Select-Object -First 1
            if ($newest -and $newest.LastWriteTime -gt $jarTime) {
                Say "检测到源码比 jar 新（$($newest.Name)），需要重新打包。"
                $needBuild = $true
            }
        }
    }

    if ($needBuild) {
        $mvn = Resolve-Exe -Names @('mvn.cmd', 'mvn') -FriendlyName 'Maven'
        Say '正在重新打包后端（mvn -DskipTests package），输出写入 logs\backend-build.log ...'
        Push-Location $BackendDir
        try {
            $build = Invoke-External -FilePath $mvn -ArgumentList @('-B', '-DskipTests', 'package')
        } finally {
            Pop-Location
        }
        Set-Content -Path $BuildLog -Value $build.Output -Encoding UTF8
        if ($build.ExitCode -ne 0) {
            Write-Host ($build.Output -split "`n" | Select-Object -Last 25 | Out-String)
            throw "后端打包失败（exit $($build.ExitCode)），完整输出见 logs\backend-build.log"
        }
        Say '后端打包完成。'
    }

    if (& $backendCheck) {
        Say '后端已在 8084 运行，跳过启动。'
    } else {
        $java = Resolve-Exe -Names @('java.exe', 'java') -FriendlyName 'Java'
        Say "启动后端：$java -jar $JarPath"
        Start-Process -FilePath $java `
            -ArgumentList @('-jar', $JarPath) `
            -WorkingDirectory $BackendDir `
            -RedirectStandardOutput (Join-Path $LogDir 'backend.out.log') `
            -RedirectStandardError  (Join-Path $LogDir 'backend.err.log') `
            -WindowStyle Hidden | Out-Null

        $backendUp = Wait-Until -What '后端 8084' -TimeoutSec 180 -Condition $backendCheck
        if (-not $backendUp) {
            Write-Host (Get-Content (Join-Path $LogDir 'backend.err.log') -Tail 25 | Out-String)
            throw '后端在 180 秒内未就绪（详见 logs\backend.err.log）。'
        }
        Say '后端已就绪：http://127.0.0.1:8084'
    }
}

# --------------------------------------------------------------- 3. 前端 5175
if ($NoFrontend) {
    Say '已指定 -NoFrontend，跳过前端。'
} else {
    $frontendCheck = { Test-HttpOk -Url 'http://127.0.0.1:5175/' -Headers @{ Accept = 'text/html' } }
    if (& $frontendCheck) {
        Say '前端已在 5175 运行，跳过启动。'
    } else {
        $npm = Resolve-Exe -Names @('npm.cmd', 'npm') -FriendlyName 'npm'
        Say '启动前端 Vite dev server...'
        Start-Process -FilePath $npm `
            -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1') `
            -WorkingDirectory $FrontendDir `
            -RedirectStandardOutput (Join-Path $LogDir 'frontend.out.log') `
            -RedirectStandardError  (Join-Path $LogDir 'frontend.err.log') `
            -WindowStyle Hidden | Out-Null

        $frontendUp = Wait-Until -What '前端 5175' -TimeoutSec 120 -Condition $frontendCheck
        if (-not $frontendUp) {
            Write-Host (Get-Content (Join-Path $LogDir 'frontend.err.log') -Tail 25 | Out-String)
            throw '前端在 120 秒内未就绪（详见 logs\frontend.err.log）。'
        }
        Say '前端已就绪：http://127.0.0.1:5175'
    }
}

Say '=== ContentHub dev-up 完成 ==='
