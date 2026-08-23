package com.aether.finances.internal;

/** Total spent or earned in one category, for the summary breakdown. */
record CategoryTotal(String category, TransactionType type, long totalCents) {}
