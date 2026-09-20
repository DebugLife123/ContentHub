# 公共工具函数。由 dev-up / dev-down / dev-status 通过 dot-source 引入：
#     . (Join-Path $PSScriptRoot '_common.ps1')
#
# 为什么需要 Invoke-External：
#   Windows PowerShell 5.1 下，当 $ErrorActionPreference = 'Stop' 时，原生命令
#   （docker / mvn / npm）写到 stderr 的内容会被包装成 NativeCommandError 并作为
#   终止错误抛出。而 `docker compose up -d` 会把「Container xxx Running」这类
#   正常进度信息写到 stderr —— 于是脚本会在一切正常的情况下中断。
#
#   这里统一把原生命令的 stdout + stderr 合并捕获，只把退出码交回调用方判断，
#   既不会误判，也不会把噪音刷到控制台。

function Invoke-External {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$ArgumentList = @()
    )

    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & $FilePath @ArgumentList 2>&1 | Out-String
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $prev
    }

    return [pscustomobject]@{
        ExitCode = $code
        Output   = $output
    }
}

function Test-TcpPort {
    param([int]$Port)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect('127.0.0.1', $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Test-HttpOk {
    param([string]$Url, [hashtable]$Headers = @{})
    try {
        $resp = Invoke-WebRequest -Uri $Url -Headers $Headers -TimeoutSec 5 -UseBasicParsing
        return ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400)
    } catch {
        return $false
    }
}

function Wait-Until {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Condition,
        [string]$What = '条件',
        [int]$TimeoutSec = 180,
        [scriptblock]$OnTimeoutMessage
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) { return $true }
        Start-Sleep -Seconds 2
    }
    if ($OnTimeoutMessage) { & $OnTimeoutMessage }
    return $false
}

function Resolve-Exe {
    param([string[]]$Names, [string]$FriendlyName)
    foreach ($n in $Names) {
        $cmd = Get-Command $n -ErrorAction SilentlyContinue
        if ($cmd) { return $cmd.Source }
    }
    throw "找不到 $FriendlyName（尝试过：$($Names -join ', ')）。请确认已安装并在 PATH 中。"
}
