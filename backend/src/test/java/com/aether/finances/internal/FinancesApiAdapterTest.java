package com.aether.finances.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aether.finances.CategoryTotalView;
import com.aether.finances.EntryType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancesApiAdapterTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");

    @Mock
    private TransactionService transactions;

    @Test
    void summaryKeepsCentsAndMapsCategoryTypes() {
        when(transactions.summary(USER, FROM, TO)).thenReturn(new TransactionSummary(
                200_000, 4_299, 195_701,
                List.of(new CategoryTotal("Salário", TransactionType.INCOME, 200_000),
                        new CategoryTotal("Comida", TransactionType.EXPENSE, 4_299))));

        var summary = new FinancesApiAdapter(transactions).summary(USER, FROM, TO);

        assertThat(summary.incomeCents()).isEqualTo(200_000);
        assertThat(summary.expenseCents()).isEqualTo(4_299);
        assertThat(summary.netCents()).isEqualTo(195_701);
        assertThat(summary.byCategory()).containsExactly(
                new CategoryTotalView("Salário", EntryType.INCOME, 200_000),
                new CategoryTotalView("Comida", EntryType.EXPENSE, 4_299));
    }

    @Test
    void addTransactionTranslatesThePublicTypeBothWays() {
        UUID id = UUID.randomUUID();
        when(transactions.create(USER, TransactionType.EXPENSE, 1_299, "Comida", "almoço",
                LocalDate.of(2026, 9, 5)))
                .thenReturn(new TransactionResponse(id, TransactionType.EXPENSE, 1_299,
                        "Comida", "almoço", LocalDate.of(2026, 9, 5), NOW, NOW));

        var entry = new FinancesApiAdapter(transactions).addTransaction(
                USER, EntryType.EXPENSE, 1_299, "Comida", "almoço", LocalDate.of(2026, 9, 5));

        assertThat(entry.type()).isEqualTo(EntryType.EXPENSE);
        assertThat(entry.amountCents()).isEqualTo(1_299);
        assertThat(entry.category()).isEqualTo("Comida");
    }

    @Test
    void incomeEntriesMapToIncome() {
        UUID id = UUID.randomUUID();
        when(transactions.list(USER, FROM, TO)).thenReturn(List.of(
                new TransactionResponse(id, TransactionType.INCOME, 200_000, "Salário", "",
                        FROM, NOW, NOW)));

        assertThat(new FinancesApiAdapter(transactions).transactions(USER, FROM, TO))
                .singleElement()
                .satisfies(entry -> assertThat(entry.type()).isEqualTo(EntryType.INCOME));
    }
}
