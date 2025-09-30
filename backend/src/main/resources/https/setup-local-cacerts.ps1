# Nastavení proměnných
$certDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$keystoreFile = Join-Path $certDir "openapi-generator-keystore.p12"
$localCacerts = Join-Path $certDir "local-cacerts"
$keystorePassword = "secret"
$cacertsPassword = "changeit"
$alias = "localhost"
$validity = 3650
$hostname = "localhost"

# Kontrola, zda je nastaveno JAVA_HOME
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME není nastaven. Pokusím se najít cestu k Java..."
    try {
        $javaBin = (Get-Command java).Source
        $env:JAVA_HOME = (Split-Path (Split-Path $javaBin -Parent) -Parent)
        Write-Host "Detekován JAVA_HOME: $env:JAVA_HOME"
    }
    catch {
        Write-Host "Java nebyla nalezena. Nastavte JAVA_HOME nebo nainstalujte Javu."
        exit 1
    }
}

# Cesta k originálnímu cacerts souboru
$srcCacerts = Join-Path $env:JAVA_HOME "lib\security\cacerts"
if (-not (Test-Path $srcCacerts)) {
    # Zkusíme alternativní umístění pro některé distribuce Java
    $srcCacerts = Join-Path $env:JAVA_HOME "jre\lib\security\cacerts"
    if (-not (Test-Path $srcCacerts)) {
        Write-Host "Nenalezen cacerts soubor ve standardním umístění. Zkontrolujte instalaci Javy."
        exit 1
    }
}

Write-Host "Kopíruji originální cacerts z: $srcCacerts"
# Vytvoření lokální kopie cacerts
Copy-Item $srcCacerts $localCacerts

Write-Host "Generování self-signed certifikátu pro $hostname s podporou SAN..."

# Vytvoření konfiguračního souboru pro OpenSSL
$opensslConf = Join-Path $certDir "openssl.cnf"
@"
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_req

[dn]
CN = localhost
O = Zabiny Club
OU = Development
L = Prague
ST = Czech Republic
C = CZ

[v3_req]
subjectAltName = @alt_names
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = localhost
DNS.2 = 127.0.0.1
"@ | Out-File -FilePath $opensslConf -Encoding ascii

# Kontrola, zda je OpenSSL nainstalován
try {
    $null = Get-Command openssl -ErrorAction Stop
}
catch {
    Write-Host "OpenSSL není nainstalován nebo není v PATH. Nainstalujte OpenSSL a zkuste to znovu."
    exit 1
}

# Vytvoření privátního klíče a CSR
openssl req -new -newkey rsa:2048 -nodes -keyout "$certDir\localhost.key" -out "$certDir\localhost.csr" -config "$opensslConf"

# Podepsání CSR a vytvoření self-signed certifikátu
openssl x509 -req -days $validity -in "$certDir\localhost.csr" -signkey "$certDir\localhost.key" -out "$certDir\localhost.crt" -extensions v3_req -extfile "$opensslConf"

# Vytvoření PKCS12 keystore s klíčem a certifikátem
openssl pkcs12 -export -in "$certDir\localhost.crt" -inkey "$certDir\localhost.key" -name $alias -out $keystoreFile -passout "pass:$keystorePassword"

Write-Host "Self-signed certifikát vygenerován: $certDir\localhost.crt"
Write-Host "Keystore vytvořen: $keystoreFile (heslo: $keystorePassword)"

# Import certifikátu do lokálního cacerts souboru
Write-Host "Importování certifikátu do lokálního cacerts keystore..."

# Cesta k keytool
$keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"

# Import certifikátu do lokální kopie cacerts
& $keytool -importcert -noprompt -file "$certDir\localhost.crt" -alias $alias -keystore $localCacerts -storepass $cacertsPassword -trustcacerts

# Zobrazení detailů certifikátu
Write-Host "Zobrazení detailů importovaného certifikátu v SAN..."
openssl x509 -in "$certDir\localhost.crt" -noout -text | Select-String -Pattern "Subject Alternative Name" -Context 0,1

Write-Host "Hotovo! Certifikát s podporou localhost byl vygenerován a importován do lokálního keystore."
Write-Host "Lokální cacerts soubor: $localCacerts (heslo: $cacertsPassword)"
