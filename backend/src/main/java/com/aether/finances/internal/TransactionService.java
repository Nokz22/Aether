package com.aether.finances.internal;

import com.aether.shared.ApiException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TransactionService {

    private final TransactionRepository transactions;

    TransactionService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    List<TransactionResponse> list(UUID userId, LocalDate from, LocalDate to) {
        return inRange(userId, from, to).stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    TransactionSummary summary(UUID userId, LocalDate from, LocalDate to) {
        List<Transaction> all = inRange(userId, from, to);
        long income = sumOf(all, TransactionType.INCOME);
        long expense = sumOf(all, TransactionType.EXPENSE);

        List<CategoryTotal> byCategory = all.stream()
                .collect(Collectors.groupingBy(
                        t -> Map.entry(t.category(), t.type()),
                        Collectors.summingLong(Transaction::amountCents)))
                .entrySet().stream()
                .map(e -> new CategoryTotal(e.getKey().getKey(), e.getKey().getValue(), e.getValue()))
                .sorted(Comparator.comparingLong(CategoryTotal::totalCents).reversed())
                .toList();

        return new TransactionSummary(income, expense, income - expense, byCategory);
    }

    @Transactional
    TransactionResponse create(UUID userId, TransactionType type, long amountCents,
                               String category, String description, LocalDate date) {
        Transaction transaction = new Transaction(userId, type, amountCents,
                normalize(category), normalize(description), date);
        transactions.save(transaction);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    TransactionResponse update(UUID userId, UUID transactionId, TransactionType type,
                               Long amountCents, String category, String description,
                               LocalDate date) {
        Transaction transaction = owned(userId, transactionId);
        transaction.update(type, amountCents,
                category == null ? null : category.trim(),
                description == null ? null : description.trim(), date);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    void delete(UUID userId, UUID transactionId) {
        transactions.delete(owned(userId, transactionId));
    }

    private List<Transaction> inRange(UUID userId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_range",
                    "The range end must not be before its start.");
        }
        return transactions.findInRange(userId, from, to);
    }

    // Ownership failures surface as 404, never revealing another user's transaction exists.
    private Transaction owned(UUID userId, UUID transactionId) {
        return transactions.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "transaction_not_found",
                        "Transaction not found."));
    }

    private static long sumOf(List<Transaction> all, TransactionType type) {
        return all.stream()
                .filter(t -> t.type() == type)
                .mapToLong(Transaction::amountCents)
                .sum();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
