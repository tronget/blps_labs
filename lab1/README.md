# Order Management — BPMN Business Process Implementation

## Описание

Spring Boot приложение, реализующее бизнес-процесс **"Работа с интерфейсом продавца — управление заказами"** в соответствии с BPMN 2.0 диаграммой.

## Бизнес-процесс (BPMN)

### Участники (пулы)

| Пул | Описание |
|-----|----------|
| **Заказчик** | Создаёт заказ, получает уведомления о статусах |
| **Продавец** | Проверяет, готовит, собирает заказ, ищет курьера |
| **Курьер (Yandex Delivery)** | Принимает доставку, приходит в заведение |

### Жизненный цикл заказа

```
CREATED → IN_PROCESSING → COOKING → ASSEMBLING → SEARCHING_COURIER → AWAITING_COURIER → IN_DELIVERY
                        ↘ CANCELLED (продавец отклонил или таймаут 10 мин)
                                                                      ↘ DELAYED (курьер опаздывает)
```

### Шаги бизнес-процесса

1. **Заказчик** собирает товары в корзину → **Создать заказ** → статус `IN_PROCESSING`
2. **Продавец** получает уведомление → **Проверить заказ** → решение: "Возможно ли выполнить?"
   - **Да** → **Передать на приготовление** → статус `COOKING`
   - **Нет** → **Отменить заказ** → статус `CANCELLED`
3. **Продавец** → **Собрать заказ** → статус `ASSEMBLING`
4. **Продавец** → **Искать курьера** → статус `SEARCHING_COURIER` / `AWAITING_COURIER`
5. **Курьер** получает уведомление → **Принял запрос на доставку**
6. **Курьер** → **Пришёл в заведение** → **Продавец передаёт заказ курьеру** → статус `IN_DELIVERY`

### Таймеры (Timer Events)

- ⏱ **Продавец не реагирует 10 минут** → заказ автоматически отменяется
- ⏱ **Курьер не пришёл к назначенному времени** → статус `DELAYED`

## Технологии

- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- PostgreSQL
- SpringDoc OpenAPI (Swagger UI)
- Lombok

## Запуск

### 1. Подготовка БД

```bash
# Создать БД PostgreSQL
createdb order_management

# Применить схему
psql -d order_management -f src/main/resources/schema.sql
```

### 2. Настройка подключения

Отредактируйте `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/order_management
    username: postgres
    password: postgres
```

### 3. Сборка и запуск

```bash
./mvnw clean package -DskipTests
java -jar target/order-management-0.0.1-SNAPSHOT.jar
```

Или через Maven:

```bash
./mvnw spring-boot:run
```

### 4. Swagger UI

После запуска: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## REST API

### Заказчики `/api/customers`

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/customers` | Создать заказчика |
| GET | `/api/customers` | Получить всех |
| GET | `/api/customers/{id}` | Получить по ID |

### Продавцы `/api/sellers`

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/sellers` | Создать продавца |
| GET | `/api/sellers` | Получить всех |
| GET | `/api/sellers/{id}` | Получить по ID |

### Курьеры `/api/couriers`

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/couriers` | Создать курьера |
| GET | `/api/couriers` | Получить всех |
| GET | `/api/couriers/{id}` | Получить по ID |

### Заказы `/api/orders`

| Метод | URL | BPMN шаг | Описание |
|-------|-----|----------|----------|
| POST | `/api/orders` | Создать заказ | Заказчик создаёт заказ |
| POST | `/api/orders/{id}/review` | Проверить заказ | Продавец принимает/отклоняет |
| POST | `/api/orders/{id}/assemble` | Собрать заказ | Продавец собирает |
| POST | `/api/orders/{id}/search-courier` | Искать курьера | Продавец ищет курьера |
| POST | `/api/orders/{id}/courier/{cId}/accept` | Принял доставку | Курьер принимает |
| POST | `/api/orders/{id}/courier/{cId}/arrived` | Пришёл в заведение | Курьер пришёл |
| GET | `/api/orders/{id}` | — | Получить заказ |
| GET | `/api/orders` | — | Все заказы |
| GET | `/api/orders/status/{status}` | — | По статусу |
| GET | `/api/orders/customer/{id}` | — | Заказы покупателя |
| GET | `/api/orders/seller/{id}` | — | Заказы продавца |
| GET | `/api/orders/courier/{id}` | — | Заказы курьера |

### Уведомления `/api/notifications`

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/api/notifications/{type}/{id}` | Все уведомления |
| GET | `/api/notifications/{type}/{id}/unread` | Непрочитанные |
| POST | `/api/notifications/{id}/read` | Отметить прочитанным |

## Пример сценария (curl)

```bash
# 1. Создать участников
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name": "Иван Иванов", "email": "ivan@mail.ru", "phone": "+79001234567"}'

curl -X POST http://localhost:8080/api/sellers \
  -H "Content-Type: application/json" \
  -d '{"name": "Пиццерия Марио", "address": "ул. Ленина, 42"}'

curl -X POST http://localhost:8080/api/couriers \
  -H "Content-Type: application/json" \
  -d '{"name": "Курьер Алексей", "phone": "+79009876543"}'

# 2. Создать заказ (→ IN_PROCESSING)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1, "sellerId": 1, "items": [{"productName": "Пицца Маргарита", "quantity": 2, "price": 599.00}]}'

# 3. Продавец принимает заказ (→ COOKING)
curl -X POST http://localhost:8080/api/orders/1/review \
  -H "Content-Type: application/json" \
  -d '{"canFulfill": true}'

# 4. Собрать заказ (→ ASSEMBLING)
curl -X POST http://localhost:8080/api/orders/1/assemble

# 5. Искать курьера (→ AWAITING_COURIER)
curl -X POST http://localhost:8080/api/orders/1/search-courier

# 6. Курьер принимает
curl -X POST http://localhost:8080/api/orders/1/courier/1/accept

# 7. Курьер пришёл (→ IN_DELIVERY)
curl -X POST http://localhost:8080/api/orders/1/courier/1/arrived

# Проверить уведомления покупателя
curl http://localhost:8080/api/notifications/CUSTOMER/1
```
