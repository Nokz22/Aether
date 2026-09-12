package com.aether.ai.internal;

import com.aether.auth.AuthApi;
import jakarta.validation.Valid;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
class AiController {

    private final AiService ai;
    private final AuthApi authApi;

    AiController(AiService ai, AuthApi authApi) {
        this.ai = ai;
        this.authApi = authApi;
    }

    @GetMapping("/messages")
    List<ChatMessageResponse> history(@RequestParam(defaultValue = "50") int limit) {
        return ai.history(authApi.currentUserId(), limit);
    }

    @PostMapping("/chat")
    ChatMessageResponse chat(@Valid @RequestBody ChatRequest request) {
        ChatContext context = new ChatContext(request.today(),
                ZoneOffset.ofTotalSeconds(request.offsetMinutes() * 60));
        return ai.reply(authApi.currentUserId(), request.message().trim(), context);
    }

    @DeleteMapping("/messages")
    ResponseEntity<Void> clear() {
        ai.clear(authApi.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
