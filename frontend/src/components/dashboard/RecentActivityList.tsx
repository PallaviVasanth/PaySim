import { PlusCircle, Undo2 } from 'lucide-react';
import type { RecentActivity } from '../../api/types';
import { formatRelative } from '../../utils/format';
import { StatusBadge } from '../ui/StatusBadge';

interface Props {
  items: RecentActivity[];
  onOpen: (paymentId: string) => void;
}

export function RecentActivityList({ items, onOpen }: Props) {
  return (
    <ul className="activity">
      {items.map((item, index) => {
        const Icon = item.eventType === 'PAYMENT_REFUNDED' ? Undo2 : PlusCircle;
        return (
          <li key={`${item.paymentId}-${item.eventType}-${index}`}>
            <Icon size={18} aria-hidden="true" className="activity__icon" />
            <div className="activity__body">
              <button type="button" className="link-btn" onClick={() => onOpen(item.paymentId)}>
                {item.orderReference}
              </button>
              <span className="activity__msg">{item.message}</span>
            </div>
            {item.newStatus && <StatusBadge status={item.newStatus} />}
            <time className="activity__time" dateTime={item.occurredAt}>
              {formatRelative(item.occurredAt)}
            </time>
          </li>
        );
      })}
    </ul>
  );
}
