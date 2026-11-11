import { Search } from 'lucide-react';
import type { PaymentStatus } from '../../api/types';
import { STATUS_LABEL } from '../../utils/format';

interface FilterBarProps {
  search: string;
  status: PaymentStatus | '';
  onSearchChange: (value: string) => void;
  onStatusChange: (value: PaymentStatus | '') => void;
}

export function FilterBar({ search, status, onSearchChange, onStatusChange }: FilterBarProps) {
  return (
    <form className="filters" role="search" onSubmit={(e) => e.preventDefault()}>
      <label className="field field--search">
        <span className="sr-only">Search by order, customer or payment ID</span>
        <Search size={16} aria-hidden="true" className="field__icon" />
        <input
          type="search"
          placeholder="Search order, customer or payment ID"
          value={search}
          maxLength={64}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </label>
      <label className="field">
        <span className="field__label">Status</span>
        <select value={status} onChange={(e) => onStatusChange(e.target.value as PaymentStatus | '')}>
          <option value="">All statuses</option>
          {(Object.keys(STATUS_LABEL) as PaymentStatus[]).map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </select>
      </label>
    </form>
  );
}
