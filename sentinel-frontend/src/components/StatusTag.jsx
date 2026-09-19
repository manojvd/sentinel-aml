export default function StatusTag({ status }) {
  if (!status) return null;
  return <span className={`status-tag status-${status.toLowerCase()}`}>{status.replace(/_/g, ' ')}</span>;
}
