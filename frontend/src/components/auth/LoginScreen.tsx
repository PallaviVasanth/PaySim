import { LogIn, ShieldAlert } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { ThemeToggle } from '../layout/ThemeToggle';

export function LoginScreen() {
  const { signIn, sessionExpired } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await signIn(username.trim(), password);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? 'Username or password is incorrect.'
          : err instanceof ApiError
            ? err.message
            : 'Sign-in failed. Try again.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="login">
      <div className="login__theme"><ThemeToggle /></div>
      <form className="login__card" onSubmit={onSubmit}>
        <div className="login__brand">
          <span className="sidebar__logo" aria-hidden="true">P</span>
          <h1>Sign in to PaySim</h1>
        </div>
        <p className="login__note">
          <ShieldAlert size={16} aria-hidden="true" /> A payment simulator. No real money or card data is processed.
        </p>
        {sessionExpired && !error && <p className="form__error" role="alert">Your session ended. Sign in again to continue.</p>}
        {error && <p className="form__error" role="alert">{error}</p>}

        <div className="field field--block">
          <label className="field__label" htmlFor="username">Username</label>
          <input id="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required autoFocus />
        </div>
        <div className="field field--block">
          <label className="field__label" htmlFor="password">Password</label>
          <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </div>
        <button type="submit" className="btn btn--primary btn--block" disabled={busy || !username || !password}>
          <LogIn size={16} aria-hidden="true" /> {busy ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="login__hint">Accounts are defined in your <code>.env</code> file: an operator (can create and refund) and a viewer (read-only).</p>
      </form>
    </main>
  );
}
