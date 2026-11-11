export type PaymentStatus = 'SUCCEEDED' | 'DECLINED' | 'REFUNDED';
export type AuditEventType = 'PAYMENT_CREATED' | 'PAYMENT_REFUNDED';
export type Role = 'OPERATOR' | 'VIEWER';

export interface Payment {
  id: string;
  customerReference: string;
  orderReference: string;
  amount: number;
  currency: string;
  description: string | null;
  status: PaymentStatus;
  declineReason: string | null;
  createdAt: string;
  updatedAt: string;
  refundedAt: string | null;
}

export interface AuditEvent {
  id: number;
  eventType: AuditEventType;
  previousStatus: PaymentStatus | null;
  newStatus: PaymentStatus | null;
  message: string;
  occurredAt: string;
}

export interface PaymentDetail {
  payment: Payment;
  auditTrail: AuditEvent[];
}

export interface Paged<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface RecentActivity {
  eventType: AuditEventType;
  paymentId: string;
  orderReference: string;
  previousStatus: PaymentStatus | null;
  newStatus: PaymentStatus | null;
  message: string;
  occurredAt: string;
}

export interface DashboardSummary {
  totalTransactions: number;
  successfulTransactions: number;
  declinedTransactions: number;
  refundedTransactions: number;
  statusDistribution: Record<PaymentStatus, number>;
  recentActivity: RecentActivity[];
}

export interface CurrentUser {
  username: string;
  roles: Role[];
}

export interface CreatePaymentInput {
  customerReference: string;
  orderReference: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface FieldViolation {
  field: string;
  message: string;
}

export type SortField = 'createdAt' | 'amount' | 'status' | 'orderReference';
export type SortDirection = 'asc' | 'desc';

export interface PaymentQuery {
  q: string;
  status: PaymentStatus | '';
  sort: SortField;
  direction: SortDirection;
  page: number;
  size: number;
}
