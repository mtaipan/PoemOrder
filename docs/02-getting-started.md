# Установка и запуск

## Требования

- **Вариант Docker:** Docker + docker-compose. Больше ничего.
- **Вариант локальной разработки:** JDK 21, PostgreSQL 16 (можно в Docker). Maven не нужен — в репо есть wrapper (`mvnw`).

## Вариант 1: всё в Docker (рекомендуется для прода)

```bash
# 1. Конфигурация
cp .env.example .env
# отредактируй .env: как минимум POSTGRES_PASSWORD и APP_ADMINS

# 2. Сборка и запуск
docker compose up --build -d

# 3. Проверка
curl http://localhost:8081/actuator/health
# {"status":"UP", ...}
```

- Сайт: http://localhost:8081
- Админка: http://localhost:8081/admin (логин/пароль из `APP_ADMINS`)
- Приложение слушает только `127.0.0.1:8081` — наружу его открывает reverse proxy (см. [10-deployment.md](10-deployment.md)).

Compose **намеренно падает**, если `APP_ADMINS` не задан в `.env` — это защита от деплоя админки без учётных записей.

## Вариант 2: БД в Docker, приложение локально (для разработки)

```bash
# 1. Только база
docker compose up -d db

# 2. Приложение
APP_ADMINS=admin:admin THYMELEAF_CACHE=false ./mvnw spring-boot:run
```

`THYMELEAF_CACHE=false` включает live-reload шаблонов: правки в `templates/` видны по F5 без рестарта.

> Если `./mvnw` отвечает `permission denied` — запускай `sh mvnw ...` или `chmod +x mvnw`.

## Вариант 3: вообще без Docker

Нужен локальный PostgreSQL 16:

```bash
createuser poems --pwprompt        # пароль poems
createdb poems --owner=poems
APP_ADMINS=admin:admin ./mvnw spring-boot:run
```

Параметры подключения по умолчанию: `jdbc:postgresql://localhost:5432/poems`, пользователь/пароль `poems`/`poems`. Меняются через `SPRING_DATASOURCE_*`.

## Переменные окружения

### Обязательные

| Переменная | Описание |
|---|---|
| `APP_ADMINS` | Учётки админов: `user:pass` или `user1:pass1,user2:pass2`. Без неё приложение не стартует (`IllegalStateException` при создании `userDetailsService`). Пароли хэшируются Argon2 при старте и в памяти в открытом виде не хранятся. |

### База данных

| Переменная | Дефолт | Описание |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/poems` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `poems` | пользователь |
| `SPRING_DATASOURCE_PASSWORD` | `poems` | пароль |
| `DB_POOL_SIZE` | `10` | максимум соединений HikariCP |
| `DB_POOL_MIN_IDLE` | `2` | минимум простаивающих соединений |

### Приложение

| Переменная | Дефолт | Описание |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP-порт |
| `THYMELEAF_CACHE` | `true` | кэш шаблонов; `false` для live-reload при разработке |
| `APP_AUTHOR_NAME` | `Алексей Боков` | имя автора в шапке/футере публичных страниц |
| `RATE_LIMIT_MAX` | `5` | отправок публичных форм с одного IP за окно |
| `RATE_LIMIT_WINDOW_MINUTES` | `10` | длина окна rate limit |
| `TELEGRAM_BOT_TOKEN` | пусто | токен бота; пусто = уведомления выключены |
| `TELEGRAM_CHAT_ID` | пусто | chat id получателя уведомлений |

### Только для docker-compose

| Переменная | Дефолт | Описание |
|---|---|---|
| `POSTGRES_DB` | `poems` | имя БД в контейнере |
| `POSTGRES_USER` | `poems` | пользователь БД |
| `POSTGRES_PASSWORD` | `poems` | пароль БД — **обязательно поменять в проде** |

## Что происходит при первом старте

1. HikariCP подключается к Postgres (ждёт до 30 секунд).
2. Flyway применяет миграции `V1..V10` и создаёт `flyway_schema_history`.
3. Миграция `V3` вставляет дефолтную строку `site_settings` (id=1) — приложение рассчитывает на её существование.
4. Для каждого админа из `APP_ADMINS` хэшируется пароль (Argon2 — это заметные ~0.1–0.5 c на пользователя, нормально).
5. Tomcat поднимается на `SERVER_PORT`. Health становится `UP`.

## Частые проблемы при запуске

См. [13-troubleshooting.md](13-troubleshooting.md).
