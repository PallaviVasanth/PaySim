import { Send } from 'lucide-react';
import { useRef, useState, type FormEvent, type ReactNode } from 'react';
import { ApiError } from '../../api/client';
import { createPayment } from '../../api/payments';
import type { Payment } from '../../api/types';
import { useToast } from '../../context/ToastContext';
import { newIdempotencyKey } from '../../utils/format';
import { Modal } from '../ui/Modal';

const CURRENCIES = ['USD', 'EUR', 'GBP', 'INR', 'JPY', 'CAD', 'AUD'];
const REF_PATTERN = /^[A-Za-z0-9._:-]{1,64}$/;
const AMOUNT_PATTERN = /^\d{1,7}(\.\d{1,2})?$/;

interface FormValues {
  customerReference: string;
  orderReference: string;
  amount: string;
  currency: string;
  description: string;
}

type Errors = Partial<Record<keyof FormValues, string>>;

function validate(v: FormValues): Errors {
  const errors: Errors = {};
  if (!REF_PATTERN.test(v.customerReference.trim())) errors.customerReference = 'Use 1–64 letters, digits, ".", "_", ":" or "-".';
  if (!REF_PATTERN.test(v.orderReference.trim())) errors.orderReference = 'Use 1–64 letters, digits, ".", "_", ":" or "-".';
  const amount = v.amount.trim();
  if (!AMOUNT_PATTERN.test(amount) || Number(amount) < 0.01) errors.amount = 'Enter an amount of at least 0.01 with at most 2 decimals.';
  else if (Number(amount) > 1_000_000) errors.amount = 'Amount must not exceed 1,000,000.00.';
  else if (v.currency === 'JPY' && amount.includes('.') && !/\.0{1,2}$/.test(amount)) errors.amount = 'JPY has no decimal places.';
  if (v.description.length > 255) errors.description = 'Description must be at most 255 characters.';
  return errors;
}

interface Props {
  onClose: () => void;
  onCreated: (payment: Payment) => void;
}

export function PaymentFormDialog({ onClose, onCreated }: Props) {
  const { notify } = useToast();
  const [values, setValues] = useState<FormValues>({
    customerReference: '',
    orderReference: '',
    amount: '',
    currency: 'USD',
    description: '',
  });
  const [errors, setErrors] = useState<Errors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // One key per distinct payload: retrying the SAME payload (e.g. after a network error) reuses the key,
  // so the server can de-duplicate; editing the payload starts a new logical request with a new key.
  const attempt = useRef<{ key: string; payload: string }>({ key: newIdempotencyKey(), payload: '' });

  const set = (field: keyof FormValues) => (e: { target: { value: string } }) =>
    setValues((v) => ({ ...v, [field]: e.target.value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const found = validate(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    const input = {
      customerReference: values.customerReference.trim(),
      orderReference: values.orderReference.trim(),
      amount: Number(values.amount.trim()),
      currency: values.currency,
      description: values.description.trim() || undefined,
    };
    const payload = JSON.stringify(input);
    if (attempt.current.payload !== payload) attempt.current = { key: newIdempotencyKey(), payload };

    setSubmitting(true);
    try {
      const { payment, replayed } = await createPayment(input, attempt.current.key);
      notify(
        replayed ? 'info' : payment.status === 'DECLINED' ? 'info' : 'success',
        replayed
          ? `This request was already processed. Showing existing payment ${payment.orderReference}.`
          : payment.status === 'DECLINED'
            ? `Payment ${payment.orderReference} was declined by the simulator.`
            : `Payment ${payment.orderReference} succeeded.`,
      );
      onCreated(payment);
    } catch (err) {
      if (err instanceof ApiError) {
        const fieldMap: Errors = {};
        err.fieldErrors.forEach((f) => {
          if (f.field in values) fieldMap[f.field as keyof FormValues] = f.message;
        });
        setErrors(fieldMap);
        setFormError(err.message);
      } else {
        setFormError('Something went wrong. Try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="New simulated payment" onClose={onClose}>
      <form className="form" onSubmit={onSubmit} noValidate>
        <p className="form__note">
          Simulator only. Never enter real card details. Tip: amounts ending in .13 or .51, or above 10,000.00, are declined.
        </p>
        {formError && (
          <p className="form__error" role="alert">
            {formError}
          </p>
        )}

        <div className="form__grid">
          <Field label="Customer reference" error={errors.customerReference}>
            {(props) => <input {...props} value={values.customerReference} onChange={set('customerReference')} autoFocus autoComplete="off" placeholder="CUST_1042" />}
          </Field>
          <Field label="Order reference" error={errors.orderReference}>
            {(props) => <input {...props} value={values.orderReference} onChange={set('orderReference')} autoComplete="off" placeholder="ORD-2026-0001" />}
          </Field>
          <Field label="Amount" error={errors.amount}>
            {(props) => <input {...props} value={values.amount} onChange={set('amount')} inputMode="decimal" autoComplete="off" placeholder="49.90" />}
          </Field>
          <Field label="Currency" error={errors.currency}>
            {(props) => (
              <select {...props} value={values.currency} onChange={set('currency')}>
                {CURRENCIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            )}
          </Field>
        </div>
        <Field label="Description (optional)" error={errors.description}>
          {(props) => <textarea {...props} rows={2} value={values.description} onChange={set('description')} maxLength={255} />}
        </Field>

        <footer className="modal__footer">
          <button type="button" className="btn btn--ghost" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            <Send size={16} aria-hidden="true" />
            {submitting ? 'Creating…' : 'Create payment'}
          </button>
        </footer>
      </form>
    </Modal>
  );
}

interface FieldProps {
  label: string;
  error?: string;
  children: (props: { id: string; 'aria-invalid': boolean; 'aria-describedby'?: string }) => ReactNode;
}

function Field({ label, error, children }: FieldProps) {
  const id = `f-${label.replace(/\W+/g, '-').toLowerCase()}`;
  const errId = `${id}-error`;
  return (
    <div className={`field field--block${error ? ' field--invalid' : ''}`}>
      <label htmlFor={id} className="field__label">{label}</label>
      {children({ id, 'aria-invalid': Boolean(error), 'aria-describedby': error ? errId : undefined })}
      {error && <p id={errId} className="field__error">{error}</p>}
    </div>
  );
}
