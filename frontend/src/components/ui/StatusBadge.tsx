import { CheckCircle2, Undo2, XCircle } from 'lucide-react';
import type { PaymentStatus } from '../../api/types';
import { STATUS_LABEL } from '../../utils/format';

const ICONS = { SUCCEEDED: CheckCircle2, DECLINED: XCircle, REFUNDED: Undo2 } as const;

/** Status is conveyed by icon and text as well as colour. */
export function StatusBadge({ status }: { status: PaymentStatus }) {
  const Icon = ICONS[status];
  return (
    <span className={`badge badge--${status.toLowerCase()}`}>
      <Icon size={14} aria-hidden="true" />
      {STATUS_LABEL[status]}
    </span>
  );
}
