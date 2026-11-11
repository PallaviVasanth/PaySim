import type { PaymentStatus } from '../../api/types';
import { STATUS_LABEL } from '../../utils/format';

const ORDER: PaymentStatus[] = ['SUCCEEDED', 'DECLINED', 'REFUNDED'];

interface StatusStripProps {
  distribution: Record<PaymentStatus, number>;
  total: number;
}

/** One continuous bar showing how all transactions split across statuses. */
export function StatusStrip({ distribution, total }: StatusStripProps) {
  const summary = ORDER.map((s) => `${STATUS_LABEL[s]} ${distribution[s]}`).join(', ');
  return (
    <div className="strip">
      <div className="strip__bar" role="img" aria-label={`Status distribution of ${total} transactions: ${summary}`}>
        {total === 0 ? (
          <span className="strip__seg strip__seg--empty" style={{ flexBasis: '100%' }} />
        ) : (
          ORDER.filter((s) => distribution[s] > 0).map((s) => (
            <span
              key={s}
              className={`strip__seg strip__seg--${s.toLowerCase()}`}
              style={{ flexBasis: `${(distribution[s] / total) * 100}%` }}
            />
          ))
        )}
      </div>
      <ul className="strip__legend">
        {ORDER.map((s) => (
          <li key={s}>
            <span className={`strip__dot strip__seg--${s.toLowerCase()}`} aria-hidden="true" />
            {STATUS_LABEL[s]}
            <strong>{distribution[s].toLocaleString()}</strong>
            <span className="strip__pct">{total === 0 ? '0%' : `${Math.round((distribution[s] / total) * 100)}%`}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
