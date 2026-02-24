package ru.itmo.ordermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.ordermanagement.dto.CreateSellerRequest;
import ru.itmo.ordermanagement.exception.ResourceNotFoundException;
import ru.itmo.ordermanagement.model.entity.Seller;
import ru.itmo.ordermanagement.repository.SellerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "Sellers", description = "Управление продавцами")
public class SellerController {

    private final SellerRepository sellerRepository;

    @PostMapping
    @Operation(summary = "Создать продавца")
    public ResponseEntity<Seller> create(@Valid @RequestBody CreateSellerRequest request) {
        Seller seller = Seller.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerRepository.save(seller));
    }

    @GetMapping
    @Operation(summary = "Получить всех продавцов")
    public ResponseEntity<List<Seller>> getAll() {
        return ResponseEntity.ok(sellerRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить продавца по ID")
    public ResponseEntity<Seller> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + id)));
    }
}
