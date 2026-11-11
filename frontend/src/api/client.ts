import type { FieldViolation } from './types';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

// Credentials live in memory only: nothing is written to localStorage/sessionStorage/cookies.
let authorization: string | null = null;
let onUnauthorized: (() => void) | null = null;

export function setCredentials(username: string, password: string): void {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = '';
  bytes.forEach((b) => {
    binary += String.fromCharCode(b);
  });
  authorization = `Basic ${btoa(binary)}`;
}

export function clearCredentials(): void {
  authorization = null;
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldViolation[];

  constructor(status: number, code: string, message: string, fieldErrors: FieldViolation[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST';
  body?: unknown;
  idempotencyKey?: string;
  /** Do not trigger the global "session expired" handler on 401 (used by the login call). */
  skipUnauthorizedHandler?: boolean;
}

export interface ApiResult<T> {
  data: T;
  headers: Headers;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiResult<T>> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (authorization) headers.Authorization = authorization;
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (options.idempotencyKey) headers['Idempotency-Key'] = options.idempotencyKey;

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the PaySim API. Check that the backend is running.');
  }

  if (!response.ok) {
    let code = 'HTTP_ERROR';
    let message = `Request failed with status ${response.status}.`;
    let fieldErrors: FieldViolation[] = [];
    try {
      const err = await response.json();
      code = err.code ?? code;
      message = err.message ?? message;
      fieldErrors = err.fieldErrors ?? [];
    } catch {
      // Non-JSON error body (e.g. proxy error page): keep the generic message.
    }
    if (response.status === 401 && !options.skipUnauthorizedHandler) onUnauthorized?.();
    throw new ApiError(response.status, code, message, fieldErrors);
  }

  const data = (await response.json()) as T;
  return { data, headers: response.headers };
}
