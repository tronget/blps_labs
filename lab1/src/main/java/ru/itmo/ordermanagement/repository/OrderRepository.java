package ru.itmo.ordermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findBySellerId(Long sellerId);

    List<Order> findByCourierId(Long courierId);

    List<Order> findByStatusAndSellerNotifiedAtBefore(OrderStatus status, LocalDateTime deadline);

    List<Order> findByStatusAndCourierAssignedAtBefore(OrderStatus status, LocalDateTime deadline);
}
