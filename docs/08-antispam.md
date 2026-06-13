# Антиспам

Публичные формы (`POST /order`, `POST /reviews`) защищены двумя независимыми механизмами.

## 1. Rate limiting

Реализация: `web/RateLimitFilter.java` — `OncePerRequestFilter`, срабатывает только на `POST` к `/order` и `/reviews` (см. `shouldNotFilter`).

### Как работает

- Ключ — IP клиента. За reverse proxy берётся **первый** адрес из заголовка `X-Forwarded-For`; если заголовка нет — `request.getRemoteAddr()`.
- Для каждого IP — счётчик в скользящем окне (`WindowCounter`: момент старта окна + `AtomicInteger`).
- Лимит: не более `RATE_LIMIT_MAX` (по умолчанию **5**) отправок за `RATE_LIMIT_WINDOW_MINUTES` (по умолчанию **10**) минут.
- Превышение → ответ `429 Too Many Requests` с текстом «Слишком много запросов. Попробуй ещё раз позже.» (UTF-8). Запрос до контроллера не доходит.
- Окно «сбрасывается» лениво: первый запрос после истечения окна начинает новое.

### Защита от переполнения памяти

Счётчики живут в `ConcurrentHashMap`. Чтобы спам с тысяч IP не съел память, при превышении `MAX_TRACKED_IPS` (10 000 записей) карта чистится от протухших окон. Для одного инстанса этого достаточно.

### Настройка

```properties
# application.properties / переменные окружения
app.rate-limit.max-requests=${RATE_LIMIT_MAX:5}
app.rate-limit.window-minutes=${RATE_LIMIT_WINDOW_MINUTES:10}
```

Поднять лимит на время акции — увеличь `RATE_LIMIT_MAX`. Сделать строже — уменьши и/или увеличь окно.

### Важно про доверие к `X-Forwarded-For`

Заголовок легко подделать, если приложение торчит в интернет напрямую. Поэтому:
- Приложение должно быть **за reverse proxy** и слушать `127.0.0.1` (так и настроено в `docker-compose.yml`).
- Proxy обязан **перезаписывать** `X-Forwarded-For` реальным адресом клиента (nginx `proxy_set_header X-Forwarded-For $remote_addr;` или `$proxy_add_x_forwarded_for`).

Без доверенного proxy злоумышленник сможет обходить лимит, меняя заголовок. См. [10-deployment.md](10-deployment.md).

### Ограничение

Счётчики в памяти процесса — при рестарте сбрасываются, между несколькими инстансами не разделяются. Для горизонтального масштабирования нужен общий стор (Redis + bucket4j или аналог).

## 2. Honeypot

Скрытое поле `website` в обеих формах. Реальный пользователь его не видит и не заполняет; примитивные боты заполняют все поля подряд.

Логика (в `OrderController` / `ReviewPublicController`):

```java
if (form.getWebsite() != null && !form.getWebsite().isBlank()) {
    // притворяемся успехом, ничего не сохраняем
    ra.addFlashAttribute(...);  // "Заявка отправлена!"
    return "redirect:/order";
}
```

Боту показывается обычное сообщение об успехе — он не понимает, что попал в ловушку, и не подстраивается. В БД ничего не пишется.

> Поле `website` есть и в DTO (`OrderForm.website`, `ReviewForm.website`), и в шаблонах форм. При вёрстке его прячут (visually-hidden), но **не** через `type="hidden"` — иначе бот его проигнорирует; обычно это видимый для DOM, но скрытый стилями input.

## Чего нет

- **CAPTCHA** — сознательно: для личного сайта связка rate limit + honeypot отсекает основной мусор, а reCAPTCHA ухудшает UX и тянет внешнюю зависимость. Если спам прорвётся — это первый кандидат на добавление.
- **Чёрные списки IP/слов** — не реализованы.

## Проверка работы

Воспроизводится curl'ом (см. также [12-testing.md](12-testing.md)):

```bash
# honeypot: запись не создаётся, ответ 302 "успех"
curl -i -X POST .../order --data-urlencode "website=spam" ...

# rate limit: 6-я отправка за окно → 429
for i in $(seq 1 6); do curl -s -o /dev/null -w "%{http_code}\n" -X POST .../order ...; done
```
