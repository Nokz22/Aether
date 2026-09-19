import type { NextRequest } from "next/server";

/**
 * Passes /api/* through to the backend over the internal network.
 *
 * The browser therefore only ever talks to this app's own origin: there is no
 * CORS, and the refresh cookie — `SameSite=Strict` — always travels. A rewrite
 * in `next.config.ts` cannot do this job: its destination is baked into the
 * build, while the backend's address is only known when the container starts.
 */
const BACKEND_URL = process.env.API_INTERNAL_URL ?? "http://localhost:8080";

/** Headers that belong to a single hop and must not be forwarded. */
const HOP_BY_HOP = new Set([
  "connection",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
]);

async function proxy(request: NextRequest): Promise<Response> {
  const incoming = new URL(request.url);
  const headers = new Headers(request.headers);
  // The backend must see its own host, not ours.
  headers.delete("host");

  // Bodies here are small (a note, a chat message), so buffering them keeps the
  // proxy simple and avoids half-duplex streaming caveats.
  const body = request.method === "GET" || request.method === "HEAD"
    ? undefined
    : await request.arrayBuffer();

  let response: Response;
  try {
    response = await fetch(`${BACKEND_URL}${incoming.pathname}${incoming.search}`, {
      method: request.method,
      headers,
      body,
      redirect: "manual",
      cache: "no-store",
    });
  } catch {
    // The backend is unreachable; say so in the shape the frontend already parses.
    return Response.json(
      { code: "api_unreachable", message: "The API is unavailable.", fields: {} },
      { status: 502 },
    );
  }

  const outgoing = new Headers();
  response.headers.forEach((value, name) => {
    if (!HOP_BY_HOP.has(name) && name !== "set-cookie") {
      outgoing.set(name, value);
    }
  });
  // Set-Cookie must survive as separate headers — the refresh cookie rides here.
  for (const cookie of response.headers.getSetCookie()) {
    outgoing.append("set-cookie", cookie);
  }

  return new Response(response.body, { status: response.status, headers: outgoing });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;

// Never cached, never prerendered: every call is a live pass-through.
export const dynamic = "force-dynamic";
