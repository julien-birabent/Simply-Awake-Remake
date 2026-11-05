# Enregistre les 4 secrets de signature (RELEASE_*) dans le dépôt GitHub.
# Usage : powershell -ExecutionPolicy Bypass -File tools/release/set-release-secrets.ps1
# Les valeurs sont saisies au clavier, vérifiées avec keytool, envoyées à gh, et ne sont écrites nulle part.
# Voir docs/RELEASE.md.
$ErrorActionPreference = "Stop"
$repo = "julien-birabent/Simply-Awake-Remake"
$gh = "C:\Program Files\GitHub CLI\gh.exe"
$keytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

function Read-Plain([string]$prompt) {
    $s = Read-Host $prompt -AsSecureString
    $p = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($p) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p) }
}

do {
    $jks = (Read-Host "Chemin complet de upload-key.jks").Trim().Trim('"')
    if (-not (Test-Path -LiteralPath $jks)) { Write-Host "Fichier introuvable : $jks" -ForegroundColor Yellow }
} until (Test-Path -LiteralPath $jks)

if (-not (Test-Path $keytool)) { throw "keytool introuvable : $keytool" }

# Boucle d'essais : on ne continue que si keytool accepte le mot de passe.
$attempt = 0
$aliases = @()
while ($true) {
    $attempt++
    $storePass = Read-Plain "Mot de passe du keystore (essai $attempt, Entree vide pour abandonner)"
    if (-not $storePass) { Write-Host "Abandon. Aucun secret modifie."; exit 1 }
    $lines = & $keytool -list -keystore $jks -storepass $storePass 2>&1
    if ($LASTEXITCODE -eq 0) { Write-Host "Mot de passe accepte." -ForegroundColor Green; break }
    Write-Host "Refuse par keytool, reessaie." -ForegroundColor Yellow
}

$aliases = @($lines | Where-Object { $_ -match '^(.+?),\s+\d' } | ForEach-Object { $Matches[1].Trim() })
$alias = $null
if ($aliases.Count -eq 1) { $alias = $aliases[0]; Write-Host "Alias detecte : $alias" }
elseif ($aliases.Count -gt 1) { Write-Host "Alias presents : $($aliases -join ', ')" }
if (-not $alias) { $alias = (Read-Host "Alias de la cle d'upload").Trim() }

# Mot de passe de la clé : vérifié aussi (Entrée = identique au keystore).
while ($true) {
    $keyPass = Read-Plain "Mot de passe de la cle (Entree si identique au keystore)"
    $test = if ($keyPass) { $keyPass } else { $storePass }
    # keytool -certreq exige le mot de passe de la clé ; sortie ignorée.
    $null = & $keytool -certreq -keystore $jks -storepass $storePass -alias $alias -keypass $test -file "$env:TEMP\sa-certreq-check.csr" 2>&1
    Remove-Item -LiteralPath "$env:TEMP\sa-certreq-check.csr" -ErrorAction SilentlyContinue
    if ($LASTEXITCODE -eq 0) { Write-Host "Mot de passe de la cle accepte." -ForegroundColor Green; break }
    Write-Host "Refuse pour l'alias '$alias', reessaie." -ForegroundColor Yellow
}

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($jks))

& $gh secret set RELEASE_KEYSTORE_BASE64 --repo $repo --body $b64
& $gh secret set RELEASE_STORE_PASSWORD  --repo $repo --body $storePass
& $gh secret set RELEASE_KEY_ALIAS       --repo $repo --body $alias
if ($keyPass) { & $gh secret set RELEASE_KEY_PASSWORD --repo $repo --body $keyPass }
else { & $gh secret delete RELEASE_KEY_PASSWORD --repo $repo 2>$null; Write-Host "RELEASE_KEY_PASSWORD non defini (le mot de passe du keystore sera utilise)." }

Write-Host ""
& $gh secret list --repo $repo
Write-Host "Termine : les secrets sont en place." -ForegroundColor Green
