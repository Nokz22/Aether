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
    return request("/api/auth/me", {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  },
};
