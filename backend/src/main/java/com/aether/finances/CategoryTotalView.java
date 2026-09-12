package com.aether.finances;

public record CategoryTotalView(String category, EntryType type, long totalCents) {}
