import { request } from './client';
import type {
  CreatePaymentInput,
  CurrentUser,
  DashboardSummary,
  Paged,
  Payment,
  PaymentDetail,
  PaymentQuery,
} from './types';

export async function login(): Promise<CurrentUser> {
  const { data } = await request<CurrentUser>('/api/v1/me', { skipUnauthorizedHandler: true });
  return data;
}

export async function getSummary(): Promise<DashboardSummary> {
  return (await request<DashboardSummary>('/api/v1/dashboard/summary')).data;
}

export async function listPayments(query: PaymentQuery): Promise<Paged<Payment>> {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
    sort: query.sort,
    direction: query.direction,
  });
  if (query.q.trim()) params.set('q', query.q.trim());
  if (query.status) params.set('status', query.status);
  return (await request<Paged<Payment>>(`/api/v1/payments?${params.toString()}`)).data;
}

export async function getPayment(id: string): Promise<PaymentDetail> {
  return (await request<PaymentDetail>(`/api/v1/payments/${encodeURIComponent(id)}`)).data;
}

export async function createPayment(
  input: CreatePaymentInput,
  idempotencyKey: string,
): Promise<{ payment: Payment; replayed: boolean }> {
  const { data, headers } = await request<Payment>('/api/v1/payments', {
    method: 'POST',
    body: input,
    idempotencyKey,
  });
  return { payment: data, replayed: headers.get('Idempotent-Replayed') === 'true' };
}

export async function refundPayment(id: string): Promise<Payment> {
  return (await request<Payment>(`/api/v1/payments/${encodeURIComponent(id)}/refund`, { method: 'POST' })).data;
}
