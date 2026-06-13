# Тестирование

## Текущее состояние

47 автотестов, все зелёные. Покрыты: юнит-логика, репозитории/сервисы и web-слой (MockMvc + Security) на реальном Postgres.

### Как запускать

**С Docker (CI, по умолчанию)** — тесты сами поднимают Postgres через Testcontainers:

```bash
./mvnw test
```

**Без Docker** — против внешнего Postgres (флаг `-Dtest.db=local` или env `TEST_DB=local`):

```bash
# одноразово создать пустую БД
createdb poemorder_test --owner=poems
# прогнать
TEST_DB=local ./mvnw test
# параметры БД переопределяются: -Dtest.db.url=... -Dtest.db.username=... -Dtest.db.password=...
```

Выбор режима — в `AbstractIntegrationTest`: по умолчанию `PostgreSQLContainer`, при `test.db=local` — внешняя БД. В обоих случаях Flyway применяет миграции.

### Состав

| Класс | Тип | Что проверяет |
|---|---|---|
| `PoemorderApplicationTests` | smoke | контекст + Security + Flyway поднимаются |
| `TelegramMessageFormatterTest` | unit | форматирование сообщений, обрезка, пропуск пустых полей |
| `RateLimitFilterTest` | unit | лимит/окно/per-IP/только нужные пути, на spring-test mock'ах |
| `OrderServiceTest` | integration | create+trim, смена статуса, 404, пагинация |
| `ReviewServiceTest` | integration | PENDING, срез `@`, approve→APPROVED, hideContact, лимит |
| `PoemServiceTest` | integration | защита id/createdAt, publishedAll/forHomepage, update |
| `ContactLinkServiceTest` | integration | разрешённые схемы href, отказ `javascript:`, фильтр+сортировка |
| `SiteSettingsServiceTest` | integration | singleton, обновление полей, blank→null |
| `CacheEvictionTest` | integration | кэш реально кэширует и сбрасывается при изменении через сервис |
| `PublicWebTest` | web/MockMvc | 200 на страницах, order/review submit, honeypot, CSRF 403, валидация, rate limit 429 |
| `AdminWebTest` | web/MockMvc | анон→login, ADMIN-доступ, dropdown статусов, смена статуса, CSRF |

Изоляция: мутирующие тесты помечены `@Transactional` (откат после метода); `CacheEvictionTest` не транзакционный и сам чистит данные/кэш; web-тесты используют разные `X-Forwarded-For`, чтобы счётчики rate limit не пересекались между методами.

## Ручная проверка (smoke через curl)

Пока нет автотестов — это воспроизводимый чек-лист. Все пункты прогонялись на работающем приложении.

```bash
BASE=http://localhost:8081

# health
curl -s $BASE/actuator/health        # {"status":"UP"}

# публичные страницы → 200
for p in / /portfolio /pricing /reviews /contacts /order; do
  echo "$p $(curl -s -o /dev/null -w '%{http_code}' $BASE$p)"
done

# denyAll: неизвестный путь и админка без логина → редирект на /admin/login
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" $BASE/whatever
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" $BASE/admin/orders

# POST без CSRF → 403
curl -s -o /dev/null -w "%{http_code}\n" -X POST $BASE/order -d "name=x&phone=1&description=y"
```

Отправка формы с CSRF (нужно достать токен из HTML и сохранить cookie):

```bash
JAR=/tmp/cj.txt; rm -f $JAR
TOKEN=$(curl -s -c $JAR $BASE/order | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"$//')
# валидная заявка → 302
curl -s -b $JAR -o /dev/null -w "%{http_code} %{redirect_url}\n" -X POST $BASE/order \
  --data-urlencode "_csrf=$TOKEN" --data-urlencode "name=Тест" \
  --data-urlencode "phone=+79990001122" --data-urlencode "description=Хочу стих"
# honeypot → 302 "успех", записи нет
curl -s -b $JAR -o /dev/null -w "%{http_code}\n" -X POST $BASE/order \
  --data-urlencode "_csrf=$TOKEN" --data-urlencode "name=Bot" \
  --data-urlencode "phone=1" --data-urlencode "description=spam" \
  --data-urlencode "website=spam.example"
```

Проверенные сценарии (✅ — подтверждено на запущенном приложении):

| Сценарий | Ожидание | |
|---|---|---|
| Публичные страницы | 200 | ✅ |
| denyAll / админка без логина | редирект на логин | ✅ |
| POST без CSRF | 403 | ✅ |
| Валидная заявка | 302 + запись в БД, кириллица ок | ✅ |
| Honeypot | «успех», в БД пусто | ✅ |
| Rate limit | 429 после 5 за окно | ✅ |
| Логин + все 7 страниц админки | 200 | ✅ |
| Создание стиха → портфолио | виден | ✅ |
| `javascript:` в href контакта | отклонён, 0 строк в БД | ✅ |
| Смена настроек → главная | обновилась сразу (кэш сброшен) | ✅ |
| Пагинатор (29 заказов) | «1 из 2», 20+9 | ✅ |
| Отзыв → модерация → одобрение → публикация | полный цикл | ✅ |
| Миграция V10 | таблицы удалены, типы времени timestamptz | ✅ |
| Смена статуса заказа (после фикса `allStatuses`→`statuses`) | dropdown из 4 опций, NEW→IN_PROGRESS пишется в БД | ✅ |

## Закрытые регрессии

Реальные баги, найденные при ревью/документировании, теперь под автотестами:

- `order-view.html` читал `${allStatuses}` вместо `${statuses}` (пустой dropdown) — `AdminWebTest.orderView_rendersAllStatusOptions` + `changeStatus_persistsNewStatus`.
- Старт приложения требует `app.admins` (Security) — проверяется любым интеграционным тестом через профиль `test`.
- `javascript:` в href контакта → отказ — `ContactLinkServiceTest.upsert_rejectsJavascriptScheme`.

Не покрыто автотестами (низкий приоритет): шаблонные опечатки вида `${items}`/`${poems}` (ловятся web-тестом только при проверке контента страницы), дубль миграции при незачищенном `target/classes` (среда сборки).

## Идеи на будущее

- `@DataJpaTest`-срезы вместо полного контекста для репозиториев — быстрее, но в этом проекте контекст лёгкий, разница невелика.
- Тест рендера письма об ошибке/страниц 404/500.
- Покрыть публичную страницу `/portfolio/{id}` для несуществующего id.
