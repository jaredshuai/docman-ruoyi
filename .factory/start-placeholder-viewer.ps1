$ErrorActionPreference = 'Stop'

$runtimeRoot = 'D:/codespace/docman-ruoyi/.factory/runtime'
$log = "$runtimeRoot/placeholder-viewer.log"
$err = "$runtimeRoot/placeholder-viewer.err.log"
$healthLog = "$runtimeRoot/placeholder-viewer.health.log"

function Wait-HttpReady {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds,
        [Parameter(Mandatory = $true)][int]$SettleSeconds,
        [Parameter(Mandatory = $true)][string]$ServiceName,
        [Parameter(Mandatory = $true)][int]$ProcessId,
        [Parameter(Mandatory = $true)][string]$HealthLogPath,
        [Parameter(Mandatory = $true)][string]$StdoutLogPath,
        [Parameter(Mandatory = $true)][string]$StderrLogPath
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $stableSince = $null
    while ((Get-Date) -lt $deadline) {
        if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] $ServiceName pid=$ProcessId exited before healthcheck succeeded."
            if (Test-Path $StdoutLogPath) {
                Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] stdout tail:"
                Get-Content -Path $StdoutLogPath -Tail 40 | Add-Content -Path $HealthLogPath
            }
            if (Test-Path $StderrLogPath) {
                Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] stderr tail:"
                Get-Content -Path $StderrLogPath -Tail 40 | Add-Content -Path $HealthLogPath
            }
            throw "$ServiceName exited before becoming healthy."
        }

        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] healthcheck $Url -> HTTP $($response.StatusCode)"
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                if (-not $stableSince) {
                    $stableSince = Get-Date
                    Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] $ServiceName first healthy response observed; starting settle timer (${SettleSeconds}s)."
                } elseif (((Get-Date) - $stableSince).TotalSeconds -ge $SettleSeconds) {
                    Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] $ServiceName remained healthy for settle window."
                    return
                }
            } else {
                $stableSince = $null
            }
        } catch {
            $stableSince = $null
            Add-Content -Path $HealthLogPath -Value "[$(Get-Date -Format o)] healthcheck $Url failed: $($_.Exception.Message)"
        }

        Start-Sleep -Seconds 2
    }

    throw "$ServiceName did not stay healthy on $Url within $TimeoutSeconds seconds."
}

[System.IO.Directory]::CreateDirectory($runtimeRoot) | Out-Null
if (Test-Path $healthLog) {
    Remove-Item $healthLog -Force
}
Set-Content -Path $healthLog -Value "[$(Get-Date -Format o)] starting placeholder viewer validation runtime"
$p = Start-Process -FilePath py -ArgumentList 'D:/codespace/docman-ruoyi/.factory/placeholder_viewer.py','--port','8012' -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
Add-Content -Path $healthLog -Value "[$(Get-Date -Format o)] started pid=$($p.Id)"
Wait-HttpReady -Url 'http://localhost:8012' -TimeoutSeconds 30 -SettleSeconds 5 -ServiceName 'docman-viewer-placeholder' -ProcessId $p.Id -HealthLogPath $healthLog -StdoutLogPath $log -StderrLogPath $err
Write-Output $p.Id
