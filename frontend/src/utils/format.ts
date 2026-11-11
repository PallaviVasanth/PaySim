import type { PaymentStatus } from '../api/types';

export function formatMoney(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

export function formatRelative(iso: string): string {
  const seconds = Math.round((new Date(iso).getTime() - Date.now()) / 1000);
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });
  const abs = Math.abs(seconds);
  if (abs < 60) return rtf.format(seconds, 'second');
  if (abs < 3600) return rtf.format(Math.round(seconds / 60), 'minute');
  if (abs < 86400) return rtf.format(Math.round(seconds / 3600), 'hour');
  return rtf.format(Math.round(seconds / 86400), 'day');
}

export function shortId(id: string): string {
  return id.slice(0, 8);
}

export const STATUS_LABEL: Record<PaymentStatus, string> = {
  SUCCEEDED: 'Succeeded',
  DECLINED: 'Declined',
  REFUNDED: 'Refunded',
};

export const DECLINE_REASON_LABEL: Record<string, string> = {
  INSUFFICIENT_FUNDS: 'Insufficient funds',
  RISK_CHECK_FAILED: 'Risk check failed',
  LIMIT_EXCEEDED: 'Amount above the 10,000.00 limit',
};

export function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `key-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
