import { LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { ThemeToggle } from './ThemeToggle';

export function TopBar({ title }: { title: string }) {
  const { user, signOut } = useAuth();
  return (
    <header className="topbar">
      <h1>{title}</h1>
      <div className="topbar__tools">
        <ThemeToggle />
        {user && (
          <div className="topbar__user">
            <span className="topbar__who">
              {user.username}
              <small>{user.roles.includes('OPERATOR') ? 'Operator' : 'Viewer, read-only'}</small>
            </span>
            <button type="button" className="btn btn--ghost" onClick={signOut}>
              <LogOut size={16} aria-hidden="true" /> Sign out
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
