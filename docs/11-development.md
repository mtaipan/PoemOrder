# Гайд разработчика

## Запуск для разработки

```bash
docker compose up -d db
APP_ADMINS=admin:admin THYMELEAF_CACHE=false ./mvnw spring-boot:run
```

`THYMELEAF_CACHE=false` → правки шаблонов видны по F5 без рестарта. Правки Java требуют рестарта (devtools не подключён).

Сборка / тесты:
```bash
./mvnw clean package          # сборка jar (с тестами — нужен Postgres)
./mvnw -DskipTests package    # без тестов
./mvnw test                   # тесты (нужен Postgres + APP_ADMINS)
```

> `./mvnw permission denied` → `sh mvnw ...` или `chmod +x mvnw`.

## Структура кода и конвенции

```
config/        инфраструктура (Security, Cache). Бины и @Configuration.
domain/        JPA-сущности по агрегатам: order/ poem/ review/ settings/
               + энумы статусов рядом с сущностями (PoemStatus, OrderStatus, ReviewStatus).
dto/           формы публичной части с Bean Validation.
repo/          интерфейсы Spring Data JPA. Запросы — derived-методами, @Query только при необходимости.
service/       бизнес-логика, границы транзакций. Маппинг DTO→entity здесь.
integration/   внешние системы (telegram/).
web/
  RateLimitFilter
  public_/     публичные контроллеры + GlobalModelAttributes (@ControllerAdvice).
  admin/       контроллеры /admin/**.
```

Конвенции, которые сложились в коде (придерживайся их):

- **Конструкторная инъекция**, поля `final`. Никаких `@Autowired` на полях.
- **Контроллер тонкий**: принять/провалидировать ввод, вызвать сервис, выбрать view. Без бизнес-логики и без прямого доступа к репозиториям.
- **Сервис транзакционный**: `@Transactional` на запись, `@Transactional(readOnly = true)` на чтение. Вся работа с БД — внутри (`open-in-view=false`).
- **DTO для публичного ввода**, сущности — внутри. В админских формах допустим биндинг в сущность, но системные поля (`id`, `createdAt`, `status`, где не редактируется) затирай в сервисе.
- **PRG**: каждый успешный POST → `redirect:`; сообщения и ошибки через `RedirectAttributes`/flash.
- **Только `th:text`** в шаблонах (экранирование). `th:utext` — под запретом (XSS).
- Пакет `web.public_` назван с подчёркиванием, потому что `public` — зарезервированное слово Java.

## Слой данных

- Время: `Instant` в Java ↔ `timestamptz` в Postgres, UTC (`hibernate.jdbc.time_zone=UTC`).
- Таймстемпы: `createdAt` проставляется в `@PrePersist` у всех сущностей (`Poem`, `Order`, `Review`); `Poem` дополнительно обновляет `updatedAt` в `@PreUpdate`. Сервисы дату не трогают.
- Энумы хранятся строкой (`@Enumerated(EnumType.STRING)`) — переименование значения = миграция данных, добавление нового значения безопасно.

## Админ-интерфейс (фронтенд)

Лёгкий, без зависимостей — упор на быструю загрузку (принцип «14 КБ»):

- **Один файл стилей** `static/css/admin.css` (~8 КБ raw, ~2.5 КБ gzip). Системные шрифты (`-apple-system, Segoe UI, Roboto…`), без веб-шрифтов и CDN. Светлая/тёмная тема через `prefers-color-scheme` + CSS-переменные.
- **Без JS-фреймворков.** Единственный скрипт — инлайновый `confirm()` на кнопках удаления. Подсветка активного пункта меню — серверная: layout принимает параметр `layout(content, active)`, каждый шаблон передаёт свой ключ (`'orders'`, `'poems'`…).
- **Компоненты** (классы в `admin.css`): `.topbar/.nav`, `.card`, `.btn`/`.btn-primary`/`.btn-danger`/`.btn-sm`, `.tbl` (таблицы), `.badge` + `.b-<status>` (статусные пилюли), `.field`/`.grid2` (формы), `.alert.ok`/`.alert.err`, `.empty`, `.pager`, `.tile` (плитки дашборда).
- **CSRF в формах** добавляется Thymeleaf автоматически для любого `<form>` с `th:action` — вручную скрытый `_csrf` не пишем (избегаем дублей).
- **Статусные бейджи** красятся по значению енума: `th:classappend="${'b-' + #strings.toLowerCase(order.status)}"`. Классы: `b-new/b-in_progress/b-done/b-canceled` (заказы), `b-draft/b-published` (стихи).
- Первая загрузка любой страницы админки (HTML+CSS) ≈ 5 КБ — помещается в первые TCP-пакеты.

При добавлении страницы переиспользуй существующие классы, не вводи новые цвета мимо CSS-переменных.

## Кэш

- `CacheConfig` объявляет `ConcurrentMapCacheManager("siteSettings", "contactLinks")` — в Spring Boot 4 cache-автоконфигурация в отдельном стартере, поэтому бин объявлен явно.
- `@Cacheable` на чтении, `@CacheEvict(allEntries=true)` на изменении.
- **Грабли:** не вызывай закэшированный метод из другого метода того же бина (`this.get()`) — самовызов минует прокси и кэш/evict не сработают. В `SiteSettingsService.update()` сущность для записи читается прямо из репозитория именно поэтому.

## Как добавить публичную страницу

1. Метод в `PublicPageController` (или новом контроллере пакета `public_`), верни имя шаблона.
2. Шаблон в `templates/public/`, оберни в layout: `th:replace="~{layouts/public :: layout(~{::content})}"`.
3. **Добавь путь в `SecurityConfig`** в permit-list — иначе `denyAll` отправит на логин.
4. Нужны общие данные (`settings`, `contactLinks`)? Они уже в модели через `GlobalModelAttributes`.

## Как добавить поле в сущность

1. Новая миграция `V<n+1>__описание.sql` — `ALTER TABLE ... ADD COLUMN ...` (без `IF NOT EXISTS`, `timestamptz` для времени).
2. Поле + геттер/сеттер в сущности.
3. Если поле редактируется — добавь в форму/DTO, шаблон и метод сервиса (для админских сущностных форм проверь, что копируешь только нужные поля).
4. Не редактируй уже применённые миграции — Flyway упадёт по чексумме.

## Как добавить админский раздел

1. Контроллер в `web/admin/`, `@RequestMapping("/admin/...")`. Доступ к `/admin/**` уже под `hasRole("ADMIN")`.
2. Сервис + репозиторий по образцу существующих.
3. Шаблоны в `templates/admin/`, layout `layouts/admin.html`, добавь пункт в `<nav>`.
4. Списки — пагинируй: верни `Page<T>` из сервиса, в шаблоне подключи `fragments/pager.html`:
   ```html
   <nav th:replace="~{fragments/pager :: pager(${page}, '/admin/твой-путь')}"></nav>
   ```
5. Формы — CSRF-поле добавится автоматически при `th:action`; для надёжности можно прописать скрытый `_csrf` вручную (как в существующих).

## Известный технический долг

- **`SiteSettings` тащит легаси-контакты** (`telegram/phone/email/social`) — публичка перешла на `contact_link`, поля можно вычистить отдельной миграцией + правкой формы.
- **`siteName`/`footerNote`** в `GlobalModelAttributes` оба равны `heroTitle` — приемлемо (это и есть имя сайта), но при желании развести по отдельным настройкам. Имя автора уже вынесено в свойство `app.author-name`.

Историю крупных правок и обоснования см. в [03-architecture.md](03-architecture.md).
