$ErrorActionPreference = 'Stop'
[System.IO.Directory]::CreateDirectory('D:/codespace/docman-ruoyi/.factory/runtime') | Out-Null
powershell -NoProfile -ExecutionPolicy Bypass -File 'D:/codespace/docman-ruoyi/.factory/init-viewer-validation.ps1'
$log = 'D:/codespace/docman-ruoyi/.factory/runtime/docman-validation-backend.log'
$err = 'D:/codespace/docman-ruoyi/.factory/runtime/docman-validation-backend.err.log'
$command = "mvn -pl D:/codespace/docman-ruoyi/ruoyi-admin -am -DskipTests package && java -Xms256m -Xmx512m -Dspring.profiles.active=local -Dserver.port=8080 -Dcaptcha.enable=false -Dsnail-job.enabled=false -Dspring.boot.admin.client.enabled=false -Ddocman.storage.localOnly=true -Ddocman.upload.localRoot=D:/codespace/docman-ruoyi/.factory/runtime/docman-upload -jar D:/codespace/docman-ruoyi/ruoyi-admin/target/ruoyi-admin.jar"
$p = Start-Process -FilePath cmd.exe -ArgumentList '/c', $command -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
Write-Output $p.Id
