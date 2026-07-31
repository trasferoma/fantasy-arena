<#
Avvia il server web del gioco in background e restituisce subito il prompt.

Lo script esiste per due trappole documentate in CLAUDE.md. La prima: «mvn exec:java» non
ricopia da solo le risorse della pagina in «target/classes/web/», quindi chi modifica
«index.html», «app.js» o «app.css» e riavvia senza rigenerarle vede ancora la versione
vecchia e non capisce perché. La seconda: fermare il wrapper «mvn» non termina la JVM
generata da «java.exe», che resta in ascolto sulla porta anche a shell chiusa. Per questo
il server parte come processo staccato, con l'output in un file di log sotto «target/», e
va fermato con «stop-web.ps1», che individua il processo dalla porta ascoltata e non dal
nome del processo.

Entrambi i comandi Maven girano in offline («-o»): le due dipendenze del progetto sono
SNAPSHOT che vivono solo nel repository Maven locale, quindi la rete non ha niente da
offrire e cercarla rallenta l'avvio. Se un giorno servisse risolvere una dipendenza nuova,
togli «-o» dalle due invocazioni.
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

$repoRoot = $PSScriptRoot

$existingListener = Get-PortListener -Port $Port

if ($null -ne $existingListener) {
  $occupyingProcessId = ($existingListener | Select-Object -First 1 -ExpandProperty OwningProcess)
  $occupyingProcess = Get-Process -Id $occupyingProcessId -ErrorAction SilentlyContinue

  if ($null -ne $occupyingProcess) {
    Write-Host "La porta $Port è già occupata dal processo $($occupyingProcess.ProcessName) (PID $occupyingProcessId)." -ForegroundColor Red
  } else {
    Write-Host "La porta $Port è già occupata dal processo PID $occupyingProcessId." -ForegroundColor Red
  }

  Write-Host "Esegui stop-web.ps1 per fermarlo, oppure scegli un'altra porta con -Port." -ForegroundColor Red
  exit 1
}

Write-Host "Rigenerazione delle risorse web (mvn -o process-resources)..."

Push-Location $repoRoot
try {
  & mvn -o process-resources
  $processResourcesExitCode = $LASTEXITCODE
} finally {
  Pop-Location
}

if ($processResourcesExitCode -ne 0) {
  Write-Host "Errore: 'mvn -o process-resources' è fallito (codice $processResourcesExitCode)." -ForegroundColor Red
  exit 1
}

$logFile = Join-Path $repoRoot 'target\web-server.log'
$cmdArguments = "/c mvn -o exec:java `"-Dexec.args=web $Port`" > `"$logFile`" 2>&1"

Write-Host "Avvio del server sulla porta $Port (log in $logFile)..."
$serverProcess = Start-Process -FilePath 'cmd.exe' -ArgumentList $cmdArguments -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru

$timeoutSeconds = 60
$pollIntervalMs = 500
$elapsedMs = 0
$listener = $null

while ($elapsedMs -lt ($timeoutSeconds * 1000)) {
  $listener = Get-PortListener -Port $Port

  if ($null -ne $listener) {
    break
  }

  if ($serverProcess.HasExited) {
    break
  }

  Start-Sleep -Milliseconds $pollIntervalMs
  $elapsedMs += $pollIntervalMs
}

if ($null -eq $listener) {
  Write-Host "Errore: il server non si è messo in ascolto sulla porta $Port entro $timeoutSeconds secondi." -ForegroundColor Red
  Write-Host "Ultime righe del log ($logFile):" -ForegroundColor Red

  if (Test-Path $logFile) {
    Get-Content -Path $logFile -Tail 30
  }

  exit 1
}

Write-Host "Server avviato. Apri http://127.0.0.1:$Port/ nel browser." -ForegroundColor Green
Write-Host "Ogni ricarica della pagina è una partita nuova. Per fermarlo: stop-web.ps1"
exit 0
