<#
Ferma il server web del gioco individuando il processo dalla porta, non dal nome.

Lo script esiste per la trappola documentata in CLAUDE.md e in start-web.ps1: su Windows
«mvn» è un wrapper «.cmd» che lancia «java.exe», quindi chiudere la shell o il wrapper non
termina la JVM del server, che resta in ascolto sulla porta anche da una sessione
precedente. L'unico criterio affidabile per trovarla è chiedere a Windows chi ascolta su
quella porta e fermare quel PID, indipendentemente da come e quando il server è stato
avviato.
#>

param(
  [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'

# Cerca chi ascolta sulla porta indicata. Restituisce $null se nessuno vi è in ascolto,
# perché il cmdlet solleva un errore quando non trova connessioni corrispondenti.
function Get-PortListener {
  param([int]$Port)

  try {
    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
  } catch {
    $null
  }
}

$listeners = Get-PortListener -Port $Port

if ($null -eq $listeners) {
  Write-Host "Nessun processo in ascolto sulla porta ${Port}: il server è già fermo." -ForegroundColor Yellow
  exit 0
}

$processIds = $listeners | Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $processIds) {
  $process = Get-Process -Id $processId -ErrorAction SilentlyContinue

  if ($null -ne $process) {
    Write-Host "Arresto del processo $($process.ProcessName) (PID $processId) in ascolto sulla porta $Port..."
  } else {
    Write-Host "Arresto del processo PID $processId in ascolto sulla porta $Port..."
  }

  Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
}

$timeoutSeconds = 15
$pollIntervalMs = 500
$elapsedMs = 0
$stillListening = Get-PortListener -Port $Port

while (($null -ne $stillListening) -and ($elapsedMs -lt ($timeoutSeconds * 1000))) {
  Start-Sleep -Milliseconds $pollIntervalMs
  $elapsedMs += $pollIntervalMs
  $stillListening = Get-PortListener -Port $Port
}

if ($null -ne $stillListening) {
  Write-Host "Errore: la porta $Port risulta ancora occupata dopo il tentativo di arresto." -ForegroundColor Red
  exit 1
}

Write-Host "Server fermato: la porta $Port è libera." -ForegroundColor Green
exit 0
