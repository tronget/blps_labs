package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.*;
import ru.itmo.ordermanagement.model.enums.OrderStatus;
import ru.itmo.ordermanagement.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Управление заказами (BPMN бизнес-процесс)")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Создать заказ",
            description = "BPMN: Заказчик → 'Создать заказ'. Товары собраны в корзине → заказ создаётся, продавец уведомляется.")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Получить заказы покупателя")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @PostMapping("/{orderId}/review")
    @Operation(summary = "Проверить заказ (решение продавца)",
            description = "BPMN: Продавец → 'Проверить заказ' → 'Возможно ли выполнить заказ?'. " +
                    "canFulfill=true → приготовление, canFulfill=false → отмена.")
    public ResponseEntity<OrderResponse> reviewOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody ReviewOrderRequest request) {
        return ResponseEntity.ok(orderService.reviewOrder(orderId, request));
    }

    @PostMapping("/{orderId}/assemble")
    @Operation(summary = "Собрать заказ",
            description = "BPMN: Продавец → 'Собрать заказ'. Переход COOKING → ASSEMBLING.")
    public ResponseEntity<OrderResponse> assembleOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.assembleOrder(orderId));
    }

    @PostMapping("/{orderId}/search-courier")
    @Operation(summary = "Искать курьера",
            description = "BPMN: Продавец → 'Искать курьера'. Переход ASSEMBLING → SEARCHING_COURIER.")
    public ResponseEntity<OrderResponse> searchCourier(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.searchCourier(orderId));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Получить заказы продавца")
    public ResponseEntity<List<OrderResponse>> getOrdersBySeller(@PathVariable Long sellerId) {
        return ResponseEntity.ok(orderService.getOrdersBySeller(sellerId));
    }

    @PostMapping("/{orderId}/courier/{courierId}/accept")
    @Operation(summary = "Курьер принял запрос на доставку",
            description = "BPMN: Курьер → 'Принял запрос на доставку'.")
    public ResponseEntity<OrderResponse> courierAcceptDelivery(
            @PathVariable Long orderId,
            @PathVariable Long courierId) {
        return ResponseEntity.ok(orderService.courierAcceptDelivery(orderId, courierId));
    }

    @PostMapping("/{orderId}/courier/{courierId}/arrived")
    @Operation(summary = "Курьер пришёл в заведение",
            description = "BPMN: Курьер → 'Курьер пришёл в заведение'. " +
                    "Продавец → 'Передать заказ курьеру'. Переход → IN_DELIVERY.")
    public ResponseEntity<OrderResponse> courierArrived(
            @PathVariable Long orderId,
            @PathVariable Long courierId) {
        return ResponseEntity.ok(orderService.courierArrived(orderId, courierId));
    }

    @GetMapping("/courier/{courierId}")
    @Operation(summary = "Получить заказы курьера")
    public ResponseEntity<List<OrderResponse>> getOrdersByCourier(@PathVariable Long courierId) {
        return ResponseEntity.ok(orderService.getOrdersByCourier(courierId));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Получить заказ по ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping
    @Operation(summary = "Получить все заказы")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Получить заказы по статусу")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }
}
