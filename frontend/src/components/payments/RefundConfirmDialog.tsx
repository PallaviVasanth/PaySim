import { Undo2 } from 'lucide-react';
import { useState } from 'react';
import { ApiError } from '../../api/client';
import { refundPayment } from '../../api/payments';
import type { Payment } from '../../api/types';
import { useToast } from '../../context/ToastContext';
import { formatMoney } from '../../utils/format';
import { Modal } from '../ui/Modal';

interface Props {
  payment: Payment;
  onClose: () => void;
  onRefunded: () => void;
}

export function RefundConfirmDialog({ payment, onClose, onRefunded }: Props) {
  const { notify } = useToast();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      await refundPayment(payment.id);
      notify('success', `Payment ${payment.orderReference} was fully refunded.`);
      onRefunded();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Refund failed. Try again.');
      if (e instanceof ApiError && e.status === 409) onRefunded(); // state changed elsewhere: refresh the view
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Refund this payment?" onClose={onClose} size="sm">
      <div className="form">
        <p>
          This refunds <strong>{formatMoney(payment.amount, payment.currency)}</strong> for order{' '}
          <strong>{payment.orderReference}</strong> in full. Partial refunds are not supported, and a refund cannot be undone.
        </p>
        {error && (
          <p className="form__error" role="alert">
            {error}
          </p>
        )}
        <footer className="modal__footer">
          <button type="button" className="btn btn--ghost" onClick={onClose} disabled={busy} autoFocus>
            Keep payment
          </button>
          <button type="button" className="btn btn--danger" onClick={confirm} disabled={busy}>
            <Undo2 size={16} aria-hidden="true" />
            {busy ? 'Refunding…' : 'Refund in full'}
          </button>
        </footer>
      </div>
    </Modal>
  );
}
