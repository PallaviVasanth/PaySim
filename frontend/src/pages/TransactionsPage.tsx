
import { Plus, SearchX } from 'lucide-react';
import { useState } from 'react';
import { listPayments } from '../api/payments';
import type { PaymentQuery, PaymentStatus, SortField } from '../api/types';
import { FilterBar } from '../components/payments/FilterBar';
import { PaymentDetailDialog } from '../components/payments/PaymentDetailDialog';
import { PaymentFormDialog } from '../components/payments/PaymentFormDialog';
import { TransactionTable } from '../components/payments/TransactionTable';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Pagination } from '../components/ui/Pagination';
import { Spinner } from '../components/ui/Spinner';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';
import { useDebounce } from '../hooks/useDebounce';

export function TransactionsPage() {
  const { user, canWrite } = useAuth();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<PaymentStatus | ''>('');
  const [sort, setSort] = useState<SortField>('createdAt');
  const [direction, setDirection] = useState<PaymentQuery['direction']>('desc');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [creating, setCreating] = useState(false);
  const [openId, setOpenId] = useState<string | null>(null);

  const debouncedSearch = useDebounce(search, 300);
  const query: PaymentQuery = { q: debouncedSearch, status, sort, direction, page, size };

  const { data, error, loading, reload } = useApi(
    () => listPayments(query, Boolean(user)),
    [debouncedSearch, status, sort, direction, page, size, user],
  );

  const hasFilters = debouncedSearch.trim() !== '' || status !== '';

  function onSort(field: SortField) {
    if (field === sort) {
      setDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSort(field);
      setDirection(field === 'createdAt' || field === 'amount' ? 'desc' : 'asc');
    }
    setPage(0);
  }

  return (
    <div className="page">
      <div className="toolbar">
        <FilterBar
          search={search}
          status={status}
          onSearchChange={(v) => {
            setSearch(v);
            setPage(0);
          }}
          onStatusChange={(v) => {
            setStatus(v);
            setPage(0);
          }}
        />

        {canWrite && (
          <button type="button" className="btn btn--primary" onClick={() => setCreating(true)}>
            <Plus size={16} aria-hidden="true" /> New payment
          </button>
        )}
      </div>

      <section className="panel panel--flush" aria-label="Transactions">
        {loading && !data && <Spinner label="Loading transactions" />}
        {error && !data && <ErrorState error={error} onRetry={reload} />}

        {data && (
          <>
            {error && (
              <p className="inline-error" role="alert">
                Could not refresh: {error.message}
              </p>
            )}

            {data.content.length === 0 ? (
              <EmptyState icon={<SearchX size={28} />} title={hasFilters ? 'No matching payments' : 'No payments yet'}>
                {hasFilters
                  ? 'Try a different search term or clear the status filter.'
                  : canWrite
                    ? 'Create a simulated payment to get started.'
                    : 'Nothing to show yet.'}
              </EmptyState>
            ) : (
              <TransactionTable
                payments={data.content}
                sort={sort}
                direction={direction}
                busy={loading}
                onSort={onSort}
                onOpen={setOpenId}
              />
            )}

            <Pagination
              page={data.page}
              size={data.size}
              totalElements={data.totalElements}
              totalPages={data.totalPages}
              onPageChange={setPage}
              onSizeChange={(n) => {
                setSize(n);
                setPage(0);
              }}
            />
          </>
        )}
      </section>

      {creating && (
        <PaymentFormDialog
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            setPage(0);
            reload();
          }}
        />
      )}

      {openId && (
        <PaymentDetailDialog
          paymentId={openId}
          onClose={() => setOpenId(null)}
          onChanged={reload}
        />
      )}
    </div>
  );
}
