package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.CreateCourierRequest;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.Courier;
import ru.itmo.ordermanagement.repository.CourierRepository;

import java.util.List;

@RestController
@RequestMapping("/api/couriers")
@RequiredArgsConstructor
@Tag(name = "Couriers", description = "Управление курьерами")
public class CourierController {

    private final CourierRepository courierRepository;

    @PostMapping
    @Operation(summary = "Создать курьера")
    public ResponseEntity<Courier> create(@Valid @RequestBody CreateCourierRequest request) {
        Courier courier = Courier.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .available(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(courierRepository.save(courier));
    }

    @GetMapping
    @Operation(summary = "Получить всех курьеров")
    public ResponseEntity<List<Courier>> getAll() {
        return ResponseEntity.ok(courierRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить курьера по ID")
    public ResponseEntity<Courier> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found: " + id)));
    }
}
