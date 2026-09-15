"use client";

import type { ChatMessage } from "@/lib/api";
import { ActionChips } from "./action-chips";

/**
 * One turn. What you wrote sits in a card on the right; the assistant answers
 * as plain text on the page, so its voice reads like the app talking rather
 * than another chat bubble.
 */
export function MessageTurn({ message }: { message: ChatMessage }) {
  if (message.role === "USER") {
    return (
      <li className="flex justify-end">
        <p className="max-w-[85%] whitespace-pre-wrap rounded-lg rounded-br-sm bg-accent/12 px-3.5 py-2.5 text-[15px] leading-relaxed">
          {message.content}
        </p>
      </li>
    );
  }

  return (
    <li>
      <p className="whitespace-pre-wrap text-[15px] leading-relaxed">{message.content}</p>
      <ActionChips tools={message.toolsUsed} />
    </li>
  );
}
