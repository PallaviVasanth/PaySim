import { BookOpen, LayoutDashboard, ReceiptText } from 'lucide-react';
import type { Route } from '../../hooks/useHashRoute';

interface SidebarProps {
  route: Route;
  onNavigate: (route: Route) => void;
}

const ITEMS: { route: Route; label: string; Icon: typeof LayoutDashboard }[] = [
  { route: 'dashboard', label: 'Dashboard', Icon: LayoutDashboard },
  { route: 'transactions', label: 'Transactions', Icon: ReceiptText },
];

export function Sidebar({ route, onNavigate }: SidebarProps) {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <span className="sidebar__logo" aria-hidden="true">P</span>
        <div>
          <strong>PaySim</strong>
          <small>Simulator, no real money</small>
        </div>
      </div>
      <nav aria-label="Main">
        <ul className="sidebar__nav">
          {ITEMS.map(({ route: r, label, Icon }) => (
            <li key={r}>
              <button
                type="button"
                className="sidebar__link"
                aria-current={route === r ? 'page' : undefined}
                onClick={() => onNavigate(r)}
              >
                <Icon size={18} aria-hidden="true" />
                {label}
              </button>
            </li>
          ))}
          <li>
            <a className="sidebar__link" href="/swagger-ui.html" target="_blank" rel="noreferrer">
              <BookOpen size={18} aria-hidden="true" />
              API reference
            </a>
          </li>
        </ul>
      </nav>
    </aside>
  );
}
