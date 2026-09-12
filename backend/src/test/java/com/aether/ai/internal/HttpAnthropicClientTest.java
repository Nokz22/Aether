package com.aether.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aether.shared.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * The wire format, against a real HTTP server on a local port: what we send to
 * the Messages API and what we make of its answer.
 */
class HttpAnthropicClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpServer server;
    private final AtomicReference<JsonNode> received = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> headers = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String response = "{}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", this::handle);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        received.set(MAPPER.readTree(exchange.getRequestBody().readAllBytes()));
        headers.set(Map.of(
                "x-api-key", String.valueOf(exchange.getRequestHeaders().getFirst("x-api-key")),
                "anthropic-version",
                String.valueOf(exchange.getRequestHeaders().getFirst("anthropic-version")),
                "content-length",
                String.valueOf(exchange.getRequestHeaders().getFirst("content-length"))));
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private HttpAnthropicClient client() {
        AiProperties properties = new AiProperties("secret-key", "claude-sonnet-5",
                "http://127.0.0.1:" + server.getAddress().getPort(), 2048, 5);
        return new HttpAnthropicClient(RestClient.builder(), properties, MAPPER);
    }

    @Test
    void sendsTheKeyTheVersionAndTheWholeRequest() {
        response = """
                {"stop_reason":"end_turn","content":[{"type":"text","text":"Olá."}]}
                """;

        AiTurn turn = client().send("system rules",
                List.of(AiMessage.user("olá")),
                List.of(new ToolSpec("list_today_habits", "habits",
                        Map.of("type", "object", "properties", Map.of()))));

        assertThat(headers.get())
                .containsEntry("x-api-key", "secret-key")
                .containsEntry("anthropic-version", "2023-06-01");
        // Sent with a length, not in chunks: some gateways refuse chunked bodies.
        assertThat(headers.get().get("content-length")).isNotEqualTo("null");

        JsonNode body = received.get();
        assertThat(body.path("model").asText()).isEqualTo("claude-sonnet-5");
        assertThat(body.path("max_tokens").asInt()).isEqualTo(2048);
        assertThat(body.path("system").asText()).isEqualTo("system rules");
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").get(0).path("content").get(0).path("text").asText())
                .isEqualTo("olá");
        assertThat(body.path("tools").get(0).path("name").asText()).isEqualTo("list_today_habits");
        assertThat(body.path("tools").get(0).path("input_schema").path("type").asText())
                .isEqualTo("object");

        assertThat(turn.text()).isEqualTo("Olá.");
        assertThat(turn.stopReason()).isEqualTo("end_turn");
        assertThat(turn.wantsTools()).isFalse();
    }

    @Test
    void readsAToolRequestWithItsInput() {
        response = """
                {"stop_reason":"tool_use","content":[
                  {"type":"text","text":"Vou ver."},
                  {"type":"tool_use","id":"call-1","name":"add_transaction",
                   "input":{"type":"EXPENSE","amount_cents":1299}}]}
                """;

        AiTurn turn = client().send("rules", List.of(AiMessage.user("gastei 12,99")), List.of());

        assertThat(turn.wantsTools()).isTrue();
        assertThat(turn.toolUses()).hasSize(1);
        AiContent.ToolUse use = turn.toolUses().getFirst();
        assertThat(use.id()).isEqualTo("call-1");
        assertThat(use.name()).isEqualTo("add_transaction");
        assertThat(use.input()).containsEntry("type", "EXPENSE").containsEntry("amount_cents", 1299);
        assertThat(turn.text()).isEqualTo("Vou ver.");
    }

    @Test
    void sendsToolResultsBackAndOmitsToolsOnTheLastRound() {
        response = """
                {"stop_reason":"end_turn","content":[{"type":"text","text":"Feito."}]}
                """;

        client().send("rules", List.of(
                AiMessage.user("marca o hábito"),
                AiMessage.assistant(List.of(
                        new AiContent.ToolUse("call-1", "check_in_habit", Map.of()))),
                AiMessage.toolResults(List.of(
                        new AiContent.ToolResult("call-1", "habit_not_found", true)))),
                List.of());

        JsonNode result = received.get().path("messages").get(2).path("content").get(0);
        assertThat(result.path("type").asText()).isEqualTo("tool_result");
        assertThat(result.path("tool_use_id").asText()).isEqualTo("call-1");
        assertThat(result.path("content").asText()).isEqualTo("habit_not_found");
        assertThat(result.path("is_error").asBoolean()).isTrue();
        // No tools offered, so the key is absent rather than an empty array.
        assertThat(received.get().has("tools")).isFalse();
    }

    @Test
    void aProviderFailureNeverLeaksItsBody() {
        status = 500;
        response = """
                {"error":{"message":"echo of the prompt: olá"}}
                """;

        assertThatThrownBy(() -> client().send("rules", List.of(AiMessage.user("olá")), List.of()))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> {
                    ApiException e = (ApiException) thrown;
                    assertThat(e.code()).isEqualTo("ai_unavailable");
                    assertThat(e.getMessage()).doesNotContain("olá");
                });
    }

    @Test
    void anUnknownBlockTypeIsIgnoredRatherThanFailingTheTurn() {
        response = """
                {"stop_reason":"end_turn","content":[
                  {"type":"something_new","payload":{}},
                  {"type":"text","text":"Olá."}]}
                """;

        assertThat(client().send("rules", List.of(AiMessage.user("olá")), List.of()).text())
                .isEqualTo("Olá.");
    }
}
