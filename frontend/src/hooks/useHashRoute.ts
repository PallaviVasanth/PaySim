import { useEffect, useState } from 'react';

export type Route = 'dashboard' | 'transactions';

function parse(): Route {
  return window.location.hash === '#/transactions' ? 'transactions' : 'dashboard';
}

export function useHashRoute(): [Route, (route: Route) => void] {
  const [route, setRoute] = useState<Route>(parse);
  useEffect(() => {
    const onChange = () => setRoute(parse());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return [route, (next) => (window.location.hash = `#/${next}`)];
}
