$ErrorActionPreference = 'Stop'
[System.IO.Directory]::CreateDirectory('D:/codespace/docman-ruoyi/.factory/runtime') | Out-Null
powershell -NoProfile -ExecutionPolicy Bypass -File 'D:/codespace/docman-ruoyi/.factory/init-viewer-validation.ps1'
$log = 'D:/codespace/docman-ruoyi/.factory/runtime/docman-validation-backend.log'
$err = 'D:/codespace/docman-ruoyi/.factory/runtime/docman-validation-backend.err.log'
Push-Location 'D:/codespace/docman-ruoyi'
try {
    mvn -pl ruoyi-admin -am -DskipTests package | Tee-Object -FilePath $log
} finally {
    Pop-Location
}
$javaArgs = @(
    '-XX:+UseSerialGC',
    '-XX:-UseCompressedClassPointers',
    '-XX:ThreadStackSize=512',
    '-Xms256m',
    '-Xmx512m',
    '-Dspring.profiles.active=local',
    '-Dserver.port=8080',
    '-Dcaptcha.enable=false',
    '-Dsnail-job.enabled=false',
    '-Dspring.boot.admin.client.enabled=false',
    '-Ddocman.storage.localOnly=true',
    '-Ddocman.upload.localRoot=D:/codespace/docman-ruoyi/.factory/runtime/docman-upload',
    '-jar',
    'D:/codespace/docman-ruoyi/ruoyi-admin/target/ruoyi-admin.jar'
)
$p = Start-Process -FilePath java.exe -ArgumentList $javaArgs -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
Write-Output $p.Id
