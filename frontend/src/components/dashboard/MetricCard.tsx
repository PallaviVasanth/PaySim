import type { ReactNode } from 'react';

interface MetricCardProps {
  label: string;
  value: number;
  icon: ReactNode;
  tone: 'neutral' | 'success' | 'declined' | 'refunded';
  hint?: string;
}

export function MetricCard({ label, value, icon, tone, hint }: MetricCardProps) {
  return (
    <div className={`metric metric--${tone}`}>
      <div className="metric__icon" aria-hidden="true">
        {icon}
      </div>
      <div>
        <p className="metric__label">{label}</p>
        <p className="metric__value">{value.toLocaleString()}</p>
        {hint && <p className="metric__hint">{hint}</p>}
      </div>
    </div>
  );
}
