/**
 * Money is always handled as integer minor units (cents) — never floats — and
 * only turned into a decimal string at the edges: display and form input.
 */

const CURRENCY = "EUR";

export function formatCents(cents: number, locale: string): string {
  return new Intl.NumberFormat(locale, { style: "currency", currency: CURRENCY })
    .format(cents / 100);
}

/** A cents amount as a plain decimal for a number input (e.g. 1299 → "12.99"). */
export function centsToInputValue(cents: number): string {
  return (cents / 100).toFixed(2);
}

/** Parses a number input's value into cents; null when it isn't a positive amount. */
export function inputValueToCents(value: string): number | null {
  const amount = Number.parseFloat(value.replace(",", "."));
  if (!Number.isFinite(amount) || amount <= 0) {
    return null;
  }
  return Math.round(amount * 100);
}
