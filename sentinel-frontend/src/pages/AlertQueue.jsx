import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { api, describeError } from '../api';
import RiskBadge from '../components/RiskBadge';
import StatusTag from '../components/StatusTag';
import Pagination from '../components/Pagination';

const STATUS_OPTIONS = ['ALL', 'OPEN', 'IN_REVIEW', 'CLEARED', 'ESCALATED'];
const PAGE_SIZE = 10;

function formatDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString();
}

export default function AlertQueue() {
  const { auth } = useAuth();
  const navigate = useNavigate();
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.getAlerts(auth.token, {
        status: status === 'ALL' ? undefined : status,
        page,
        size: PAGE_SIZE,
        sort: 'riskScore,desc',
      });
      setData(result);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  }, [auth.token, status, page]);

  useEffect(() => {
    load();
  }, [load]);

  function handleStatusChange(e) {
    setStatus(e.target.value);
    setPage(0);
  }

  return (
    <div>
      <div className="page-header">
        <h1>Alert Queue</h1>
        <label className="filter-label">
          Status
          <select value={status} onChange={handleStatusChange}>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s === 'ALL' ? 'All' : s.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Account</th>
                <th>Risk Score</th>
                <th>Status</th>
                <th>Created</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {data?.content?.length ? (
                data.content.map((alert) => (
                  <tr
                    key={alert.id}
                    className="clickable-row"
                    onClick={() => navigate(`/alerts/${alert.id}`)}
                  >
                    <td>
                      {alert.customerDisplayName}{' '}
                      <span className="muted">({alert.customerId})</span>
                    </td>
                    <td>{alert.accountId}</td>
                    <td><RiskBadge score={alert.riskScore} /></td>
                    <td><StatusTag status={alert.status} /></td>
                    <td>{formatDate(alert.createdAt)}</td>
                    <td>{formatDate(alert.updatedAt)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="empty-cell">No alerts found</td>
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
