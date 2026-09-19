import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { api, describeError } from '../api';
import StatusTag from '../components/StatusTag';
import Pagination from '../components/Pagination';

const PAGE_SIZE = 10;

export default function CaseList() {
  const { auth } = useAuth();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.getCases(auth.token, { page, size: PAGE_SIZE });
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
        <h1>Cases</h1>
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
                <th>Status</th>
                <th>Priority</th>
                <th>Assigned Analyst</th>
                <th>Created</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {data?.content?.length ? (
                data.content.map((c) => (
                  <tr key={c.id} className="clickable-row" onClick={() => navigate(`/cases/${c.id}`)}>
                    <td>{c.customerId}</td>
                    <td><StatusTag status={c.status} /></td>
                    <td>{c.priority}</td>
                    <td>{c.assignedAnalyst || '—'}</td>
                    <td>{new Date(c.createdAt).toLocaleString()}</td>
                    <td>{new Date(c.updatedAt).toLocaleString()}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="empty-cell">No cases found</td>
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
