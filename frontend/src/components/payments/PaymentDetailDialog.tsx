
import { Undo2 } from 'lucide-react';
import { useState } from 'react';
import { getPayment } from '../../api/payments';
import { useAuth } from '../../context/AuthContext';
import { useApi } from '../../hooks/useApi';
import { DECLINE_REASON_LABEL, formatDateTime, formatMoney } from '../../utils/format';
import { ErrorState } from '../ui/ErrorState';
import { Modal } from '../ui/Modal';
import { Spinner } from '../ui/Spinner';
import { StatusBadge } from '../ui/StatusBadge';
import { RefundConfirmDialog } from './RefundConfirmDialog';

interface Props {
  paymentId: string;
  onClose: () => void;
  onChanged: () => void;
}

export function PaymentDetailDialog({ paymentId, onClose, onChanged }: Props) {
  const { user, canWrite } = useAuth();
  const authenticated = Boolean(user);

  const { data, error, loading, reload } = useApi(
    () => getPayment(paymentId, authenticated),
    [paymentId, authenticated],
  );

  const [confirmingRefund, setConfirmingRefund] = useState(false);
  const payment = data?.payment;

  return (
    <Modal title="Payment details" onClose={onClose} size="lg">
      {loading && !data && <Spinner label="Loading payment" />}
      {error && !data && <ErrorState error={error} onRetry={reload} />}

      {payment && data && (
        <div className="detail">
          <div className="detail__head">
            <p className="detail__amount">{formatMoney(payment.amount, payment.currency)}</p>
            <StatusBadge status={payment.status} />
          </div>

          <dl className="detail__grid">
            <div><dt>Payment ID</dt><dd className="mono">{payment.id}</dd></div>
            <div><dt>Order reference</dt><dd>{payment.orderReference}</dd></div>
            <div><dt>Customer reference</dt><dd>{payment.customerReference}</dd></div>
            <div><dt>Created</dt><dd>{formatDateTime(payment.createdAt)}</dd></div>

            {payment.declineReason && (
              <div>
                <dt>Decline reason</dt>
                <dd>{DECLINE_REASON_LABEL[payment.declineReason] ?? payment.declineReason}</dd>
              </div>
            )}

            {payment.refundedAt && (
              <div><dt>Refunded</dt><dd>{formatDateTime(payment.refundedAt)}</dd></div>
            )}

            {payment.description && (
              <div className="detail__wide"><dt>Description</dt><dd>{payment.description}</dd></div>
            )}
          </dl>

          <section aria-labelledby="audit-heading">
            <h3 id="audit-heading">Audit trail</h3>
            <ol className="timeline">
              {data.auditTrail.map((event) => (
                <li key={event.id}>
                  <p>{event.message}</p>
                  <small>
                    {event.previousStatus
                      ? `${event.previousStatus} → ${event.newStatus}`
                      : `Initial status ${event.newStatus}`}
                    {' · '}
                    <time dateTime={event.occurredAt}>{formatDateTime(event.occurredAt)}</time>
                  </small>
                </li>
              ))}
            </ol>
          </section>

          <footer className="modal__footer">
            <button type="button" className="btn btn--ghost" onClick={onClose}>
              Close
            </button>

            {canWrite && payment.status === 'SUCCEEDED' && (
              <button
                type="button"
                className="btn btn--danger"
                onClick={() => setConfirmingRefund(true)}
              >
                <Undo2 size={16} aria-hidden="true" /> Refund…
              </button>
            )}
          </footer>

          {confirmingRefund && (
            <RefundConfirmDialog
              payment={payment}
              onClose={() => setConfirmingRefund(false)}
              onRefunded={() => {
                setConfirmingRefund(false);
                reload();
                onChanged();
              }}
            />
          )}
        </div>
      )}
    </Modal>
  );
}
