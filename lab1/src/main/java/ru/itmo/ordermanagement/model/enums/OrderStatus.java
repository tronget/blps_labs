package ru.itmo.ordermanagement.model.enums;

public enum OrderStatus {
    CREATED,
    IN_PROCESSING,
    COOKING,
    ASSEMBLING,
    SEARCHING_COURIER,
    AWAITING_COURIER,
    DELAYED,
    IN_DELIVERY,
    CANCELLED
}
