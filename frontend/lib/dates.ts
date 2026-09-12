function pad(value: number): string {
  return String(value).padStart(2, "0");
}

/** The user's local calendar day as YYYY-MM-DD — the API never guesses timezones. */
export function localDateISO(now: Date = new Date()): string {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** Local midnight of the given day (drops the time component). */
export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

/** Local midnight of the Monday that begins this date's week. */
export function startOfWeek(date: Date): Date {
  const day = startOfDay(date);
  const mondayOffset = (day.getDay() + 6) % 7; // Sun=6 … Mon=0
  day.setDate(day.getDate() - mondayOffset);
  return day;
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

export function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear()
    && a.getMonth() === b.getMonth()
    && a.getDate() === b.getDate();
}

/** A local Date formatted for a `datetime-local` input value (YYYY-MM-DDTHH:mm). */
export function toDateTimeLocalValue(date: Date): string {
  return `${localDateISO(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Local first day of the given date's month. */
export function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

/** Local last day of the given date's month. */
export function endOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

export function addMonths(date: Date, months: number): Date {
  return new Date(date.getFullYear(), date.getMonth() + months, 1);
}
