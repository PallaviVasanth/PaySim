import { ArrowDown, ArrowUp, ChevronsUpDown } from 'lucide-react';
import type { Payment, PaymentQuery, SortField } from '../../api/types';
import { formatDateTime, formatMoney, shortId } from '../../utils/format';
import { StatusBadge } from '../ui/StatusBadge';

interface TransactionTableProps {
  payments: Payment[];
  sort: PaymentQuery['sort'];
  direction: PaymentQuery['direction'];
  busy: boolean;
  onSort: (field: SortField) => void;
  onOpen: (id: string) => void;
}

const COLUMNS: { field: SortField | null; label: string; align?: 'right' }[] = [
  { field: null, label: 'Payment' },
  { field: 'orderReference', label: 'Order' },
  { field: null, label: 'Customer' },
  { field: 'amount', label: 'Amount', align: 'right' },
  { field: 'status', label: 'Status' },
  { field: 'createdAt', label: 'Created' },
];

export function TransactionTable({ payments, sort, direction, busy, onSort, onOpen }: TransactionTableProps) {
  return (
    <div className={`table-wrap${busy ? ' is-busy' : ''}`} aria-busy={busy}>
      <table className="table">
        <caption className="sr-only">Payment transactions</caption>
        <thead>
          <tr>
            {COLUMNS.map(({ field, label, align }) => {
              const active = field !== null && sort === field;
              const ariaSort = active ? (direction === 'asc' ? 'ascending' : 'descending') : field ? 'none' : undefined;
              return (
                <th key={label} scope="col" aria-sort={ariaSort} className={align === 'right' ? 'num' : undefined}>
                  {field ? (
                    <button type="button" className="th-btn" onClick={() => onSort(field)}>
                      {label}
                      {active ? (
                        direction === 'asc' ? <ArrowUp size={14} aria-hidden="true" /> : <ArrowDown size={14} aria-hidden="true" />
                      ) : (
                        <ChevronsUpDown size={14} aria-hidden="true" className="th-btn__idle" />
                      )}
                    </button>
                  ) : (
                    label
                  )}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {payments.map((p) => (
            <tr key={p.id}>
              <td className="mono" title={p.id}>{shortId(p.id)}</td>
              <td>
                <button type="button" className="link-btn" onClick={() => onOpen(p.id)}>
                  {p.orderReference}
                  <span className="sr-only"> (view details)</span>
                </button>
              </td>
              <td>{p.customerReference}</td>
              <td className="num">{formatMoney(p.amount, p.currency)}</td>
              <td><StatusBadge status={p.status} /></td>
              <td>{formatDateTime(p.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
