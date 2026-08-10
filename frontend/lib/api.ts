import type { components } from "./api-types";

export type ApiErrorBody = components["schemas"]["ApiError"];
export type TokenResponse = components["schemas"]["TokenResponse"];
export type UserResponse = components["schemas"]["UserResponse"];
export type RegisterRequest = components["schemas"]["RegisterRequest"];
export type LoginRequest = components["schemas"]["LoginRequest"];

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** A non-2xx response carrying the backend's typed error body. */
export class ApiRequestError extends Error {
  constructor(
    readonly body: ApiErrorBody,
    readonly status: number,
  ) {
    super(body.message);
    this.name = "ApiRequestError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    // The refresh cookie must travel with auth calls.
    credentials: "include",
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!response.ok) {
    throw new ApiRequestError((await response.json()) as ApiErrorBody, response.status);
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

export type HabitResponse = components["schemas"]["HabitResponse"];
export type HabitDay = components["schemas"]["HabitDay"];
export type HabitSummary = components["schemas"]["HabitSummary"];

export type NoteSummary = components["schemas"]["NoteSummary"];
export type NoteResponse = components["schemas"]["NoteResponse"];
export type CreateNoteRequest = components["schemas"]["CreateNoteRequest"];
export type UpdateNoteRequest = components["schemas"]["UpdateNoteRequest"];

function bearer(accessToken: string): Record<string, string> {
  return { Authorization: `Bearer ${accessToken}` };
}

export const authApi = {
  register(body: RegisterRequest): Promise<TokenResponse> {
    return request("/api/auth/register", { method: "POST", body: JSON.stringify(body) });
  },
  login(body: LoginRequest): Promise<TokenResponse> {
    return request("/api/auth/login", { method: "POST", body: JSON.stringify(body) });
  },
  refresh(): Promise<TokenResponse> {
    return request("/api/auth/refresh", { method: "POST" });
  },
  logout(): Promise<void> {
    return request("/api/auth/logout", { method: "POST" });
  },
  me(accessToken: string): Promise<UserResponse> {
    return request("/api/auth/me", { headers: bearer(accessToken) });
  },
};

export const habitsApi = {
  list(today: string, accessToken: string): Promise<HabitResponse[]> {
    return request(`/api/habits?today=${today}`, { headers: bearer(accessToken) });
  },
  create(name: string, accessToken: string): Promise<HabitSummary> {
    return request("/api/habits", {
      method: "POST",
      body: JSON.stringify({ name }),
      headers: bearer(accessToken),
    });
  },
  checkIn(habitId: string, date: string, today: string, accessToken: string): Promise<void> {
    return request(`/api/habits/${habitId}/checkins/${date}?today=${today}`, {
      method: "PUT",
      headers: bearer(accessToken),
    });
  },
  removeCheckin(habitId: string, date: string, accessToken: string): Promise<void> {
    return request(`/api/habits/${habitId}/checkins/${date}`, {
      method: "DELETE",
      headers: bearer(accessToken),
    });
  },
  rename(habitId: string, name: string, accessToken: string): Promise<void> {
    return request(`/api/habits/${habitId}`, {
      method: "PATCH",
      body: JSON.stringify({ name }),
      headers: bearer(accessToken),
    });
  },
  remove(habitId: string, accessToken: string): Promise<void> {
    return request(`/api/habits/${habitId}`, {
      method: "DELETE",
      headers: bearer(accessToken),
    });
  },
};

export const notesApi = {
  list(accessToken: string): Promise<NoteSummary[]> {
    return request("/api/notes", { headers: bearer(accessToken) });
  },
  get(noteId: string, accessToken: string): Promise<NoteResponse> {
    return request(`/api/notes/${noteId}`, { headers: bearer(accessToken) });
  },
  create(body: CreateNoteRequest, accessToken: string): Promise<NoteResponse> {
    return request("/api/notes", {
      method: "POST",
      body: JSON.stringify(body),
      headers: bearer(accessToken),
    });
  },
  update(noteId: string, body: UpdateNoteRequest, accessToken: string): Promise<NoteResponse> {
    return request(`/api/notes/${noteId}`, {
      method: "PATCH",
      body: JSON.stringify(body),
      headers: bearer(accessToken),
    });
  },
  remove(noteId: string, accessToken: string): Promise<void> {
    return request(`/api/notes/${noteId}`, {
      method: "DELETE",
      headers: bearer(accessToken),
    });
  },
};
