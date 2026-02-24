package ru.itmo.ordermanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
