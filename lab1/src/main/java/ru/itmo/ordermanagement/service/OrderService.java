package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ordermanagement.dto.*;
import ru.itmo.ordermanagement.exception.InvalidOrderStateException;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.*;
import ru.itmo.ordermanagement.model.enums.OrderStatus;
import ru.itmo.ordermanagement.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Основной сервис бизнес-процесса управления заказами.
 *
 * Реализует BPMN-диаграмму:
 *
 * 1. Заказчик: Создать заказ (CREATED)
 * 2. Продавец получает уведомление → Проверить заказ (IN_PROCESSING)
 * 3. Продавец: Возможно ли выполнить?
 *    - Да → Передать на приготовление (COOKING)
 *    - Нет → Отменить заказ (CANCELLED)
 * 4. Продавец: Собрать заказ (ASSEMBLING)
 * 5. Продавец: Искать курьера (SEARCHING_COURIER)
 * 6. Курьер: Принял запрос на доставку (AWAITING_COURIER)
 * 7. Курьер: Пришёл в заведение → Передать заказ курьеру (IN_DELIVERY)
 *
 * Таймеры:
 * - Продавец не реагирует 10 минут → авто-отмена
 * - Курьер не пришёл к назначенному времени → статус DELAYED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final CourierRepository courierRepository;
    private final NotificationService notificationService;

    // ==================== ЗАКАЗЧИК ====================

    /**
     * Шаг 1 BPMN: Заказчик → "Создать заказ".
     * Товары собраны в корзине покупателя → создаётся заказ.
     * Продавец получает уведомление.
     * Заказчик получает уведомление: "Изменён статус заказа: В обработке".
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found: " + request.getCustomerId()));

        Seller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller not found: " + request.getSellerId()));

        Order order = Order.builder()
                .customer(customer)
                .seller(seller)
                .status(OrderStatus.IN_PROCESSING)
                .build();

        // Добавить позиции заказа
        for (OrderItemDto itemDto : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productName(itemDto.getProductName())
                    .quantity(itemDto.getQuantity())
                    .price(itemDto.getPrice())
                    .build();
            order.addItem(item);
        }
        order.recalculateTotal();
        order.setSellerNotifiedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        // Уведомление продавцу о новом заказе
        notificationService.notifySellerNewOrder(order);

        // Уведомление заказчику: статус "В обработке"
        notificationService.notifyCustomerStatusChanged(order);

        log.info("Order #{} created, status: IN_PROCESSING", order.getId());
        return toResponse(order);
    }

    // ==================== ПРОДАВЕЦ ====================

    /**
     * Шаг 2-3 BPMN: Продавец → "Проверить заказ" → "Возможно ли выполнить заказ?"
     *
     * Если да (canFulfill=true):
     *   → Передать заказ на приготовление (COOKING)
     *   → Заказчику уведомление: "Готовится"
     *
     * Если нет (canFulfill=false):
     *   → Отменить заказ (CANCELLED)
     *   → Заказчику уведомление: "Заказ отменён"
     */
    @Transactional
    public OrderResponse reviewOrder(Long orderId, ReviewOrderRequest request) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.IN_PROCESSING);

        if (request.isCanFulfill()) {
            // Да → Передать заказ на приготовление
            order.setStatus(OrderStatus.COOKING);
            order = orderRepository.save(order);
            notificationService.notifyCustomerStatusChanged(order);
            log.info("Order #{} accepted by seller, status: COOKING", orderId);
        } else {
            // Нет → Отменить заказ
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason(request.getCancelReason() != null
                    ? request.getCancelReason()
                    : "Продавец не может выполнить заказ");
            order = orderRepository.save(order);
            notificationService.notifyCustomerStatusChanged(order);
            log.info("Order #{} cancelled by seller: {}", orderId, order.getCancelReason());
        }

        return toResponse(order);
    }

    /**
     * Шаг 4 BPMN: Продавец → "Собрать заказ".
     * COOKING → ASSEMBLING.
     * Заказчику уведомление: "В сборке".
     */
    @Transactional
    public OrderResponse assembleOrder(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.COOKING);

        order.setStatus(OrderStatus.ASSEMBLING);
        order = orderRepository.save(order);

        notificationService.notifyCustomerStatusChanged(order);
        log.info("Order #{} assembled, status: ASSEMBLING", orderId);
        return toResponse(order);
    }

    /**
     * Шаг 5 BPMN: Продавец → "Искать курьера".
     * ASSEMBLING → SEARCHING_COURIER.
     * Ищем свободного курьера, если находим — назначаем и уведомляем.
     */
    @Transactional
    public OrderResponse searchCourier(Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.ASSEMBLING);

        order.setStatus(OrderStatus.SEARCHING_COURIER);
        order = orderRepository.save(order);

        // Попробовать найти свободного курьера
        final Order savedOrder = order;
        courierRepository.findFirstByAvailableTrue().ifPresent(courier -> {
            assignCourier(savedOrder, courier);
        });

        return toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    /**
     * Назначить курьера на заказ (внутренний метод).
     * SEARCHING_COURIER → AWAITING_COURIER.
     * Курьеру уведомление: "Уведомление о новом заказе".
     */
    @Transactional
    public void assignCourier(Order order, Courier courier) {
        courier.setAvailable(false);
        courierRepository.save(courier);

        order.setCourier(courier);
        order.setStatus(OrderStatus.AWAITING_COURIER);
        order.setCourierAssignedAt(LocalDateTime.now());
        order.setCourierNotifiedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Уведомление курьеру
        notificationService.notifyCourierNewDelivery(order);
        log.info("Order #{}: courier #{} assigned, status: AWAITING_COURIER",
                order.getId(), courier.getId());
    }

    // ==================== КУРЬЕР ====================

    /**
     * Шаг 6 BPMN: Курьер → "Принял запрос на доставку".
     * Это подтверждение от курьера (уже назначен, просто подтверждает).
     */
    @Transactional
    public OrderResponse courierAcceptDelivery(Long orderId, Long courierId) {
        Order order = findOrderOrThrow(orderId);
        assertStatus(order, OrderStatus.AWAITING_COURIER);

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new InvalidOrderStateException(
                    "Courier #" + courierId + " is not assigned to order #" + orderId);
        }

        // Курьер подтвердил — статус не меняется, ждём прихода
        log.info("Order #{}: courier #{} accepted delivery request", orderId, courierId);
        return toResponse(order);
    }

    /**
     * Шаг 7 BPMN: Курьер → "Курьер пришёл в заведение".
     * Продавец → "Передать заказ курьеру".
     * AWAITING_COURIER → IN_DELIVERY.
     * Заказчику уведомление: "В доставке".
     */
    @Transactional
    public OrderResponse courierArrived(Long orderId, Long courierId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.AWAITING_COURIER
                && order.getStatus() != OrderStatus.DELAYED) {
            throw new InvalidOrderStateException(
                    "Order #" + orderId + " is not in AWAITING_COURIER or DELAYED status");
        }

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new InvalidOrderStateException(
                    "Courier #" + courierId + " is not assigned to order #" + orderId);
        }

        order.setCourierArrivedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.IN_DELIVERY);
        order = orderRepository.save(order);

        notificationService.notifyCustomerStatusChanged(order);
        log.info("Order #{}: courier arrived, status: IN_DELIVERY", orderId);
        return toResponse(order);
    }

    // ==================== ОБЩИЕ МЕТОДЫ ====================

    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrderOrThrow(orderId));
    }

    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersBySeller(Long sellerId) {
        return orderRepository.findBySellerId(sellerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByCourier(Long courierId) {
        return orderRepository.findByCourierId(courierId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ==================== ТАЙМЕРЫ (вызываются из Scheduler) ====================

    /**
     * Таймер BPMN: "Продавец не реагирует в течение 10 минут".
     * Если заказ в статусе IN_PROCESSING и прошло больше N минут — отменяем.
     */
    @Transactional
    public void cancelOverdueOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> overdueOrders = orderRepository
                .findByStatusAndSellerNotifiedAtBefore(OrderStatus.IN_PROCESSING, deadline);

        for (Order order : overdueOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason("Продавец не реагирует в течение " + timeoutMinutes + " минут");
            orderRepository.save(order);

            notificationService.notifyCustomerStatusChanged(order);
            log.warn("Order #{} auto-cancelled: seller timeout ({} min)", order.getId(), timeoutMinutes);
        }
    }

    /**
     * Таймер BPMN: "Курьер не пришёл к назначенному времени".
     * Если заказ в статусе AWAITING_COURIER и прошло больше N минут — помечаем DELAYED.
     */
    @Transactional
    public void markDelayedOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> delayedOrders = orderRepository
                .findByStatusAndCourierAssignedAtBefore(OrderStatus.AWAITING_COURIER, deadline);

        for (Order order : delayedOrders) {
            order.setStatus(OrderStatus.DELAYED);
            orderRepository.save(order);

            notificationService.notifyCustomerStatusChanged(order);
            log.warn("Order #{} marked as DELAYED: courier timeout ({} min)", order.getId(), timeoutMinutes);
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void assertStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(
                    String.format("Order #%d has status %s, expected %s",
                            order.getId(), order.getStatus(), expected));
        }
    }

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .sellerId(order.getSeller().getId())
                .sellerName(order.getSeller().getName())
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .courierName(order.getCourier() != null ? order.getCourier().getName() : null)
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream()
                        .map(i -> OrderItemResponse.builder()
                                .id(i.getId())
                                .productName(i.getProductName())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .cancelReason(order.getCancelReason())
                .build();
    }
}
