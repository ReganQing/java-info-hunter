@echo off
setlocal

echo [stop-all] Stopping JavaInfoHunter module processes...
powershell -ExecutionPolicy Bypass -Command ^
  "$patterns = @('spring-boot:run -pl javainfohunter-api','spring-boot:run -pl javainfohunter-crawler','spring-boot:run -pl javainfohunter-processor');" ^
  "$targets = Get-CimInstance Win32_Process | Where-Object { $cmd = $_.CommandLine; $cmd -and ($patterns | Where-Object { $cmd -like ('*' + $_ + '*') }).Count -gt 0 };" ^
  "if (-not $targets) { Write-Host '[stop-all] No matching processes found.'; exit 0 };" ^
  "$targets | ForEach-Object { Write-Host ('[stop-all] Stopping PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force };" ^
  "Write-Host '[stop-all] All matching processes stopped.'"

exit /b 0
