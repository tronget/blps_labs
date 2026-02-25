package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimerScheduler {

    private final OrderService orderService;

    @Value("${app.seller-reaction-timeout-minutes:10}")
    private int sellerTimeoutMinutes;

    @Value("${app.courier-arrival-timeout-minutes:30}")
    private int courierTimeoutMinutes;

    @Scheduled(fixedRate = 60_000)
    public void checkSellerTimeout() {
        log.debug("Checking for seller reaction timeout ({} min)...", sellerTimeoutMinutes);
        orderService.cancelOverdueOrders(sellerTimeoutMinutes);
    }

    @Scheduled(fixedRate = 60_000)
    public void checkCourierTimeout() {
        log.debug("Checking for courier arrival timeout ({} min)...", courierTimeoutMinutes);
        orderService.markDelayedOrders(courierTimeoutMinutes);
    }
}
