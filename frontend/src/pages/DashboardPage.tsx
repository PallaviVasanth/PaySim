
import { Ban, CheckCircle2, Plus, ReceiptText, Undo2 } from 'lucide-react';
import { useState } from 'react';
import { getSummary } from '../api/payments';
import { MetricCard } from '../components/dashboard/MetricCard';
import { RecentActivityList } from '../components/dashboard/RecentActivityList';
import { StatusStrip } from '../components/dashboard/StatusStrip';
import { PaymentDetailDialog } from '../components/payments/PaymentDetailDialog';
import { PaymentFormDialog } from '../components/payments/PaymentFormDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { Spinner } from '../components/ui/Spinner';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../hooks/useApi';

export function DashboardPage() {
  const { user, canWrite } = useAuth();
  const { data, error, loading, reload } = useApi(
    () => getSummary(Boolean(user)),
    [user],
  );
  const [creating, setCreating] = useState(false);
  const [openId, setOpenId] = useState<string | null>(null);

  const newPaymentButton = canWrite && (
    <button type="button" className="btn btn--primary" onClick={() => setCreating(true)}>
      <Plus size={16} aria-hidden="true" /> New payment
    </button>
  );

  return (
    <div className="page">
      {loading && !data && <Spinner label="Loading dashboard" />}
      {error && !data && <ErrorState error={error} onRetry={reload} />}

      {data && (
        <>
          <section className="hero" aria-labelledby="overview-heading">
            <div className="hero__head">
              <div>
                <h2 id="overview-heading">Transactions processed</h2>
                <p className="hero__total">{data.totalTransactions.toLocaleString()}</p>
              </div>
              {newPaymentButton}
            </div>
            <StatusStrip distribution={data.statusDistribution} total={data.totalTransactions} />
          </section>

          <section className="metrics" aria-label="Summary metrics">
            <MetricCard label="Total" value={data.totalTransactions} tone="neutral" icon={<ReceiptText size={20} />} />
            <MetricCard label="Successful" value={data.successfulTransactions} tone="success" icon={<CheckCircle2 size={20} />} hint="Not yet refunded" />
            <MetricCard label="Declined" value={data.declinedTransactions} tone="declined" icon={<Ban size={20} />} />
            <MetricCard label="Refunded" value={data.refundedTransactions} tone="refunded" icon={<Undo2 size={20} />} />
          </section>

          <section className="panel" aria-labelledby="activity-heading">
            <h2 id="activity-heading">Recent activity</h2>
            {data.recentActivity.length === 0 ? (
              <EmptyState icon={<ReceiptText size={28} />} title="No payments yet" action={newPaymentButton || undefined}>
                {canWrite
                  ? 'Create your first simulated payment to see activity here.'
                  : 'Activity appears here once an operator creates a payment.'}
              </EmptyState>
            ) : (
              <RecentActivityList items={data.recentActivity} onOpen={setOpenId} />
            )}
          </section>
        </>
      )}

      {creating && (
        <PaymentFormDialog
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
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
