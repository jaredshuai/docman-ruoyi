$ErrorActionPreference = 'Stop'
[System.IO.Directory]::CreateDirectory('D:/codespace/docman-ruoyi/.factory/runtime') | Out-Null
$log = 'D:/codespace/docman-ruoyi/.factory/runtime/placeholder-viewer.log'
$err = 'D:/codespace/docman-ruoyi/.factory/runtime/placeholder-viewer.err.log'
$p = Start-Process -FilePath py -ArgumentList 'D:/codespace/docman-ruoyi/.factory/placeholder_viewer.py','--port','8012' -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
Write-Output $p.Id
