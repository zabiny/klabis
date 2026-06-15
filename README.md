# Klabis

## License

This project is licensed under the GNU General Public License v3 (GPL-3.0)  
**with an additional non-commercial restriction**.

### What this means for you

- ✅ Feel free to use, study, and modify the code for **personal, educational, or other non-commercial projects**.
- 🔄 If you share a modified version, please keep it under the same license and clearly mark your changes.
- 📝 Don’t forget to give credit by mentioning the original author/project.
- 🚫 Commercial use (e.g. in paid services, products, or closed-source business projects) is **not allowed** without my
  permission.

### Want to use it commercially?

I’m open to cooperation!  
If you’d like to use this project in a commercial setting, please reach out to me to discuss licensing options:  
📧 [d.polach@dpolach.com](mailto://d.polach@dpolach.com)

For the full details, see the [LICENSE](./LICENSE) file.

## Informace pro vývojáře

### API dokumentace

URL: https://api.klabis.otakar.io/swagger-ui/index.html?urls.primaryName=Members+API

Stále v rané fázi, dynamicky se stále mění (jak URLs, tak i struktura dat).

### Github:

URL: https://github.com/zabiny/klabis

PR vyžaduje approve od [@dapolach](https://github.com/dapolach)

### Lokalni prostredi

Podpurne nastroje (Prometheus/Grafana/Zipkin, apod) je mozne pustit pomoci [docker-compose](./docker-compose.yml). Pak
jsou dostupne tyto sluzby:

- [Prometheus](http://localhost:9090)
- [Grafana](http://localhost:9030) (default user `admin`:`admin`)
- [Zipkin](http://localhost:9411)
- [MailPit](http://localhost:8025) — zachytává odeslané emaily (spustit viz níže)

#### MailPit — první spuštění

TLS certifikát není součástí repozitáře. Před prvním spuštěním ho vygeneruj:

```bash
mkdir -p tools/mailpit/certs
openssl req -x509 -newkey rsa:4096 \
  -keyout tools/mailpit/certs/key.pem \
  -out tools/mailpit/certs/cert.pem \
  -days 3650 -nodes -subj "/CN=mailpit"
```

Pak spusť kontejner:

```bash
docker compose -f docker-compose.mailpit.yml up -d
```

## Analýza

Motivace: https://miro.com/app/board/uXjVMZ6CEqU=/?moveToWidget=3458764550128692580&cot=14
Funkcionalita ("rozsah" projektu - aktualne modre listecky
MVP): https://miro.com/app/board/uXjVMZ6CEqU=/?moveToWidget=3458764559801334302&cot=10

## Běžící aplikace

UI: https://klabis.otakar.io/

UI PoC 'frontend': https://api.klabis.otakar.io/index.html

API: https://klabis-api-docs.otakar.io/



