import { LoginScreen } from './components/auth/LoginScreen';
import { Sidebar } from './components/layout/Sidebar';
import { TopBar } from './components/layout/TopBar';
import { useAuth } from './context/AuthContext';
import { useHashRoute } from './hooks/useHashRoute';
import { DashboardPage } from './pages/DashboardPage';
import { TransactionsPage } from './pages/TransactionsPage';

export default function App() {
  const { user } = useAuth();
  const [route, navigate] = useHashRoute();

  if (!user) return <LoginScreen />;

  return (
    <div className="shell">
      <a className="skip-link" href="#main">Skip to content</a>
      <Sidebar route={route} onNavigate={navigate} />
      <div className="shell__main">
        <TopBar title={route === 'dashboard' ? 'Dashboard' : 'Transactions'} />
        <main id="main" tabIndex={-1}>
          {route === 'dashboard' ? <DashboardPage /> : <TransactionsPage />}
        </main>
      </div>
    </div>
  );
}
