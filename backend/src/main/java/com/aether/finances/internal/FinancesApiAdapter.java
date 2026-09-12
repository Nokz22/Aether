package com.aether.finances.internal;

import com.aether.finances.CategoryTotalView;
import com.aether.finances.EntryType;
import com.aether.finances.EntryView;
import com.aether.finances.FinancesApi;
import com.aether.finances.MoneySummaryView;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class FinancesApiAdapter implements FinancesApi {

    private final TransactionService transactions;

    FinancesApiAdapter(TransactionService transactions) {
        this.transactions = transactions;
    }

    @Override
    public MoneySummaryView summary(UUID userId, LocalDate from, LocalDate to) {
        TransactionSummary summary = transactions.summary(userId, from, to);
        return new MoneySummaryView(summary.incomeCents(), summary.expenseCents(),
                summary.netCents(),
                summary.byCategory().stream()
                        .map(total -> new CategoryTotalView(total.category(),
                                toEntryType(total.type()), total.totalCents()))
                        .toList());
    }

    @Override
    public List<EntryView> transactions(UUID userId, LocalDate from, LocalDate to) {
        return transactions.list(userId, from, to).stream()
                .map(FinancesApiAdapter::toView)
                .toList();
    }

    @Override
    public EntryView addTransaction(UUID userId, EntryType type, long amountCents,
                                    String category, String description, LocalDate date) {
        return toView(transactions.create(userId, toTransactionType(type), amountCents,
                category, description, date));
    }

    private static EntryView toView(TransactionResponse transaction) {
        return new EntryView(transaction.id(), toEntryType(transaction.type()),
                transaction.amountCents(), transaction.category(),
                transaction.description(), transaction.date());
    }

    private static EntryType toEntryType(TransactionType type) {
        return switch (type) {
            case INCOME -> EntryType.INCOME;
            case EXPENSE -> EntryType.EXPENSE;
        };
    }

    private static TransactionType toTransactionType(EntryType type) {
        return switch (type) {
            case INCOME -> TransactionType.INCOME;
            case EXPENSE -> TransactionType.EXPENSE;
        };
    }
}
