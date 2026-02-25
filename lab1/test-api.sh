#!/bin/bash
# ============================================================
# Скрипт тестирования REST API — Order Management BPMN Process
# ============================================================
#
# Этот скрипт проходит полный бизнес-процесс по BPMN-диаграмме:
#   Заказчик создаёт заказ → Продавец проверяет → Готовит → Собирает →
#   Ищет курьера → Курьер принимает → Курьер приходит → В доставке
#
# Также тестируются: отмена заказа, уведомления, фильтрация по статусу.
#
# Использование:
#   chmod +x test-api.sh
#   ./test-api.sh
#
# Требования: curl, jq (опционально, для форматирования JSON)
# ============================================================

BASE_URL="http://localhost:8080/api"

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Счётчик шагов
STEP=0

print_header() {
    echo ""
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}============================================================${NC}"
}

print_step() {
    STEP=$((STEP + 1))
    echo ""
    echo -e "${YELLOW}--- Шаг $STEP: $1 ---${NC}"
}

# ============================================================
print_header "ЧАСТЬ 1: СОЗДАНИЕ УЧАСТНИКОВ"
# ============================================================

# --- Создание заказчиков ---
print_step "Создать заказчика #1"
curl -s -X POST "$BASE_URL/customers" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Иван Иванов",
        "email": "ivan@mail.ru",
        "phone": "+79001234567"
    }' | { command -v jq &>/dev/null && jq . || cat; }

print_step "Создать заказчика #2"
curl -s -X POST "$BASE_URL/customers" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Мария Петрова",
        "email": "maria@gmail.com",
        "phone": "+79009876543"
    }' | { command -v jq &>/dev/null && jq . || cat; }

# --- Создание продавцов ---
print_step "Создать продавца #1"
curl -s -X POST "$BASE_URL/sellers" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Пиццерия Марио",
        "address": "ул. Ленина, 42"
    }' | { command -v jq &>/dev/null && jq . || cat; }

print_step "Создать продавца #2"
curl -s -X POST "$BASE_URL/sellers" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Суши-бар Токио",
        "address": "пр. Невский, 100"
    }' | { command -v jq &>/dev/null && jq . || cat; }

# --- Создание курьеров ---
print_step "Создать курьера #1"
curl -s -X POST "$BASE_URL/couriers" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Алексей Доставкин",
        "phone": "+79005551111"
    }' | { command -v jq &>/dev/null && jq . || cat; }

print_step "Создать курьера #2"
curl -s -X POST "$BASE_URL/couriers" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "Дмитрий Быстров",
        "phone": "+79005552222"
    }' | { command -v jq &>/dev/null && jq . || cat; }

# --- Получение списков ---
print_step "Получить всех заказчиков"
curl -s "$BASE_URL/customers" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Получить заказчика по ID"
curl -s "$BASE_URL/customers/1" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Получить всех продавцов"
curl -s "$BASE_URL/sellers" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Получить продавца по ID"
curl -s "$BASE_URL/sellers/1" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Получить всех курьеров"
curl -s "$BASE_URL/couriers" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Получить курьера по ID"
curl -s "$BASE_URL/couriers/1" | { command -v jq &>/dev/null && jq . || cat; }

# ============================================================
print_header "ЧАСТЬ 2: УСПЕШНЫЙ БИЗНЕС-ПРОЦЕСС (по BPMN)"
# ============================================================

# Шаг BPMN: Заказчик → "Создать заказ"
print_step "BPMN: Заказчик создаёт заказ → статус IN_PROCESSING"
curl -s -X POST "$BASE_URL/orders" \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": 1,
        "sellerId": 1,
        "items": [
            {"productName": "Пицца Маргарита", "quantity": 2, "price": 599.00},
            {"productName": "Кола 0.5л", "quantity": 2, "price": 99.00}
        ]
    }' | { command -v jq &>/dev/null && jq . || cat; }

# Проверим уведомления продавца (должно быть уведомление о новом заказе)
print_step "Проверить уведомления продавца #1 (новый заказ)"
curl -s "$BASE_URL/notifications/SELLER/1" | { command -v jq &>/dev/null && jq . || cat; }

# Проверим уведомления заказчика (статус: В обработке)
print_step "Проверить уведомления заказчика #1 (статус В обработке)"
curl -s "$BASE_URL/notifications/CUSTOMER/1" | { command -v jq &>/dev/null && jq . || cat; }

# Шаг BPMN: Продавец → "Проверить заказ" → Да → "Передать на приготовление"
print_step "BPMN: Продавец принимает заказ (canFulfill=true) → статус COOKING"
curl -s -X POST "$BASE_URL/orders/1/review" \
    -H "Content-Type: application/json" \
    -d '{
        "canFulfill": true
    }' | { command -v jq &>/dev/null && jq . || cat; }

# Проверим статус заказа
print_step "Проверить статус заказа #1"
curl -s "$BASE_URL/orders/1" | { command -v jq &>/dev/null && jq . || cat; }

# Шаг BPMN: Продавец → "Собрать заказ"
print_step "BPMN: Продавец собирает заказ → статус ASSEMBLING"
curl -s -X POST "$BASE_URL/orders/1/assemble" | { command -v jq &>/dev/null && jq . || cat; }

# Шаг BPMN: Продавец → "Искать курьера"
print_step "BPMN: Продавец ищет курьера → статус AWAITING_COURIER"
curl -s -X POST "$BASE_URL/orders/1/search-courier" | { command -v jq &>/dev/null && jq . || cat; }

# Проверим уведомления курьера (должно быть уведомление о новом заказе)
print_step "Проверить уведомления курьера #1 (новый заказ на доставку)"
curl -s "$BASE_URL/notifications/COURIER/1" | { command -v jq &>/dev/null && jq . || cat; }

# Шаг BPMN: Курьер → "Принял запрос на доставку"
print_step "BPMN: Курьер #1 принимает доставку заказа #1"
curl -s -X POST "$BASE_URL/orders/1/courier/1/accept" | { command -v jq &>/dev/null && jq . || cat; }

# Шаг BPMN: Курьер → "Курьер пришёл в заведение" → Продавец → "Передать заказ курьеру"
print_step "BPMN: Курьер #1 пришёл в заведение → статус IN_DELIVERY"
curl -s -X POST "$BASE_URL/orders/1/courier/1/arrived" | { command -v jq &>/dev/null && jq . || cat; }

# Проверим все уведомления заказчика (полная цепочка статусов)
print_step "Все уведомления заказчика #1 (полная цепочка)"
curl -s "$BASE_URL/notifications/CUSTOMER/1" | { command -v jq &>/dev/null && jq . || cat; }

# ============================================================
print_header "ЧАСТЬ 3: СЦЕНАРИЙ ОТМЕНЫ ЗАКАЗА ПРОДАВЦОМ"
# ============================================================

# Создать второй заказ
print_step "Создать заказ #2 (для отмены)"
curl -s -X POST "$BASE_URL/orders" \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": 2,
        "sellerId": 2,
        "items": [
            {"productName": "Ролл Филадельфия", "quantity": 1, "price": 450.00},
            {"productName": "Мисо суп", "quantity": 1, "price": 250.00}
        ]
    }' | { command -v jq &>/dev/null && jq . || cat; }

# Шаг BPMN: Продавец → "Проверить заказ" → Нет → "Отменить заказ"
print_step "BPMN: Продавец отклоняет заказ (canFulfill=false) → статус CANCELLED"
curl -s -X POST "$BASE_URL/orders/2/review" \
    -H "Content-Type: application/json" \
    -d '{
        "canFulfill": false,
        "cancelReason": "Нет нужных ингредиентов"
    }' | { command -v jq &>/dev/null && jq . || cat; }

# Уведомления заказчика #2 (должно быть: В обработке, Отменён)
print_step "Уведомления заказчика #2 (заказ отменён)"
curl -s "$BASE_URL/notifications/CUSTOMER/2" | { command -v jq &>/dev/null && jq . || cat; }

# ============================================================
print_header "ЧАСТЬ 4: ФИЛЬТРАЦИЯ И ПОИСК"
# ============================================================

print_step "Получить все заказы"
curl -s "$BASE_URL/orders" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Фильтр по статусу: IN_DELIVERY"
curl -s "$BASE_URL/orders/status/IN_DELIVERY" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Фильтр по статусу: CANCELLED"
curl -s "$BASE_URL/orders/status/CANCELLED" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Заказы покупателя #1"
curl -s "$BASE_URL/orders/customer/1" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Заказы продавца #1"
curl -s "$BASE_URL/orders/seller/1" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Заказы курьера #1"
curl -s "$BASE_URL/orders/courier/1" | { command -v jq &>/dev/null && jq . || cat; }

# ============================================================
print_header "ЧАСТЬ 5: РАБОТА С УВЕДОМЛЕНИЯМИ"
# ============================================================

print_step "Непрочитанные уведомления заказчика #1"
curl -s "$BASE_URL/notifications/CUSTOMER/1/unread" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Отметить уведомление #1 как прочитанное"
curl -s -X POST "$BASE_URL/notifications/1/read"
echo " (done)"

print_step "Непрочитанные уведомления заказчика #1 (после прочтения)"
curl -s "$BASE_URL/notifications/CUSTOMER/1/unread" | { command -v jq &>/dev/null && jq . || cat; }

# ============================================================
print_header "ЧАСТЬ 6: ВАЛИДАЦИЯ (ожидаемые ошибки)"
# ============================================================

print_step "Создать заказ без позиций (ошибка валидации 400)"
curl -s -X POST "$BASE_URL/orders" \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": 1,
        "sellerId": 1,
        "items": []
    }' | { command -v jq &>/dev/null && jq . || cat; }

print_step "Создать заказ с несуществующим покупателем (ошибка 404)"
curl -s -X POST "$BASE_URL/orders" \
    -H "Content-Type: application/json" \
    -d '{
        "customerId": 999,
        "sellerId": 1,
        "items": [{"productName": "Тест", "quantity": 1, "price": 100}]
    }' | { command -v jq &>/dev/null && jq . || cat; }

print_step "Попытка собрать заказ в неправильном статусе (ошибка 409)"
curl -s -X POST "$BASE_URL/orders/1/assemble" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Получить несуществующий заказ (ошибка 404)"
curl -s "$BASE_URL/orders/999" | { command -v jq &>/dev/null && jq . || cat; }

print_step "Курьер пытается принять чужой заказ (ошибка 409)"
curl -s -X POST "$BASE_URL/orders/1/courier/2/accept" | { command -v jq &>/dev/null && jq . || cat; }

# ============================================================
print_header "ТЕСТИРОВАНИЕ ЗАВЕРШЕНО"
# ============================================================
echo ""
echo -e "${GREEN}Все запросы выполнены.${NC}"
echo "Итого шагов: $STEP"
echo ""
