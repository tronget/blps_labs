package ru.itmo.ordermanagement.dto;

import lombok.Data;

/**
 * DTO для решения продавца по заказу ("Возможно ли выполнить заказ?").
 */
@Data
public class ReviewOrderRequest {

    /** true = заказ можно выполнить, false = отменить */
    private boolean canFulfill;

    /** Причина отмены (если canFulfill = false) */
    private String cancelReason;
}
