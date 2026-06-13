# Логирование

Логирование — SLF4J поверх Logback (стандарт Spring Boot). Конфигурация: `src/main/resources/logback-spring.xml`. Уровни можно докручивать через `application.properties` / переменные окружения.

## Куда пишутся логи

| Назначение | Где | Когда |
|---|---|---|
| Консоль | stdout | всегда (во всех профилях) |
| Файл | `${LOG_PATH:-logs}/poemorder.log` | во всех профилях, **кроме** `test` |

Формат строки:
```
2026-06-13 09:43:28.694 INFO  [http-nio-...-exec-1] c.poemorder.app.service.OrderService - New order #5 created (name='Иван')
```
`время | уровень | поток | логгер | сообщение`.

### Ротация файлов

`SizeAndTimeBasedRollingPolicy`:
- новый файл каждый день и при достижении 10 MB;
- архивы сжимаются в `.gz` (`poemorder-YYYY-MM-DD.N.log.gz`);
- хранятся 14 дней, суммарно не более 200 MB.

Путь логов задаётся переменной `LOG_PATH` (по умолчанию `./logs`). Папка `logs/` в `.gitignore`.

> В профиле `test` файловый аппендер отключён — тесты не мусорят на диск.

## Уровни

| Логгер | Уровень | Где задано |
|---|---|---|
| root | `INFO` | `logback-spring.xml` |
| `com.poemorder` | `DEBUG` | `logback-spring.xml` (+ дублируется в `application.properties`) |
| `org.hibernate.SQL` | `INFO` (в тестах `WARN`) | `application.properties` / `application-test.properties` |
| `org.flywaydb` | `WARN` в тестах | `application-test.properties` |

Изменить уровень на лету (без правки кода) — переменной окружения, например:
```bash
LOGGING_LEVEL_COM_POEMORDER=INFO          # тише
LOGGING_LEVEL_ORG_HIBERNATE_SQL=DEBUG     # показать SQL
```
или строкой в `application.properties`: `logging.level.com.poemorder=INFO`.

## Что именно логируется

Доменные события (уровень `INFO`) — кто и что изменил:

| Событие | Логгер | Сообщение |
|---|---|---|
| Новая заявка | `OrderService` | `New order #{id} created (name=...)` |
| Смена статуса заказа | `OrderService` | `Order #{id} status changed X -> Y` |
| Новый отзыв | `ReviewService` | `New review #{id} submitted for moderation ...` |
| Одобрение/скрытие/удаление отзыва | `ReviewService` | `Review #{id} approved` / `... contact hidden` / `... deleted` |
| Создание/обновление/удаление стиха | `PoemService` | `Poem #{id} created/updated/deleted ...` |
| Изменение/удаление контакта | `ContactLinkService` | `Contact link #{id} created/updated/deleted ...` |
| Отклонён небезопасный href (`WARN`) | `ContactLinkService` | `Rejected contact link href with disallowed scheme: '...'` |
| Изменение настроек сайта | `SiteSettingsService` | `Site settings updated` |
| Загрузка админа при старте | `SecurityConfig` | `Loaded admin user '{username}'` |
| Telegram-уведомление не ушло (`WARN`) | `TelegramNotifier` | `Telegram notification failed: ...` |
| Telegram выключен | `TelegramNotifier` | `Telegram notifications disabled ...` |

Принципы:
- **INFO** — значимые изменения состояния (создание/правка/удаление, смена статуса, модерация). По ним восстанавливается, что происходило.
- **WARN** — то, что не сломало запрос, но требует внимания (недоступный Telegram, отклонённый ввод).
- **Без PII в избытке**: логируем имя и id, но не телефон/текст целиком (текст идёт только в Telegram-сообщение, не в лог).
- **Чувствительное не логируется**: пароли (только факт загрузки пользователя по имени), токен Telegram, содержимое сессий.

## Просмотр

```bash
# локально
tail -f logs/poemorder.log
grep "status changed" logs/poemorder.log

# в Docker (логи контейнера = stdout)
docker compose logs -f app
docker compose logs app | grep "New order"
```

В Docker файловый аппендер тоже работает (внутри контейнера в `/app/logs`), но штатный способ — `docker compose logs` (stdout). При желании прокинь том для `/app/logs`, чтобы хранить файлы на хосте.

## Расширение

- Структурированный JSON-лог (для ELK/Loki): добавить `logstash-logback-encoder` и заменить паттерн файла на `LogstashEncoder`.
- MDC с request-id: добавить фильтр, кладущий `traceId` в MDC, и `%X{traceId}` в паттерн — удобно сшивать строки одного запроса.
- Отправка ERROR в Telegram/почту: отдельный appender или обработчик; сейчас осознанно не делается.
