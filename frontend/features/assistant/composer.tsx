"use client";

import { useEffect, useRef, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { ArrowUpIcon } from "@/components/ui/icons";

const MAX_LENGTH = 4000;
const MAX_ROWS_HEIGHT = 200;

type ComposerProps = {
  value: string;
  disabled: boolean;
  onChange: (value: string) => void;
  onSend: () => void;
};

export function Composer({ value, disabled, onChange, onSend }: ComposerProps) {
  const t = useTranslations("assistant");
  const textarea = useRef<HTMLTextAreaElement>(null);

  // Grows with the text up to a few lines, then scrolls internally.
  useEffect(() => {
    const field = textarea.current;
    if (!field) {
      return;
    }
    field.style.height = "auto";
    field.style.height = `${Math.min(field.scrollHeight, MAX_ROWS_HEIGHT)}px`;
  }, [value]);

  const canSend = value.trim().length > 0 && !disabled;

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter sends; Shift+Enter writes a new line.
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      if (canSend) {
        onSend();
      }
    }
  }

  return (
    <div className="flex items-end gap-2 rounded-lg border border-border-glass bg-[var(--surface-glass)] p-2 transition-colors duration-150 focus-within:border-accent/40">
      <label className="sr-only" htmlFor="assistant-composer">
        {t("inputLabel")}
      </label>
      <textarea
        id="assistant-composer"
        ref={textarea}
        rows={1}
        value={value}
        maxLength={MAX_LENGTH}
        placeholder={t("placeholder")}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={handleKeyDown}
        className="max-h-[200px] min-h-9 flex-1 resize-none bg-transparent px-1.5 py-1.5 text-[15px] leading-relaxed text-foreground outline-none placeholder:text-muted"
      />
      <button
        type="button"
        onClick={onSend}
        disabled={!canSend}
        aria-label={t("send")}
        className="flex size-9 shrink-0 items-center justify-center rounded-md bg-accent text-accent-foreground transition-opacity duration-150 hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
      >
        <ArrowUpIcon width={18} height={18} />
      </button>
    </div>
  );
}
