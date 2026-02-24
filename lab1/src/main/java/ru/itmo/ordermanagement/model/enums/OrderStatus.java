package ru.itmo.ordermanagement.model.enums;

/**
 * Статусы заказа в соответствии с BPMN-диаграммой.
 *
 * Жизненный цикл заказа:
 * CREATED → IN_PROCESSING → COOKING → ASSEMBLING → SEARCHING_COURIER → AWAITING_COURIER → IN_DELIVERY
 *                         ↘ CANCELLED
 * Также возможен переход в DELAYED (задержка курьера).
 */
public enum OrderStatus {
    /** Заказ создан заказчиком (товары собраны в корзине) */
    CREATED,

    /** Заказ в обработке у продавца (продавец проверяет заказ) */
    IN_PROCESSING,

    /** Заказ передан на приготовление */
    COOKING,

    /** Заказ собирается (сборка) */
    ASSEMBLING,

    /** Поиск курьера */
    SEARCHING_COURIER,

    /** Ожидание прихода курьера в заведение */
    AWAITING_COURIER,

    /** Заказ задерживается (курьер не пришёл к назначенному времени) */
    DELAYED,

    /** Заказ передан курьеру, в доставке */
    IN_DELIVERY,

    /** Заказ отменён */
    CANCELLED
}
