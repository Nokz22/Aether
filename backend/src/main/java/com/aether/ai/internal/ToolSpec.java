package com.aether.ai.internal;

import java.util.Map;

/**
 * A tool offered to the model: its name, what it does, and a JSON Schema for
 * its input.
 */
record ToolSpec(String name, String description, Map<String, Object> inputSchema) {}
