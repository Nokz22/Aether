package com.aether.finances.internal;

import com.aether.auth.AuthApi;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
class TransactionController {

    private final TransactionService transactionService;
    private final AuthApi authApi;

    TransactionController(TransactionService transactionService, AuthApi authApi) {
        this.transactionService = transactionService;
        this.authApi = authApi;
    }

    @GetMapping
    List<TransactionResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.list(authApi.currentUserId(), from, to);
    }

    @GetMapping("/summary")
    TransactionSummary summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactionService.summary(authApi.currentUserId(), from, to);
    }

    @PostMapping
    ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse transaction = transactionService.create(authApi.currentUserId(),
                request.type(), request.amountCents(), request.category(),
                request.description(), request.date());
        return ResponseEntity
                .created(URI.create("/api/transactions/" + transaction.id()))
                .body(transaction);
    }

    @PatchMapping("/{id}")
    TransactionResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        return transactionService.update(authApi.currentUserId(), id,
                request.type(), request.amountCents(), request.category(),
                request.description(), request.date());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(authApi.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
