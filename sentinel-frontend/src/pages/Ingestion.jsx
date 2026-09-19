import { useState } from 'react';
import { useAuth } from '../AuthContext';
import { api, describeError } from '../api';

const KINDS = [
  { key: 'customers', label: 'Customers', hint: 'customer_id, first_name, last_name, ... (see customers.csv)' },
  { key: 'accounts', label: 'Accounts', hint: 'account_id, customer_id, account_type, ... (see accounts.csv)' },
  { key: 'transactions', label: 'Transactions', hint: 'account_id, direction, amount, currency, channel, txn_timestamp, ...' },
];

function UploadCard({ kind, label, hint }) {
  const { auth } = useAuth();
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [uploading, setUploading] = useState(false);

  const upload = async (e) => {
    e.preventDefault();
    if (!file) return;
    setUploading(true);
    setError(null);
    setResult(null);
    try {
      const summary = await api.ingestCsv(auth.token, kind, file);
      setResult(summary);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="section">
      <h2>{label}</h2>
      <p className="muted">{hint}</p>
      <form className="case-form" onSubmit={upload}>
        <label>
          CSV file
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </label>
        <div className="action-row">
          <button type="submit" className="btn btn-primary" disabled={!file || uploading}>
            {uploading ? 'Uploading…' : `Upload ${label.toLowerCase()}`}
          </button>
        </div>
      </form>

      {error && <div className="error-banner">{error}</div>}

      {result && (
        <div className="disposition-summary">
          <p>
            <strong>{result.accepted}</strong> accepted, <strong>{result.rejected}</strong> rejected
            {typeof result.elapsedMillis === 'number' && <> · {result.elapsedMillis}ms</>}
          </p>
          {result.errors?.length > 0 && (
            <ul className="muted">
              {result.errors.slice(0, 10).map((e, i) => (
                <li key={i}>{e}</li>
              ))}
              {result.errors.length > 10 && <li>… and {result.errors.length - 10} more</li>}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

export default function Ingestion() {
  return (
    <div>
      <div className="page-header">
        <h1>Data Ingestion</h1>
      </div>
      <p className="muted">
        Bulk-load customer, account, and transaction CSVs. Rows are validated individually —
        bad rows are rejected and logged without failing the whole batch. Transactions are run
        through the detection engine immediately after loading, so any new alerts will show up
        in the Alert Queue right after upload completes. Load customers before accounts, and
        accounts before transactions, since each row is checked for referential integrity
        against what's already loaded.
      </p>
      {KINDS.map((k) => (
        <UploadCard key={k.key} kind={k.key} label={k.label} hint={k.hint} />
      ))}
    </div>
  );
}
