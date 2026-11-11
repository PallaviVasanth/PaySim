import { RefreshCw, TriangleAlert } from 'lucide-react';
import type { ApiError } from '../../api/client';

export function ErrorState({ error, onRetry }: { error: ApiError; onRetry?: () => void }) {
  return (
    <div className="state state--error" role="alert">
      <div className="state__icon" aria-hidden="true">
        <TriangleAlert size={28} />
      </div>
      <h3>Could not load data</h3>
      <p>{error.message}</p>
      {onRetry && (
        <button type="button" className="btn" onClick={onRetry}>
          <RefreshCw size={16} aria-hidden="true" /> Try again
        </button>
      )}
    </div>
  );
}
