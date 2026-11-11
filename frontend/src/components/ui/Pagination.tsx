import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

export function Pagination({ page, size, totalElements, totalPages, onPageChange, onSizeChange }: PaginationProps) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <nav className="pagination" aria-label="Pagination">
      <p className="pagination__summary">
        Showing {from}–{to} of {totalElements}
      </p>
      <label className="pagination__size">
        Rows per page
        <select value={size} onChange={(e) => onSizeChange(Number(e.target.value))}>
          {[10, 20, 50].map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </label>
      <div className="pagination__buttons">
        <button type="button" className="btn btn--icon" disabled={page <= 0} onClick={() => onPageChange(page - 1)} aria-label="Previous page">
          <ChevronLeft size={18} aria-hidden="true" />
        </button>
        <span aria-live="polite">
          Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
        </span>
        <button
          type="button"
          className="btn btn--icon"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
        >
          <ChevronRight size={18} aria-hidden="true" />
        </button>
      </div>
    </nav>
  );
}
