@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0__%"=="" (SET __MVNW_ARG0__=%~dpnx0& SET __MVNW_PSMODULEPATH__%=%%PSModulePath%%& SET PSModulePath=& powershell.exe -NoLogo -NoProfile -Command^
 "& {[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12;"^
 "$distributionUrl=(Get-Content -Raw '%~dp0.mvn\wrapper\maven-wrapper.properties' | Select-String 'distributionUrl=(.*)').Matches.Groups[1].Value.Trim();"^
 "$wrapperJar='%~dp0.mvn\wrapper\maven-wrapper.jar';"^
 "if (!(Test-Path $wrapperJar)) {"^
 "  $wrapperUrl=(Get-Content -Raw '%~dp0.mvn\wrapper\maven-wrapper.properties' | Select-String 'wrapperUrl=(.*)').Matches.Groups[1].Value.Trim();"^
 "  Write-Host \"Downloading Maven Wrapper from $wrapperUrl...\";"^
 "  (New-Object Net.WebClient).DownloadFile($wrapperUrl, $wrapperJar)"^
 "}"^
 "$mavenHome='%~dp0.mvn\wrapper\maven';"^
 "$mavenBin=\"$mavenHome\bin\";"^
 "if (!(Test-Path \"$mavenBin\mvn.cmd\")) {"^
 "  $zipFile=\"$mavenHome\maven.zip\";"^
 "  Write-Host \"Downloading Maven from $distributionUrl...\";"^
 "  New-Item -ItemType Directory -Force -Path $mavenHome | Out-Null;"^
 "  (New-Object Net.WebClient).DownloadFile($distributionUrl, $zipFile);"^
 "  Expand-Archive $zipFile -DestinationPath $mavenHome -Force;"^
 "  $extracted=(Get-ChildItem $mavenHome -Directory | Where-Object { $_.Name -like 'apache-maven-*' } | Select-Object -First 1).FullName;"^
 "  Get-ChildItem $extracted | Move-Item -Destination $mavenHome -Force;"^
 "  Remove-Item $extracted -Force;"^
 "  Remove-Item $zipFile -Force"^
 "}"^
 "$env:JAVA_HOME ??= (Get-Command java).Source | Split-Path | Split-Path;"^
 "& \"$mavenBin\mvn.cmd\" %*"^
 "}" ) & SET PSModulePath=%__MVNW_PSMODULEPATH__%& SET __MVNW_PSMODULEPATH__%=& EXIT /B %ERRORLEVEL%
