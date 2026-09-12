package com.aether.ai.internal;

import com.aether.shared.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Talks to the Anthropic Messages API.
 *
 * <p>Nothing here logs message content, tool inputs or tool results: the
 * conversation carries the user's personal data and must not leak into logs.
 */
@Component
class HttpAnthropicClient implements AnthropicClient {

    private static final String API_VERSION = "2023-06-01";
    private static final TypeReference<Map<String, Object>> INPUT_MAP = new TypeReference<>() {};
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    // A single round trip; a whole turn may make several of them.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

    private final RestClient restClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    HttpAnthropicClient(RestClient.Builder builder, AiProperties properties,
                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiTurn send(String systemPrompt, List<AiMessage> messages, List<ToolSpec> tools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("max_tokens", properties.maxTokens());
        body.put("system", systemPrompt);
        body.put("messages", messages.stream().map(HttpAnthropicClient::toWire).toList());
        if (!tools.isEmpty()) {
            // Omitted rather than sent empty: that is how the last round asks for
            // an answer in words instead of another tool call.
            body.put("tools", tools.stream().map(HttpAnthropicClient::toWire).toList());
        }

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", API_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    // Serialized up front so the request carries a Content-Length
                    // instead of being streamed in chunks.
                    .body(objectMapper.writeValueAsBytes(body))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize the model request", e);
        } catch (RestClientException e) {
            // Deliberately drops the provider's message: it can echo request content.
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ai_unavailable",
                    "The assistant is unavailable right now.");
        }
        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ai_unavailable",
                    "The assistant returned an empty response.");
        }
        return fromWire(response);
    }

    private AiTurn fromWire(JsonNode response) {
        List<AiContent> content = new ArrayList<>();
        for (JsonNode block : response.path("content")) {
            switch (block.path("type").asText()) {
                case "text" -> content.add(new AiContent.Text(block.path("text").asText()));
                case "tool_use" -> content.add(new AiContent.ToolUse(
                        block.path("id").asText(),
                        block.path("name").asText(),
                        toInputMap(block.path("input"))));
                default -> {
                    // Unknown block types are ignored rather than failing the turn.
                }
            }
        }
        return new AiTurn(content, response.path("stop_reason").asText(""));
    }

    private Map<String, Object> toInputMap(JsonNode input) {
        return input.isObject() ? objectMapper.convertValue(input, INPUT_MAP) : Map.of();
    }

    private static Map<String, Object> toWire(AiMessage message) {
        return Map.of(
                "role", message.role(),
                "content", message.content().stream().map(HttpAnthropicClient::toWire).toList());
    }

    private static Map<String, Object> toWire(AiContent content) {
        return switch (content) {
            case AiContent.Text text -> Map.of("type", "text", "text", text.text());
            case AiContent.ToolUse use -> Map.of(
                    "type", "tool_use", "id", use.id(), "name", use.name(), "input", use.input());
            case AiContent.ToolResult result -> Map.of(
                    "type", "tool_result",
                    "tool_use_id", result.toolUseId(),
                    "content", result.content(),
                    "is_error", result.isError());
        };
    }

    private static Map<String, Object> toWire(ToolSpec tool) {
        return Map.of(
                "name", tool.name(),
                "description", tool.description(),
                "input_schema", tool.inputSchema());
    }
}
