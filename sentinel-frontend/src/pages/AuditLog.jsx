import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../AuthContext';
import { api, describeError } from '../api';
import Pagination from '../components/Pagination';

const PAGE_SIZE = 20;

// `details` comes back from the backend as a JSON object (e.g. {"note": "..."}),
// not a plain string, so it can't be rendered directly as a React child.
function formatDetails(details) {
  if (details === null || details === undefined) return '—';
  if (typeof details === 'string') return details;
  try {
    return JSON.stringify(details);
  } catch {
    return String(details);
  }
}

export default function AuditLog() {
  const { auth } = useAuth();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.getAuditLog(auth.token, { page, size: PAGE_SIZE });
      setData(result);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  }, [auth.token, page]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div>
      <div className="page-header">
        <h1>Audit Log</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Entity</th>
                <th>Entity ID</th>
                <th>Action</th>
                <th>Actor</th>
                <th>Details</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {data?.content?.length ? (
                data.content.map((entry) => (
                  <tr key={entry.id}>
                    <td>{entry.entityType}</td>
                    <td className="mono">{entry.entityId}</td>
                    <td>{entry.action}</td>
                    <td>
                      {entry.actorUsername} <span className="muted">({entry.actorRole})</span>
                    </td>
                    <td className="mono">{formatDetails(entry.details)}</td>
                    <td>{new Date(entry.createdAt).toLocaleString()}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="empty-cell">No audit entries</td>
                </tr>
              )}
            </tbody>
          </table>
          <Pagination page={data?.number ?? 0} totalPages={data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </div>
  );
}
