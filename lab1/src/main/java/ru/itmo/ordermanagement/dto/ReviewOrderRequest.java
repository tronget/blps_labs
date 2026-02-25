package ru.itmo.ordermanagement.dto;

import lombok.Data;

@Data
public class ReviewOrderRequest {

    private boolean canFulfill;

    private String cancelReason;
}
