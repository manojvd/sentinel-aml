import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth, canDisposition } from '../AuthContext';
import { api, describeError } from '../api';
import RiskBadge from '../components/RiskBadge';
import StatusTag from '../components/StatusTag';

const ACTIONS = [
  { action: 'START_REVIEW', label: 'Start Review' },
  { action: 'CLEAR', label: 'Clear' },
  { action: 'ESCALATE', label: 'Escalate' },
];

export default function AlertDetail() {
  const { id } = useParams();
  const { auth } = useAuth();
  const navigate = useNavigate();

  const [alert, setAlert] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const [activeAction, setActiveAction] = useState(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState(null);

  const [showCaseForm, setShowCaseForm] = useState(false);
  const [casePriority, setCasePriority] = useState('MEDIUM');
  const [caseSummary, setCaseSummary] = useState('');
  const [caseSubmitting, setCaseSubmitting] = useState(false);
  const [caseError, setCaseError] = useState(null);

  const allowed = canDisposition(auth.role);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getAlert(auth.token, id);
      setAlert(data);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  }, [auth.token, id]);

  useEffect(() => {
    load();
  }, [load]);

  function openAction(action) {
    setActiveAction(action);
    setReason('');
    setActionError(null);
  }

  async function submitDisposition() {
    if (!reason.trim()) {
      setActionError('A reason is required.');
      return;
    }
    setSubmitting(true);
    setActionError(null);
    try {
      const updated = await api.dispositionAlert(auth.token, id, activeAction, reason.trim());
      setAlert(updated);
      setActiveAction(null);
      setReason('');
    } catch (err) {
      setActionError(describeError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function submitCase(e) {
    e.preventDefault();
    if (!caseSummary.trim()) {
      setCaseError('Summary is required.');
      return;
    }
    setCaseSubmitting(true);
    setCaseError(null);
    try {
      const created = await api.createCase(auth.token, {
        customerId: alert.customerId,
        alertIds: [alert.id],
        priority: casePriority,
        summary: caseSummary.trim(),
      });
      navigate(`/cases/${created.id}`);
    } catch (err) {
      setCaseError(describeError(err));
    } finally {
      setCaseSubmitting(false);
    }
  }

  if (loading) return <p className="muted">Loading…</p>;
  if (error) return <div className="error-banner">{error}</div>;
  if (!alert) return null;

  return (
    <div>
      <button type="button" className="btn btn-link" onClick={() => navigate('/alerts')}>
        &larr; Back to Alert Queue
      </button>

      <div className="detail-header">
        <h1>
          {alert.customerDisplayName} <span className="muted">({alert.customerId})</span>
        </h1>
        <div className="detail-meta">
          <div><strong>Account:</strong> {alert.accountId}</div>
          <div><strong>Risk Score:</strong> <RiskBadge score={alert.riskScore} /></div>
          <div><strong>Status:</strong> <StatusTag status={alert.status} /></div>
          <div><strong>Created:</strong> {new Date(alert.createdAt).toLocaleString()}</div>
        </div>
        {alert.dispositionReason && (
          <div className="disposition-summary">
            <strong>Last disposition:</strong> {alert.dispositionReason}
            {alert.dispositionActor ? ` — by ${alert.dispositionActor}` : ''}
            {alert.dispositionedAt ? ` at ${new Date(alert.dispositionedAt).toLocaleString()}` : ''}
          </div>
        )}
      </div>

      <section className="section">
        <h2>Evidence</h2>
        {alert.evidence?.length ? (
          <div className="evidence-list">
            {alert.evidence.map((ev, i) => (
              <div className="evidence-card" key={`${ev.ruleCode}-${i}`}>
                <div className="evidence-head">
                  <span className="chip">{ev.ruleCode}</span>
                  <span className="muted">Risk contribution: {ev.riskContribution}</span>
                </div>
                <p>{ev.explanation}</p>
                {ev.evidenceTransactionIds?.length > 0 && (
                  <div className="tx-ids">
                    <strong>Transactions:</strong> {ev.evidenceTransactionIds.join(', ')}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">No evidence recorded.</p>
        )}
      </section>

      {allowed ? (
        <section className="section">
          <h2>Disposition</h2>
          <div className="action-row">
            {ACTIONS.map(({ action, label }) => (
              <button
                key={action}
                type="button"
                className="btn btn-secondary"
                onClick={() => openAction(action)}
              >
                {label}
              </button>
            ))}
          </div>
          {activeAction && (
            <div className="reason-panel">
              <label>
                Reason for &quot;{ACTIONS.find((a) => a.action === activeAction)?.label}&quot;
                <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={3} autoFocus />
              </label>
              {actionError && <div className="error-banner">{actionError}</div>}
              <div className="action-row">
                <button type="button" className="btn btn-primary" disabled={submitting} onClick={submitDisposition}>
                  {submitting ? 'Submitting…' : 'Confirm'}
                </button>
                <button type="button" className="btn btn-link" onClick={() => setActiveAction(null)}>
                  Cancel
                </button>
              </div>
            </div>
          )}
        </section>
      ) : (
        <section className="section">
          <h2>Disposition</h2>
          <p className="muted">Read-only access — viewers cannot disposition alerts.</p>
        </section>
      )}

      {allowed && (
        <section className="section">
          <h2>Case</h2>
          {!showCaseForm ? (
            <button type="button" className="btn btn-secondary" onClick={() => setShowCaseForm(true)}>
              Create Case from this Alert
            </button>
          ) : (
            <form className="case-form" onSubmit={submitCase}>
              <label>
                Priority
                <select value={casePriority} onChange={(e) => setCasePriority(e.target.value)}>
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </label>
              <label>
                Summary
                <textarea
                  value={caseSummary}
                  onChange={(e) => setCaseSummary(e.target.value)}
                  rows={3}
                  placeholder="Describe why this case is being opened…"
                />
              </label>
              {caseError && <div className="error-banner">{caseError}</div>}
              <div className="action-row">
                <button type="submit" className="btn btn-primary" disabled={caseSubmitting}>
                  {caseSubmitting ? 'Creating…' : 'Create Case'}
                </button>
                <button type="button" className="btn btn-link" onClick={() => setShowCaseForm(false)}>
                  Cancel
                </button>
              </div>
            </form>
          )}
        </section>
      )}
    </div>
  );
}
