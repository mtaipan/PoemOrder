# Деплой в продакшен

Целевая схема: **reverse proxy (TLS) → приложение в Docker → PostgreSQL в Docker**.

```
Интернет ──HTTPS──► nginx/caddy ──HTTP──► app:8081 (127.0.0.1) ──► db:5432
                     (TLS, заголовки)      (Spring Boot)            (Postgres, volume pgdata)
```

## Шаги

```bash
# 1. На сервере
git clone <repo> && cd PoemOrder

# 2. Конфигурация
cp .env.example .env
nano .env   # POSTGRES_PASSWORD, APP_ADMINS (сильные!), при желании TELEGRAM_*

# 3. Запуск
docker compose up -d --build

# 4. Проверка
curl http://127.0.0.1:8081/actuator/health   # {"status":"UP"}
docker compose logs -f app                    # смотрим старт
```

`.env` для прода минимально:
```env
POSTGRES_DB=poems
POSTGRES_USER=poems
POSTGRES_PASSWORD=<длинный-случайный>
APP_ADMINS=admin:<сильный-пароль>
THYMELEAF_CACHE=true
TELEGRAM_BOT_TOKEN=<если нужно>
TELEGRAM_CHAT_ID=<если нужно>
```

## Что делает docker-compose

- **db** — Postgres 16-alpine, данные в named volume `pgdata`, healthcheck `pg_isready`.
- **app** — multi-stage сборка из `Dockerfile`, запуск **не под root** (`appuser`), `MaxRAMPercentage=75`. Стартует только после `db: service_healthy` (`depends_on`).
- Порт приложения опубликован как `127.0.0.1:8081:8081` — **снаружи напрямую недоступен**, только через reverse proxy.
- `APP_ADMINS` помечен как обязательный (`${APP_ADMINS:?...}`) — compose упадёт с понятной ошибкой, если переменная не задана.

## Reverse proxy

Приложение само TLS не терминирует. Нужен nginx/caddy перед ним.

### nginx (пример)

```nginx
server {
    listen 443 ssl http2;
    server_name poems.example.com;

    ssl_certificate     /etc/letsencrypt/live/poems.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/poems.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {                       # redirect http→https
    listen 80;
    server_name poems.example.com;
    return 301 https://$host$request_uri;
}
```

`X-Forwarded-For` и `X-Forwarded-Proto` **обязательны**:
- `X-Forwarded-For` — иначе rate limiter увидит IP proxy у всех клиентов и будет лимитировать всех скопом (см. [08-antispam.md](08-antispam.md)).
- `X-Forwarded-Proto` — иначе сгенерированные приложением ссылки/редиректы могут получиться `http://`. Приложение учитывает заголовок (`server.forward-headers-strategy=framework`).

### caddy (пример, TLS автоматом)

```
poems.example.com {
    reverse_proxy 127.0.0.1:8081
}
```

Caddy сам выпускает Let's Encrypt и проставляет forwarded-заголовки.

## Статика

`/css/**` и `favicon.ico` можно кэшировать на proxy (длинный `Cache-Control`). Сам Spring отключает кэш для своих ответов — это про HTML, не про статику.

## Обновление версии

```bash
git pull
docker compose up -d --build      # пересоберёт app, db не тронет
docker compose logs -f app        # проверь, что Flyway применил новые миграции и health UP
```

Откат: вернуть прежний коммит и пересобрать. **Важно:** Flyway-миграции вперёд-совместимы, но не откатываются автоматически — если новая версия добавила миграцию, откат кода не вернёт схему. Перед рискованными миграциями делай дамп (ниже).

## Бэкапы

```bash
# ручной дамп
docker exec poems_db pg_dump -U poems poems > backup_$(date +%F).sql

# восстановление
cat backup_2026-06-12.sql | docker exec -i poems_db psql -U poems poems
```

Автоматизация — cron на хосте:
```cron
0 3 * * * docker exec poems_db pg_dump -U poems poems | gzip > /backups/poems_$(date +\%F).sql.gz
```

Данные в volume `pgdata` переживают `docker compose down`. Удаляет их только `docker compose down -v` — **не делай этого в проде без дампа**.

## Мониторинг

- **Health:** `GET /actuator/health` → `{"status":"UP"}`. Подключи к uptime-мониторингу (через proxy лучше отдавать на отдельном внутреннем пути или ограничить доступ).
- **Логи:** `docker compose logs app`. Уровень — INFO; SQL Hibernate на INFO (не сыпет запросами).
- **Telegram** сам по себе сигнализирует о жизни: если перестали приходить заявки, которые точно были, — повод проверить.

## Чек-лист перед публикацией

- [ ] `APP_ADMINS` и `POSTGRES_PASSWORD` — не из примеров.
- [ ] `.env` не в git, права 600.
- [ ] Reverse proxy с валидным TLS, http→https редирект.
- [ ] Проброшены `X-Forwarded-For` / `X-Forwarded-Proto`.
- [ ] Настроен бэкап БД (cron + проверка восстановления).
- [ ] Health-check в мониторинге.
- [ ] `THYMELEAF_CACHE=true` (дефолт; не выставляй `false` в проде).
