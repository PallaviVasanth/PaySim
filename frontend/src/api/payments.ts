
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
  const { data } = await request<CurrentUser>('/api/v1/me', {
    skipUnauthorizedHandler: true,
  });
  return data;
}

export async function getSummary(authenticated = false): Promise<DashboardSummary> {
  const endpoint = authenticated
    ? '/api/v1/dashboard/summary'
    : '/api/v1/demo/dashboard/summary';

  return (await request<DashboardSummary>(endpoint)).data;
}

export async function listPayments(
  query: PaymentQuery,
  authenticated = false,
): Promise<Paged<Payment>> {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
    sort: query.sort,
    direction: query.direction,
  });

  if (query.q.trim()) params.set('q', query.q.trim());
  if (query.status) params.set('status', query.status);

  const endpoint = authenticated
    ? '/api/v1/payments'
    : '/api/v1/demo/payments';

  return (await request<Paged<Payment>>(`${endpoint}?${params.toString()}`)).data;
}

export async function getPayment(
  id: string,
  authenticated = false,
): Promise<PaymentDetail> {
  const endpoint = authenticated
    ? '/api/v1/payments'
    : '/api/v1/demo/payments';

  return (
    await request<PaymentDetail>(`${endpoint}/${encodeURIComponent(id)}`)
  ).data;
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

  return {
    payment: data,
    replayed: headers.get('Idempotent-Replayed') === 'true',
  };
}

export async function refundPayment(id: string): Promise<Payment> {
  return (
    await request<Payment>(
      `/api/v1/payments/${encodeURIComponent(id)}/refund`,
      { method: 'POST' },
    )
  ).data;
}
