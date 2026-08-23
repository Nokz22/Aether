package com.aether.finances.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 31);
    private static final LocalDate DAY = LocalDate.of(2026, 8, 15);

    @Mock
    private TransactionRepository transactions;

    private TransactionService service() {
        return new TransactionService(transactions);
    }

    private Transaction tx(TransactionType type, long cents, String category) {
        return new Transaction(USER, type, cents, category, "", DAY);
    }

    @Test
    void createTrimsTextAndStoresPositiveCents() {
        TransactionResponse response =
                service().create(USER, TransactionType.EXPENSE, 1299, "  Food  ", "  lunch ", DAY);

        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.amountCents()).isEqualTo(1299);
        assertThat(response.category()).isEqualTo("Food");
        assertThat(response.description()).isEqualTo("lunch");
        verify(transactions).save(any(Transaction.class));
    }

    @Test
    void summaryComputesIncomeExpenseAndNet() {
        when(transactions.findInRange(USER, FROM, TO)).thenReturn(List.of(
                tx(TransactionType.INCOME, 200_000, "Salary"),
                tx(TransactionType.EXPENSE, 5_000, "Food"),
                tx(TransactionType.EXPENSE, 3_000, "Food"),
                tx(TransactionType.EXPENSE, 2_000, "Transport")));

        TransactionSummary summary = service().summary(USER, FROM, TO);

        assertThat(summary.incomeCents()).isEqualTo(200_000);
        assertThat(summary.expenseCents()).isEqualTo(10_000);
        assertThat(summary.netCents()).isEqualTo(190_000);
    }

    @Test
    void summaryGroupsByCategoryAndTypeSortedByTotal() {
        when(transactions.findInRange(USER, FROM, TO)).thenReturn(List.of(
                tx(TransactionType.EXPENSE, 5_000, "Food"),
                tx(TransactionType.EXPENSE, 3_000, "Food"),
                tx(TransactionType.EXPENSE, 2_000, "Transport"),
                tx(TransactionType.INCOME, 200_000, "Salary")));

        List<CategoryTotal> byCategory = service().summary(USER, FROM, TO).byCategory();

        // Salary (200000) first, then Food expenses combined (8000), then Transport (2000).
        assertThat(byCategory).containsExactly(
                new CategoryTotal("Salary", TransactionType.INCOME, 200_000),
                new CategoryTotal("Food", TransactionType.EXPENSE, 8_000),
                new CategoryTotal("Transport", TransactionType.EXPENSE, 2_000));
    }

    @Test
    void rangeQueriesRejectAnEndBeforeStart() {
        assertThatThrownBy(() -> service().list(USER, TO, FROM))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_range"));
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Transaction transaction = tx(TransactionType.EXPENSE, 5_000, "Food");
        when(transactions.findByIdAndUserId(transaction.id(), USER))
                .thenReturn(Optional.of(transaction));

        TransactionResponse updated = service().update(
                USER, transaction.id(), null, 7_500L, null, null, null);

        assertThat(updated.amountCents()).isEqualTo(7_500);
        assertThat(updated.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(updated.category()).isEqualTo("Food");
    }

    @Test
    void someoneElsesTransactionIsNotFound() {
        UUID foreign = UUID.randomUUID();
        when(transactions.findByIdAndUserId(foreign, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(USER, foreign))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("transaction_not_found"));
        verify(transactions, never()).delete(any());
    }
}
