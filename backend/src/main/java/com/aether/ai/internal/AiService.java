package com.aether.ai.internal;

import com.aether.shared.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one message through the model, carrying out any tools it asks for.
 *
 * <p>Deliberately not transactional: a turn waits on the network for seconds,
 * and holding a database connection open across that would starve the pool.
 * Each stored message and each tool call commits on its own, so a provider
 * failure halfway through leaves the work already done intact — and the reply
 * always reports exactly what went through.
 */
@Service
class AiService {

    /** How many stored messages are replayed as context. */
    private static final int CONTEXT_MESSAGES = 20;
    private static final int MAX_HISTORY = 200;

    private final AnthropicClient client;
    private final AiTools tools;
    private final ChatMessageRepository messages;
    private final AiProperties properties;

    AiService(AnthropicClient client, AiTools tools, ChatMessageRepository messages,
              AiProperties properties) {
        this.client = client;
        this.tools = tools;
        this.messages = messages;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    List<ChatMessageResponse> history(UUID userId, int limit) {
        return recent(userId, Math.min(Math.max(limit, 1), MAX_HISTORY)).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    ChatMessageResponse reply(UUID userId, String message, ChatContext context) {
        if (!properties.configured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ai_not_configured",
                    "The assistant is not configured on this server.");
        }

        List<AiMessage> conversation = new ArrayList<>(replay(userId));
        conversation.add(AiMessage.user(message));
        messages.save(new ChatMessage(userId, ChatRole.USER, message, List.of()));

        String prompt = ChatPrompt.of(context);
        Set<String> toolsUsed = new LinkedHashSet<>();
        List<ToolSpec> specs = tools.specs();

        for (int iteration = 0; iteration < properties.maxToolIterations(); iteration++) {
            AiTurn turn = client.send(prompt, conversation, specs);
            if (!turn.wantsTools()) {
                return persistReply(userId, turn.text(), toolsUsed);
            }
            conversation.add(AiMessage.assistant(turn.content()));
            conversation.add(AiMessage.toolResults(run(userId, turn, context, toolsUsed)));
        }

        // Out of tool rounds: ask once more with no tools, so the model has to
        // answer in words instead of reaching for another one.
        return persistReply(userId, client.send(prompt, conversation, List.of()).text(), toolsUsed);
    }

    @Transactional
    void clear(UUID userId) {
        messages.deleteByUserId(userId);
    }

    private List<AiContent> run(UUID userId, AiTurn turn, ChatContext context,
                                Set<String> toolsUsed) {
        List<AiContent> results = new ArrayList<>();
        for (AiContent.ToolUse use : turn.toolUses()) {
            ToolOutcome outcome = tools.run(userId, use.name(), use.input(), context);
            if (!outcome.isError()) {
                toolsUsed.add(use.name());
            }
            results.add(new AiContent.ToolResult(use.id(), outcome.content(), outcome.isError()));
        }
        return results;
    }

    private ChatMessageResponse persistReply(UUID userId, String text, Set<String> toolsUsed) {
        if (text.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ai_unavailable",
                    "The assistant did not answer.");
        }
        ChatMessage reply = new ChatMessage(userId, ChatRole.ASSISTANT, text,
                List.copyOf(toolsUsed));
        messages.save(reply);
        return ChatMessageResponse.from(reply);
    }

    /** The tail of the stored conversation, oldest first, as plain text turns. */
    private List<AiMessage> replay(UUID userId) {
        return recent(userId, CONTEXT_MESSAGES).stream()
                .map(stored -> stored.role() == ChatRole.USER
                        ? AiMessage.user(stored.content())
                        : AiMessage.assistant(List.of(new AiContent.Text(stored.content()))))
                .toList();
    }

    private List<ChatMessage> recent(UUID userId, int limit) {
        List<ChatMessage> newestFirst = messages.findRecent(userId, PageRequest.of(0, limit));
        return newestFirst.reversed();
    }
}
