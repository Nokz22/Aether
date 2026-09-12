package com.aether.ai.internal;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * The caller's calendar context. The server never guesses a timezone: the
 * client sends its local date and offset with every message.
 */
record ChatContext(LocalDate today, ZoneOffset offset) {}
