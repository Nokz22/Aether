package com.aether.ai.internal;

import static com.aether.ai.internal.ScriptedAnthropicClient.text;
import static com.aether.ai.internal.ScriptedAnthropicClient.toolUse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final ChatContext CONTEXT =
            new ChatContext(LocalDate.of(2026, 9, 12), ZoneOffset.ofHours(1));

    @Mock
    private AiTools tools;

    @Mock
    private ChatMessageRepository messages;

    private final ScriptedAnthropicClient client = new ScriptedAnthropicClient();

    @BeforeEach
    void offerNoTools() {
        when(tools.specs()).thenReturn(List.of(
                new ToolSpec("list_today_habits", "habits", Map.of())));
    }

    private AiService service(String apiKey, int maxToolIterations) {
        return new AiService(client, tools, messages,
                new AiProperties(apiKey, "claude-sonnet-5", "https://example.invalid",
                        2048, maxToolIterations));
    }

    private AiService service() {
        return service("test-key", 5);
    }

    @Test
    void answersAndStoresBothSidesOfTheExchange() {
        client.script(text("Olá."));

        ChatMessageResponse reply = service().reply(USER, "olá", CONTEXT);

        assertThat(reply.role()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(reply.content()).isEqualTo("Olá.");
        assertThat(reply.toolsUsed()).isEmpty();

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ChatMessage::role, ChatMessage::content)
                .containsExactly(
                        tuple(ChatRole.USER, "olá"),
                        tuple(ChatRole.ASSISTANT, "Olá."));
    }

    @Test
    void carriesOutAToolAndFeedsItsResultBack() {
        client.script(
                toolUse("call-1", "list_today_habits", Map.of()),
                text("Tens 2 hábitos por fazer."));
        when(tools.run(USER, "list_today_habits", Map.of(), CONTEXT))
                .thenReturn(ToolOutcome.ok("[{\"name\":\"Ler\"}]"));

        ChatMessageResponse reply = service().reply(USER, "o que falta hoje?", CONTEXT);

        assertThat(reply.content()).isEqualTo("Tens 2 hábitos por fazer.");
        assertThat(reply.toolsUsed()).containsExactly("list_today_habits");

        // The second round carries the assistant's request and our answer to it.
        List<AiMessage> secondRound = client.calls().get(1).messages();
        assertThat(secondRound).hasSize(3);
        assertThat(secondRound.get(2).content()).containsExactly(
                new AiContent.ToolResult("call-1", "[{\"name\":\"Ler\"}]", false));
    }

    @Test
    void aFailedToolIsNotReportedAsWorkDone() {
        client.script(
                toolUse("call-1", "check_in_habit", Map.of("habit_id", "nope")),
                text("Não encontrei esse hábito."));
        when(tools.run(eq(USER), anyString(), any(), eq(CONTEXT)))
                .thenReturn(ToolOutcome.failed("habit_not_found"));

        ChatMessageResponse reply = service().reply(USER, "marca o hábito", CONTEXT);

        assertThat(reply.toolsUsed()).isEmpty();
        assertThat(client.calls().get(1).messages().get(2).content())
                .containsExactly(new AiContent.ToolResult("call-1", "habit_not_found", true));
    }

    @Test
    void stopsAtTheToolCeilingAndAsksForAnAnswerInWords() {
        client.script(
                toolUse("call-1", "list_today_habits", Map.of()),
                toolUse("call-2", "list_today_habits", Map.of()),
                text("Aqui está o resumo."));
        when(tools.run(eq(USER), anyString(), any(), eq(CONTEXT)))
                .thenReturn(ToolOutcome.ok("[]"));

        ChatMessageResponse reply = service("test-key", 2).reply(USER, "resume", CONTEXT);

        assertThat(reply.content()).isEqualTo("Aqui está o resumo.");
        assertThat(client.calls()).hasSize(3);
        // No tools offered on the last round, so the model has to write an answer.
        assertThat(client.lastCall().tools()).isEmpty();
    }

    @Test
    void refusesWithoutAnApiKeyAndStoresNothing() {
        assertThatThrownBy(() -> service("", 5).reply(USER, "olá", CONTEXT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("ai_not_configured");

        verify(messages, never()).save(any());
    }

    @Test
    void anEmptyAnswerIsTreatedAsAFailure() {
        client.script(text("   "));

        assertThatThrownBy(() -> service().reply(USER, "olá", CONTEXT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("ai_unavailable");
    }

    @Test
    void historyComesBackOldestFirst() {
        when(messages.findRecent(eq(USER), any(Pageable.class))).thenReturn(List.of(
                new ChatMessage(USER, ChatRole.ASSISTANT, "segundo", List.of()),
                new ChatMessage(USER, ChatRole.USER, "primeiro", List.of())));

        assertThat(service().history(USER, 50))
                .extracting(ChatMessageResponse::content)
                .containsExactly("primeiro", "segundo");
    }

    @Test
    void storedHistoryIsReplayedAsContext() {
        when(messages.findRecent(eq(USER), any(Pageable.class))).thenReturn(List.of(
                new ChatMessage(USER, ChatRole.ASSISTANT, "Olá.", List.of()),
                new ChatMessage(USER, ChatRole.USER, "olá", List.of())));
        client.script(text("Diz."));

        service().reply(USER, "e agora?", CONTEXT);

        assertThat(client.lastCall().messages())
                .extracting(AiMessage::role)
                .containsExactly("user", "assistant", "user");
        assertThat(client.lastCall().systemPrompt())
                .contains("2026-09-12")
                .contains("+01:00");
    }
}
