import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../api/client';

interface State<T> {
  data: T | null;
  error: ApiError | null;
  loading: boolean;
}

/** Runs `fetcher` on mount and whenever `deps` change; ignores responses from superseded calls. */
export function useApi<T>(fetcher: () => Promise<T>, deps: readonly unknown[]) {
  const [state, setState] = useState<State<T>>({ data: null, error: null, loading: true });
  const [tick, setTick] = useState(0);
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  useEffect(() => {
    let cancelled = false;
    setState((s) => ({ ...s, loading: true, error: null }));
    fetcherRef
      .current()
      .then((data) => {
        if (!cancelled) setState({ data, error: null, loading: false });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const error = e instanceof ApiError ? e : new ApiError(0, 'UNKNOWN', 'Something went wrong.');
        setState((s) => ({ data: s.data, error, loading: false }));
      });
    return () => {
      cancelled = true;
    };
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { ...state, reload };
}
