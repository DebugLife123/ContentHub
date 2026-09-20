<#
.SYNOPSIS
    注册 / 卸载 ContentHub 的「登录时自启」计划任务。

.DESCRIPTION
    为当前用户注册一个登录触发的计划任务，调用 scripts\dev-up.ps1。

    设计说明：
      - 用当前用户身份 + Interactive 登录类型 + Limited 运行级别，
        因此不需要管理员权限，也不会弹 UAC。
      - 延迟 30 秒触发，给 Docker Desktop 留出启动时间；
        dev-up.ps1 自身还会最多等待 180 秒 Docker 引擎就绪。
      - 不限制执行时长，避免首次自动打包（mvn package）被中断。

.PARAMETER Remove
    卸载已注册的计划任务。

.PARAMETER DelaySeconds
    登录后延迟多少秒触发，默认 30。

.EXAMPLE
    .\scripts\register-autostart.ps1
.EXAMPLE
    .\scripts\register-autostart.ps1 -Remove
#>
[CmdletBinding()]
param(
    [switch]$Remove,
    [int]$DelaySeconds = 30
)

$ErrorActionPreference = 'Stop'

$TaskName = 'ContentHub Dev Up'
$Root     = Split-Path -Parent $PSScriptRoot
$UpScript = Join-Path $PSScriptRoot 'dev-up.ps1'

if ($Remove) {
    $existing = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($existing) {
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
        Write-Host "已卸载计划任务：$TaskName" -ForegroundColor Green
    } else {
        Write-Host "计划任务不存在，无需卸载：$TaskName"
    }
    return
}

if (-not (Test-Path $UpScript)) { throw "找不到 $UpScript，请确认脚本目录完整。" }

$currentUser = "$env:USERDOMAIN\$env:USERNAME"

$action = New-ScheduledTaskAction `
    -Execute 'powershell.exe' `
    -Argument ('-NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File "{0}"' -f $UpScript) `
    -WorkingDirectory $Root

$trigger = New-ScheduledTaskTrigger -AtLogOn -User $currentUser
$trigger.Delay = "PT${DelaySeconds}S"

$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 0) `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 5) `
    -MultipleInstances IgnoreNew

$principal = New-ScheduledTaskPrincipal -UserId $currentUser -LogonType Interactive -RunLevel Limited

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description '登录后自动拉起 ContentHub 开发环境（MySQL/Redis 容器 -> 后端 8084 -> 前端 5175）' `
    -Force | Out-Null

$task = Get-ScheduledTask -TaskName $TaskName
Write-Host ''
Write-Host "已注册计划任务：$TaskName" -ForegroundColor Green
Write-Host ("  触发方式 : 用户 {0} 登录后 {1} 秒" -f $currentUser, $DelaySeconds)
Write-Host ("  运行级别 : {0}" -f $task.Principal.RunLevel)
Write-Host ("  执行脚本 : {0}" -f $UpScript)
Write-Host ''
Write-Host '常用操作：'
Write-Host '  立即试运行 : Start-ScheduledTask -TaskName "ContentHub Dev Up"'
Write-Host '  查看状态   : .\scripts\dev-status.ps1'
Write-Host '  卸载       : .\scripts\register-autostart.ps1 -Remove'
