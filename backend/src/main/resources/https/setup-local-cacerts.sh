#!/bin/bash

# Nastavení proměnných
CERT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_FILE="$CERT_DIR/openapi-generator-keystore.p12"
LOCAL_CACERTS="$CERT_DIR/local-cacerts"
KEYSTORE_PASSWORD="secret"
CACERTS_PASSWORD="changeit"
HOSTNAME="localhost"
ALIAS="localhost"
VALIDITY=3650

if [ -f "$LOCAL_CACERTS" ]; then
  echo "Mazu stary soubor ${LOCAL_CACERTS}"
  rm "$LOCAL_CACERTS"
fi

if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME není nastaven. Pokusím se najít cestu k Java..."
    if command -v java &> /dev/null; then
        JAVA_BIN=$(command -v java)
        JAVA_HOME=$(dirname "$(dirname "$JAVA_BIN")")
        echo "Detekován JAVA_HOME: $JAVA_HOME"
    else
        echo "Java nebyla nalezena. Nastavte JAVA_HOME nebo nainstalujte Javu."
        exit 1
    fi
fi

# Cesta k originálnímu cacerts souboru
SRC_CACERTS="$JAVA_HOME/lib/security/cacerts"
if [ ! -f "$SRC_CACERTS" ]; then
    # Zkusíme alternativní umístění pro některé distribuce Java
    SRC_CACERTS="$JAVA_HOME/jre/lib/security/cacerts"
    if [ ! -f "$SRC_CACERTS" ]; then
        echo "Nenalezen cacerts soubor ve standardním umístění. Zkontrolujte instalaci Javy."
        exit 1
    fi
fi

echo "Kopíruji originální cacerts z: $SRC_CACERTS"
# Vytvoření lokální kopie cacerts
cp "$SRC_CACERTS" "$LOCAL_CACERTS"
chmod 644 "$LOCAL_CACERTS"

echo "Generování self-signed certifikátu pro $HOSTNAME s podporou SAN..."

# Vytvoření konfiguračního souboru pro OpenSSL
cat > "$CERT_DIR/openssl.cnf" << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_req

[dn]
CN = ${HOSTNAME}
O = SK Brno Zabovresky
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
EOF

# Vytvoření privátního klíče a CSR
openssl req -new -newkey rsa:2048 -nodes -keyout "$CERT_DIR/localhost.key" -out "$CERT_DIR/localhost.csr" -config "$CERT_DIR/openssl.cnf"

# Podepsání CSR a vytvoření self-signed certifikátu
openssl x509 -req -days $VALIDITY -in "$CERT_DIR/localhost.csr" -signkey "$CERT_DIR/localhost.key" -out "$CERT_DIR/localhost.crt" -extensions v3_req -extfile "$CERT_DIR/openssl.cnf"

# Vytvoření PKCS12 keystore s klíčem a certifikátem
openssl pkcs12 -export -in "$CERT_DIR/localhost.crt" -inkey "$CERT_DIR/localhost.key" -name "$ALIAS" -out "$KEYSTORE_FILE" -password "pass:$KEYSTORE_PASSWORD"

echo "Self-signed certifikát vygenerován: $CERT_DIR/localhost.crt"
echo "Keystore vytvořen: $KEYSTORE_FILE (heslo: $KEYSTORE_PASSWORD)"


echo "Importování certifikátu do lokálního cacerts keystore..."
# Cesta k keytool
KEYTOOL="$JAVA_HOME/bin/keytool"
# Import certifikátu do lokální kopie cacerts
"$KEYTOOL" -importcert -noprompt -file "$CERT_DIR/localhost.crt" -alias "$ALIAS" -keystore "$LOCAL_CACERTS" -storepass "$CACERTS_PASSWORD" -trustcacerts

echo "Zobrazení detailů importovaného certifikátu v SAN..."
openssl x509 -in "$CERT_DIR/localhost.crt" -noout -text | grep -A1 "Subject Alternative Name"

echo "Hotovo! Certifikát s podporou localhost byl vygenerován a importován do lokálního keystore."
echo "Lokální cacerts soubor: $LOCAL_CACERTS (heslo: $CACERTS_PASSWORD)"

# cleanup
rm openssl.cnf
rm localhost.*