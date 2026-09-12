package com.aether.notes.internal;

/** First non-blank line of a note's body, capped — a label for titleless notes. */
final class NotePreview {

    private static final int MAX_LENGTH = 140;

    private NotePreview() {
    }

    static String of(String content) {
        return content.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .map(line -> line.length() > MAX_LENGTH ? line.substring(0, MAX_LENGTH) : line)
                .orElse("");
    }
}
