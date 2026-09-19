import { NavLink } from 'react-router-dom';
import { useAuth, canSeeAuditLog, canAccessCases } from '../AuthContext';

export default function Header() {
  const { auth, logout } = useAuth();
  if (!auth) return null;

  return (
    <header className="app-header">
      <div className="brand">Sentinel AML</div>
      <nav className="main-nav">
        <NavLink to="/alerts" className={({ isActive }) => (isActive ? 'active' : '')}>
          Alert Queue
        </NavLink>
        {canAccessCases(auth.role) && (
          <NavLink to="/cases" className={({ isActive }) => (isActive ? 'active' : '')}>
            Cases
          </NavLink>
        )}
        {canSeeAuditLog(auth.role) && (
          <NavLink to="/audit-log" className={({ isActive }) => (isActive ? 'active' : '')}>
            Audit Log
          </NavLink>
        )}
      </nav>
      <div className="user-info">
        <span className="user-badge">{auth.username} · {auth.role.replace(/_/g, ' ')}</span>
        <button type="button" className="btn btn-secondary" onClick={logout}>
          Logout
        </button>
      </div>
    </header>
  );
}
