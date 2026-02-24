package ru.itmo.ordermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Courier;

import java.util.Optional;

@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {

    /** Найти первого доступного курьера */
    Optional<Courier> findFirstByAvailableTrue();
}
