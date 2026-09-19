import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth, canDisposition } from '../AuthContext';
import { api, describeError } from '../api';
import StatusTag from '../components/StatusTag';

const STATUSES = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'CLOSED_SAR_FILED', 'CLOSED_FALSE_POSITIVE'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

export default function CaseDetail() {
  const { id } = useParams();
  const { auth } = useAuth();
  const navigate = useNavigate();

  const [caseData, setCaseData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const [status, setStatus] = useState('');
  const [assignedAnalyst, setAssignedAnalyst] = useState('');
  const [priority, setPriority] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);

  const [note, setNote] = useState('');
  const [noteSubmitting, setNoteSubmitting] = useState(false);
  const [noteError, setNoteError] = useState(null);

  const allowed = canDisposition(auth.role);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getCase(auth.token, id);
      setCaseData(data);
      setStatus(data.status);
      setAssignedAnalyst(data.assignedAnalyst || '');
      setPriority(data.priority);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  }, [auth.token, id]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleSave() {
    setSaving(true);
    setSaveError(null);
    try {
      const updated = await api.updateCase(auth.token, id, {
        status,
        assignedAnalyst: assignedAnalyst.trim() || null,
        priority,
      });
      setCaseData(updated);
    } catch (err) {
      setSaveError(describeError(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleAddNote(e) {
    e.preventDefault();
    if (!note.trim()) return;
    setNoteSubmitting(true);
    setNoteError(null);
    try {
      const updated = await api.addCaseNote(auth.token, id, note.trim());
      setCaseData(updated);
      setStatus(updated.status);
      setAssignedAnalyst(updated.assignedAnalyst || '');
      setPriority(updated.priority);
      setNote('');
    } catch (err) {
      setNoteError(describeError(err));
    } finally {
      setNoteSubmitting(false);
    }
  }

  if (loading) return <p className="muted">Loading…</p>;
  if (error) return <div className="error-banner">{error}</div>;
  if (!caseData) return null;

  return (
    <div>
      <button type="button" className="btn btn-link" onClick={() => navigate('/cases')}>
        &larr; Back to Cases
      </button>

      <div className="detail-header">
        <h1>Case &middot; {caseData.customerId}</h1>
        <div className="detail-meta">
          <div><strong>Status:</strong> <StatusTag status={caseData.status} /></div>
          <div><strong>Priority:</strong> {caseData.priority}</div>
          <div><strong>Created:</strong> {new Date(caseData.createdAt).toLocaleString()}</div>
          <div><strong>Updated:</strong> {new Date(caseData.updatedAt).toLocaleString()}</div>
        </div>
      </div>

      <section className="section">
        <h2>Case Fields</h2>
        <div className="case-fields">
          <label>
            Status
            <select value={status} onChange={(e) => setStatus(e.target.value)} disabled={!allowed}>
              {STATUSES.map((s) => (
                <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </label>
          <label>
            Assigned Analyst
            <input
              value={assignedAnalyst}
              onChange={(e) => setAssignedAnalyst(e.target.value)}
              disabled={!allowed}
              placeholder="unassigned"
            />
          </label>
          <label>
            Priority
            <select value={priority} onChange={(e) => setPriority(e.target.value)} disabled={!allowed}>
              {PRIORITIES.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </label>
          {allowed && (
            <button type="button" className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
          )}
        </div>
        {saveError && <div className="error-banner">{saveError}</div>}
        {!allowed && <p className="muted">Read-only access — viewers cannot edit cases.</p>}
      </section>

      <section className="section">
        <h2>Notes / Summary</h2>
        <pre className="notes-box">{caseData.summary || 'No notes yet.'}</pre>
        {allowed && (
          <form className="note-form" onSubmit={handleAddNote}>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              placeholder="Add a note…"
            />
            {noteError && <div className="error-banner">{noteError}</div>}
            <button type="submit" className="btn btn-secondary" disabled={noteSubmitting || !note.trim()}>
              {noteSubmitting ? 'Adding…' : 'Add Note'}
            </button>
          </form>
        )}
      </section>

      <section className="section">
        <h2>Linked Alerts</h2>
        {caseData.alertIds?.length ? (
          <ul className="linked-list">
            {caseData.alertIds.map((aid) => (
              <li key={aid}>
                <Link to={`/alerts/${aid}`}>{aid}</Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">No linked alerts.</p>
        )}
      </section>
    </div>
  );
}
